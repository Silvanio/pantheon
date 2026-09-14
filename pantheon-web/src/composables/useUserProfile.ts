import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export interface UserProfile {
  email: string
  cnpjCpf: string | null
  legalName: string | null
  address: string | null
  postalCode: string | null
}

export interface UpdateUserProfileData {
  cnpjCpf: string
  legalName: string
  address: string
  postalCode: string
}

async function authFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token.value}`,
      ...options.headers,
    },
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

/**
 * The current user's own registration data — their account email (read-only here) plus their
 * editable CNPJ/CPF, legal name, address, and CEP, independent of any company. The email is
 * always present even before a profile has ever been saved.
 */
export function useUserProfile() {
  function getMyProfile(): Promise<UserProfile> {
    return authFetch('/api/users/me/profile')
  }

  function updateMyProfile(data: UpdateUserProfileData): Promise<UserProfile> {
    return authFetch('/api/users/me/profile', { method: 'PUT', body: JSON.stringify(data) })
  }

  return { getMyProfile, updateMyProfile }
}
