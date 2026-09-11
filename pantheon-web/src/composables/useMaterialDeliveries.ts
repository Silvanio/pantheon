import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type MaterialDeliveryStatus = 'AWAITING_DELIVERY' | 'DELIVERED' | 'DELIVERED_AND_CHECKED'

export interface Material {
  id: string
  constructionSiteId: string
  orcamentoLineItemId: string
  name: string
  type: string | null
  quantity: string
  status: MaterialDeliveryStatus
  deliveredAt: string | null
  deliveredBy: string | null
  checkedAt: string | null
  checkedBy: string | null
  createdAt: string
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

export function useMaterialDeliveries() {
  function listMaterials(siteId: string, orcamentoId?: string): Promise<Material[]> {
    const query = orcamentoId ? `?orcamentoId=${orcamentoId}` : ''
    return authFetch(`/api/construction-sites/${siteId}/materials${query}`)
  }

  function markDelivered(materialId: string): Promise<Material> {
    return authFetch(`/api/materials/${materialId}/mark-delivered`, { method: 'POST' })
  }

  async function markChecked(materialId: string, photos: File[]): Promise<Material> {
    const { token } = useAuth()
    const formData = new FormData()
    photos.forEach((file) => formData.append('photos', file))
    const response = await fetch(`${SERVICE_BASE_URL}/api/materials/${materialId}/mark-checked`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token.value}` },
      body: formData,
    })
    if (!response.ok) {
      throw new HttpError(response.status, `Request failed with status ${response.status}`)
    }
    return (await response.json()) as Material
  }

  return { listMaterials, markDelivered, markChecked }
}
