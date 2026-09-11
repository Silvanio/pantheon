import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { TaskColumn } from './useTaskColumns'

export interface GlobalTaskCard {
  id: string
  constructionSiteId: string
  siteName: string
  siteColorHex: string
  columnId: string
  title: string
  description: string | null
  sortOrder: number
  labelIds: string[]
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface GlobalTaskBoard {
  columns: TaskColumn[]
  cards: GlobalTaskCard[]
}

async function authFetch<T>(path: string): Promise<T> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${token.value}` },
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

export function useGlobalTasksBoard() {
  function getGlobalBoard(companyId: string): Promise<GlobalTaskBoard> {
    return authFetch(`/api/companies/${companyId}/tasks-board`)
  }

  return { getGlobalBoard }
}
