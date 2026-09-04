import { onBeforeUnmount, ref } from 'vue'
import { SERVICE_BASE_URL } from '../lib/config'
import { useAuth } from './useAuth'

export interface SseLogEntry {
  id: number
  eventName: string
  data: string
  receivedAt: Date
}

const MAX_LOG_ENTRIES = 50
const INITIAL_BACKOFF_MS = 1000
const MAX_BACKOFF_MS = 30000

let entryCounter = 0

/**
 * Wraps the browser EventSource API to connect to pantheon-service's
 * GET /api/sse/subscribe stream, reconnecting with exponential backoff on drop.
 * The native EventSource API can't set an Authorization header, so the JWT is
 * passed as a query parameter instead (see JwtAuthenticationFilter.resolveToken).
 */
export function useSse(eventNames: string[]) {
  const { token } = useAuth()
  const connected = ref(false)
  const entries = ref<SseLogEntry[]>([])

  let eventSource: EventSource | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let backoffMs = INITIAL_BACKOFF_MS

  function recordEntry(eventName: string, data: string) {
    entryCounter += 1
    entries.value = [{ id: entryCounter, eventName, data, receivedAt: new Date() }, ...entries.value].slice(
      0,
      MAX_LOG_ENTRIES,
    )
  }

  function connect() {
    if (!token.value) {
      return
    }
    const url = new URL('/api/sse/subscribe', SERVICE_BASE_URL)
    url.searchParams.set('access_token', token.value)

    const source = new EventSource(url.toString())
    eventSource = source

    source.onopen = () => {
      connected.value = true
      backoffMs = INITIAL_BACKOFF_MS
    }

    source.onerror = () => {
      connected.value = false
      source.close()
      scheduleReconnect()
    }

    for (const eventName of eventNames) {
      source.addEventListener(eventName, (event) => {
        recordEntry(eventName, (event as MessageEvent<string>).data)
      })
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) {
      return
    }
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS)
      connect()
    }, backoffMs)
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    eventSource?.close()
    eventSource = null
    connected.value = false
  }

  onBeforeUnmount(disconnect)

  return { connected, entries, connect, disconnect }
}
