import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type SiteStatus = 'PLANNING' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED'

export interface ConstructionSite {
  id: string
  companyId: string
  name: string
  address: string
  status: SiteStatus
  startDate: string
  expectedEndDate: string | null
  photoObjectKey: string | null
}

export interface ConstructionSiteRegistrationData {
  name: string
  address: string
  startDate: string
  expectedEndDate: string | null
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

export function useConstructionSites() {
  function listSites(companyId: string): Promise<ConstructionSite[]> {
    return authFetch(`/api/companies/${companyId}/construction-sites`)
  }

  function createSite(companyId: string, data: ConstructionSiteRegistrationData): Promise<ConstructionSite> {
    return authFetch(`/api/companies/${companyId}/construction-sites`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function updateSiteStatus(siteId: string, status: SiteStatus): Promise<ConstructionSite> {
    return authFetch(`/api/construction-sites/${siteId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })
  }

  async function updateSitePhoto(siteId: string, photo: File): Promise<ConstructionSite> {
    const { token } = useAuth()
    const formData = new FormData()
    formData.set('photo', photo)
    const response = await fetch(`${SERVICE_BASE_URL}/api/construction-sites/${siteId}/photo`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token.value}` },
      body: formData,
    })
    if (!response.ok) {
      throw new HttpError(response.status, `Request failed with status ${response.status}`)
    }
    return (await response.json()) as ConstructionSite
  }

  function getSite(siteId: string): Promise<ConstructionSite> {
    return authFetch(`/api/construction-sites/${siteId}`)
  }

  async function getSitePhotoBlob(siteId: string): Promise<Blob> {
    const { token } = useAuth()
    const response = await fetch(`${SERVICE_BASE_URL}/api/construction-sites/${siteId}/photo/content`, {
      headers: { Authorization: `Bearer ${token.value}` },
    })
    if (!response.ok) {
      throw new HttpError(response.status, `Request failed with status ${response.status}`)
    }
    return response.blob()
  }

  return { listSites, createSite, updateSiteStatus, updateSitePhoto, getSite, getSitePhotoBlob }
}
