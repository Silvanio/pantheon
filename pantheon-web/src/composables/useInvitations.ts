import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type MembershipType = 'COMPANY' | 'SITE'

export interface Invitation {
  targetName: string
  membershipType: MembershipType
  inviterName: string | null
  email: string
  requiresRegistration: boolean
  accepted: boolean
}

export interface CompleteRegistrationData {
  password: string
  displayName: string
}

export interface AcceptedInvitation {
  targetId: string
  targetName: string
  membershipType: MembershipType
}

async function request<T>(path: string, options: RequestInit = {}, auth = false): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(options.headers as Record<string, string>) }
  if (auth) {
    const { token } = useAuth()
    headers.Authorization = `Bearer ${token.value}`
  }
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, { ...options, headers })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function useInvitations() {
  function getInvitation(token: string): Promise<Invitation> {
    return request(`/api/invitations/${encodeURIComponent(token)}`)
  }

  function completeRegistration(token: string, data: CompleteRegistrationData): Promise<{ token: string }> {
    return request(`/api/invitations/${encodeURIComponent(token)}/complete-registration`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function acceptInvitation(token: string): Promise<AcceptedInvitation> {
    return request(
      `/api/invitations/${encodeURIComponent(token)}/accept`,
      { method: 'POST' },
      true,
    )
  }

  return { getInvitation, completeRegistration, acceptInvitation }
}
