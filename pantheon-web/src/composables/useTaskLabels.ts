import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { TaskLabel } from './useTaskCards'

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

export function useTaskLabels() {
  function listCompanyLabels(companyId: string): Promise<TaskLabel[]> {
    return authFetch(`/api/companies/${companyId}/task-labels`)
  }

  function createCompanyLabel(companyId: string, name: string, colorHex: string): Promise<TaskLabel> {
    return authFetch(`/api/companies/${companyId}/task-labels`, {
      method: 'POST',
      body: JSON.stringify({ name, colorHex }),
    })
  }

  function deleteCompanyLabel(companyId: string, labelId: string): Promise<void> {
    return authFetch(`/api/companies/${companyId}/task-labels/${labelId}`, { method: 'DELETE' })
  }

  return { listCompanyLabels, createCompanyLabel, deleteCompanyLabel }
}
