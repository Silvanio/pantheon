import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { TaskColumn } from './useTaskColumns'

export interface TaskCard {
  id: string
  constructionSiteId: string
  columnId: string
  title: string
  description: string | null
  dueDate: string | null
  sortOrder: number
  labelIds: string[]
  assigneeIds: string[]
  commentCount: number
  attachmentCount: number
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface TaskBoard {
  columns: TaskColumn[]
  cards: TaskCard[]
  labels: TaskLabel[]
}

export interface TaskLabel {
  id: string
  companyId: string | null
  cardId: string | null
  name: string
  colorHex: string
}

export interface TaskComment {
  id: string
  cardId: string
  authorId: string
  authorName: string | null
  body: string
  createdAt: string
}

export interface TaskCardAttachment {
  id: string
  constructionSiteId: string
  siteDocumentProjectId: string | null
  originalName: string
  contentType: string
  taskCardId: string | null
  uploadedBy: string
  uploadedByName: string | null
  createdAt: string
  folderPath: string | null
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

async function authUpload<T>(path: string, formData: FormData): Promise<T> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token.value}` },
    body: formData,
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

async function authFetchBlob(path: string): Promise<{ blob: Blob; filename: string | null }> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${token.value}` },
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = /filename="?([^"]+)"?/.exec(disposition)
  return { blob: await response.blob(), filename: match ? match[1] : null }
}

export function useTaskCards() {
  function getBoard(siteId: string): Promise<TaskBoard> {
    return authFetch(`/api/construction-sites/${siteId}/task-board`)
  }

  function createCard(
    siteId: string,
    columnId: string,
    title: string,
    description: string | null,
    dueDate: string | null = null,
  ): Promise<TaskCard> {
    return authFetch(`/api/construction-sites/${siteId}/task-cards`, {
      method: 'POST',
      body: JSON.stringify({ columnId, title, description, dueDate }),
    })
  }

  function moveCard(cardId: string, columnId: string, sortOrder: number): Promise<TaskCard> {
    return authFetch(`/api/task-cards/${cardId}/move`, {
      method: 'PATCH',
      body: JSON.stringify({ columnId, sortOrder }),
    })
  }

  function updateDueDate(cardId: string, dueDate: string | null): Promise<TaskCard> {
    return authFetch(`/api/task-cards/${cardId}/due-date`, {
      method: 'PATCH',
      body: JSON.stringify({ dueDate }),
    })
  }

  function deleteCard(cardId: string): Promise<void> {
    return authFetch(`/api/task-cards/${cardId}`, { method: 'DELETE' })
  }

  function createCustomLabel(cardId: string, name: string, colorHex: string): Promise<TaskLabel> {
    return authFetch(`/api/task-cards/${cardId}/custom-labels`, {
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

  function assignMember(cardId: string, siteMembershipId: string): Promise<void> {
    return authFetch(`/api/task-cards/${cardId}/assignees/${siteMembershipId}`, { method: 'POST' })
  }

  function unassignMember(cardId: string, siteMembershipId: string): Promise<void> {
    return authFetch(`/api/task-cards/${cardId}/assignees/${siteMembershipId}`, { method: 'DELETE' })
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

  function listCardAttachments(cardId: string): Promise<TaskCardAttachment[]> {
    return authFetch(`/api/task-cards/${cardId}/attachments`)
  }

  function attachFileToCard(cardId: string, parentId: string | null, file: File): Promise<TaskCardAttachment> {
    const query = parentId ? `?parentId=${parentId}` : ''
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/task-cards/${cardId}/attachments${query}`, formData)
  }

  function getCardAttachmentContentBlob(attachmentId: string): Promise<{ blob: Blob; filename: string | null }> {
    return authFetchBlob(`/api/site-project-attachments/${attachmentId}/content`)
  }

  return {
    getBoard,
    createCard,
    moveCard,
    updateDueDate,
    deleteCard,
    createCustomLabel,
    attachLabel,
    detachLabel,
    assignMember,
    unassignMember,
    listComments,
    addComment,
    listCardAttachments,
    attachFileToCard,
    getCardAttachmentContentBlob,
  }
}
