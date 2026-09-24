import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { PageResponse } from './usePurchaseRequests'

export type EquipmentStatus = 'AVAILABLE' | 'IN_USE' | 'MAINTENANCE' | 'UNAVAILABLE'

export interface Equipment {
  id: string
  constructionSiteId: string
  name: string
  type: string | null
  status: EquipmentStatus
}

export interface EquipmentRegistrationData {
  name: string
  type: string | null
  status: EquipmentStatus
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

export interface EquipmentListFilter {
  name?: string
  status?: EquipmentStatus
  type?: string
  page?: number
  size?: number
}

export function useEquipment() {
  function listEquipment(siteId: string, options: EquipmentListFilter = {}): Promise<PageResponse<Equipment>> {
    const params = new URLSearchParams()
    if (options.name) params.set('name', options.name)
    if (options.status) params.set('status', options.status)
    if (options.type) params.set('type', options.type)
    params.set('page', String(options.page ?? 0))
    params.set('size', String(options.size ?? 20))
    return authFetch(`/api/construction-sites/${siteId}/equipment?${params.toString()}`)
  }

  function createEquipment(siteId: string, data: EquipmentRegistrationData): Promise<Equipment> {
    return authFetch(`/api/construction-sites/${siteId}/equipment`, { method: 'POST', body: JSON.stringify(data) })
  }

  function updateEquipmentStatus(equipmentId: string, status: EquipmentStatus): Promise<Equipment> {
    return authFetch(`/api/equipment/${equipmentId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })
  }

  return { listEquipment, createEquipment, updateEquipmentStatus }
}
