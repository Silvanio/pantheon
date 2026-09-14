import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type ConstructionFunction = 'ADMIN' | 'CLIENT' | 'ARCHITECT' | 'ENGINEER' | 'SITE_FOREMAN' | 'SERVICE_PROVIDER'
export type MembershipStatus = 'INVITED' | 'ACTIVE' | 'NONE'

export interface SiteMember {
  membershipId: string
  userId: string | null
  email: string | null
  displayName: string | null
  function: ConstructionFunction
  trade: string | null
  cpf: string | null
  phone: string | null
  status: MembershipStatus
  invited: boolean
}

export interface AddSiteMemberData {
  function: ConstructionFunction
  displayName: string
  email?: string | null
  cpf?: string | null
  phone?: string | null
  trade?: string | null
  contactEmail?: string | null
}

export interface MemberInvitation {
  invitationId: string
  membershipId: string
  email: string
  requiresRegistration: boolean
}

export interface PersonSearchResult {
  name: string | null
  email: string | null
  cpf: string | null
  phone: string | null
}

export interface EmailConflictCheck {
  activeOnThisSite: boolean
  existsInAnotherCompany: boolean
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
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function useSiteMembers() {
  function listMembers(siteId: string): Promise<SiteMember[]> {
    return authFetch(`/api/sites/${siteId}/members`)
  }

  function addMember(siteId: string, data: AddSiteMemberData): Promise<SiteMember | MemberInvitation> {
    return authFetch(`/api/sites/${siteId}/members`, { method: 'POST', body: JSON.stringify(data) })
  }

  function removeMember(siteId: string, membershipId: string): Promise<void> {
    return authFetch(`/api/sites/${siteId}/members/${membershipId}`, { method: 'DELETE' })
  }

  function searchPeople(siteId: string, query: string): Promise<PersonSearchResult[]> {
    return authFetch(`/api/sites/${siteId}/people/search?q=${encodeURIComponent(query)}`)
  }

  function checkEmailConflicts(siteId: string, email: string): Promise<EmailConflictCheck> {
    return authFetch(`/api/sites/${siteId}/people/email-check?email=${encodeURIComponent(email)}`)
  }

  return { listMembers, addMember, removeMember, searchPeople, checkEmailConflicts }
}
