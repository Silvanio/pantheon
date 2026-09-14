import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { SiteStatus } from './useConstructionSites'

export interface MySite {
  id: string
  companyId: string
  companyName: string | null
  name: string
  address: string
  status: SiteStatus
  startDate: string
  expectedEndDate: string | null
  photoObjectKey: string | null
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
 * A site-only member's own obras, gathered across every company they have SiteMembership
 * access to — mirrors useConstructionSites but scoped by the caller's own memberships instead
 * of one companyId. See add-site-only-member-dashboard.
 */
export function useMySites() {
  function listMine(): Promise<MySite[]> {
    return authFetch('/api/construction-sites/mine')
  }

  async function getSiteCompanyLogoBlob(siteId: string): Promise<Blob> {
    const { token } = useAuth()
    const response = await fetch(`${SERVICE_BASE_URL}/api/construction-sites/${siteId}/company-logo`, {
      headers: { Authorization: `Bearer ${token.value}` },
    })
    if (!response.ok) {
      throw new HttpError(response.status, `Request failed with status ${response.status}`)
    }
    return response.blob()
  }

  return { listMine, getSiteCompanyLogoBlob }
}
