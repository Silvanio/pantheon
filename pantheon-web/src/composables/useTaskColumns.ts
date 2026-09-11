import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export interface TaskColumn {
  id: string
  companyId: string
  name: string
  sortOrder: number
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

export function useTaskColumns() {
  function listColumns(companyId: string): Promise<TaskColumn[]> {
    return authFetch(`/api/companies/${companyId}/task-columns`)
  }

  function createColumn(companyId: string, name: string): Promise<TaskColumn> {
    return authFetch(`/api/companies/${companyId}/task-columns`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    })
  }

  function renameColumn(companyId: string, columnId: string, name: string): Promise<TaskColumn> {
    return authFetch(`/api/companies/${companyId}/task-columns/${columnId}`, {
      method: 'PUT',
      body: JSON.stringify({ name }),
    })
  }

  function reorderColumn(companyId: string, columnId: string, sortOrder: number): Promise<TaskColumn> {
    return authFetch(`/api/companies/${companyId}/task-columns/${columnId}/sort-order`, {
      method: 'PUT',
      body: JSON.stringify({ sortOrder }),
    })
  }

  function deleteColumn(companyId: string, columnId: string): Promise<void> {
    return authFetch(`/api/companies/${companyId}/task-columns/${columnId}`, { method: 'DELETE' })
  }

  return { listColumns, createColumn, renameColumn, reorderColumn, deleteColumn }
}
