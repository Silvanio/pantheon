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

export function useAuth() {
  const isAuthenticated = computed(() => token.value !== null)

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

  return { token, isAuthenticated, login, register, logout, setToken, googleLoginUrl }
}
