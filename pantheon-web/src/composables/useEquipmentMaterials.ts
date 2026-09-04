import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

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

export interface MaterialItem {
  id: string
  constructionSiteId: string
  name: string
  unit: string
}

export interface MaterialRegistrationData {
  name: string
  unit: string
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

export function useEquipmentMaterials() {
  function listEquipment(siteId: string): Promise<Equipment[]> {
    return authFetch(`/api/construction-sites/${siteId}/equipment`)
  }

  function createEquipment(siteId: string, data: EquipmentRegistrationData): Promise<Equipment> {
    return authFetch(`/api/construction-sites/${siteId}/equipment`, { method: 'POST', body: JSON.stringify(data) })
  }

  function updateEquipmentStatus(equipmentId: string, status: EquipmentStatus): Promise<Equipment> {
    return authFetch(`/api/equipment/${equipmentId}/status`, { method: 'PATCH', body: JSON.stringify({ status }) })
  }

  function listMaterials(siteId: string): Promise<MaterialItem[]> {
    return authFetch(`/api/construction-sites/${siteId}/materials`)
  }

  function createMaterial(siteId: string, data: MaterialRegistrationData): Promise<MaterialItem> {
    return authFetch(`/api/construction-sites/${siteId}/materials`, { method: 'POST', body: JSON.stringify(data) })
  }

  return { listEquipment, createEquipment, updateEquipmentStatus, listMaterials, createMaterial }
}
