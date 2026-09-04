import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { ConstructionFunction } from './useSiteMembers'

export type PermissionCapability =
  | 'DOCUMENT_PROJECTS'
  | 'DAILY_REPORT'
  | 'EQUIPMENT_MATERIAL'
  | 'MATERIAL_REQUEST'
  | 'MATERIAL_APPROVAL'
export type AccessLevel = 'VIEW' | 'MANAGE'

export interface SitePermissionOverride {
  id: string
  siteMembershipId: string | null
  function: ConstructionFunction | null
  capability: PermissionCapability
  accessLevel: AccessLevel
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

export function useSitePermissions() {
  function listOverrides(siteId: string): Promise<SitePermissionOverride[]> {
    return authFetch(`/api/sites/${siteId}/permissions`)
  }

  function setFunctionOverride(
    siteId: string,
    fn: ConstructionFunction,
    capability: PermissionCapability,
    accessLevel: AccessLevel,
  ): Promise<SitePermissionOverride> {
    return authFetch(`/api/sites/${siteId}/permissions/function`, {
      method: 'PUT',
      body: JSON.stringify({ function: fn, capability, accessLevel }),
    })
  }

  function setMemberOverride(
    siteId: string,
    siteMembershipId: string,
    capability: PermissionCapability,
    accessLevel: AccessLevel,
  ): Promise<SitePermissionOverride> {
    return authFetch(`/api/sites/${siteId}/permissions/member`, {
      method: 'PUT',
      body: JSON.stringify({ siteMembershipId, capability, accessLevel }),
    })
  }

  return { listOverrides, setFunctionOverride, setMemberOverride }
}
