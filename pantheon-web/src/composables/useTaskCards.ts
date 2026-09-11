import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { TaskColumn } from './useTaskColumns'

export interface TaskCard {
  id: string
  constructionSiteId: string
  columnId: string
  title: string
  description: string | null
  sortOrder: number
  labelIds: string[]
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface TaskBoard {
  columns: TaskColumn[]
  cards: TaskCard[]
}

export interface TaskLabel {
  id: string
  constructionSiteId: string
  name: string
  colorHex: string
}

export interface TaskComment {
  id: string
  cardId: string
  authorId: string
  body: string
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
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export function useTaskCards() {
  function getBoard(siteId: string): Promise<TaskBoard> {
    return authFetch(`/api/construction-sites/${siteId}/task-board`)
  }

  function createCard(siteId: string, columnId: string, title: string, description: string | null): Promise<TaskCard> {
    return authFetch(`/api/construction-sites/${siteId}/task-cards`, {
      method: 'POST',
      body: JSON.stringify({ columnId, title, description }),
    })
  }

  function moveCard(cardId: string, columnId: string, sortOrder: number): Promise<TaskCard> {
    return authFetch(`/api/task-cards/${cardId}/move`, {
      method: 'PATCH',
      body: JSON.stringify({ columnId, sortOrder }),
    })
  }

  function listLabels(siteId: string): Promise<TaskLabel[]> {
    return authFetch(`/api/construction-sites/${siteId}/task-labels`)
  }

  function createLabel(siteId: string, name: string, colorHex: string): Promise<TaskLabel> {
    return authFetch(`/api/construction-sites/${siteId}/task-labels`, {
      method: 'POST',
      body: JSON.stringify({ name, colorHex }),
    })
  }

  function attachLabel(cardId: string, labelId: string): Promise<void> {
    return authFetch(`/api/task-cards/${cardId}/labels/${labelId}`, { method: 'POST' })
  }

  function detachLabel(cardId: string, labelId: string): Promise<void> {
    return authFetch(`/api/task-cards/${cardId}/labels/${labelId}`, { method: 'DELETE' })
  }

  function listComments(cardId: string): Promise<TaskComment[]> {
    return authFetch(`/api/task-cards/${cardId}/comments`)
  }

  function addComment(cardId: string, body: string): Promise<TaskComment> {
    return authFetch(`/api/task-cards/${cardId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ body }),
    })
  }

  return {
    getBoard,
    createCard,
    moveCard,
    listLabels,
    createLabel,
    attachLabel,
    detachLabel,
    listComments,
    addComment,
  }
}
