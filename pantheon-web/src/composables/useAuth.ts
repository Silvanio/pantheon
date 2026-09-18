import { computed, ref } from 'vue'
import { SERVICE_BASE_URL } from '../lib/config'

const TOKEN_KEY = 'pantheon-token'

const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))

interface AuthResponse {
  token: string
}

export class HttpError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'HttpError'
    this.status = status
  }
}

async function postJson(path: string, body: unknown): Promise<AuthResponse> {
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as AuthResponse
}

/** Decodes the JWT payload's `email` claim for display purposes only — never trust this for authorization. */
function decodeEmail(jwt: string | null): string | null {
  if (!jwt) return null
  try {
    const payload = jwt.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    return (JSON.parse(json) as { email?: string }).email ?? null
  } catch {
    return null
  }
}

export function useAuth() {
  const isAuthenticated = computed(() => token.value !== null)
  const userEmail = computed(() => decodeEmail(token.value))

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
  }

  async function login(email: string, password: string) {
    const response = await postJson('/api/auth/login', { email, password })
    setToken(response.token)
  }

  async function register(email: string, password: string, displayName: string) {
    const response = await postJson('/api/auth/register', { email, password, displayName })
    setToken(response.token)
  }

  function logout() {
    token.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  function googleLoginUrl(): string {
    return `${SERVICE_BASE_URL}/oauth2/authorization/google`
  }

  return { token, isAuthenticated, userEmail, login, register, logout, setToken, googleLoginUrl }
}
