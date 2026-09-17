import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export interface SiteDocumentFolder {
  id: string
  constructionSiteId: string
  parentId: string | null
  name: string
  taskCardId: string | null
  linkedTaskTitle: string | null
  createdBy: string
  createdByName: string | null
  createdAt: string
  updatedBy: string
  updatedByName: string | null
  updatedAt: string
}

export interface SiteDocumentFile {
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

export interface SiteDocumentFolderContents {
  folders: SiteDocumentFolder[]
  files: SiteDocumentFile[]
}

export interface SiteDocumentBreadcrumb {
  id: string
  name: string
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

export function useSiteDocumentProjects() {
  function listContents(siteId: string, parentId: string | null): Promise<SiteDocumentFolderContents> {
    const query = parentId ? `?parentId=${parentId}` : ''
    return authFetch(`/api/sites/${siteId}/projects/contents${query}`)
  }

  function getBreadcrumbs(folderId: string): Promise<SiteDocumentBreadcrumb[]> {
    return authFetch(`/api/site-projects/${folderId}/breadcrumbs`)
  }

  function createFolder(
    siteId: string,
    name: string,
    parentId: string | null,
    taskCardId: string | null = null,
  ): Promise<SiteDocumentFolder> {
    return authFetch(`/api/sites/${siteId}/projects`, {
      method: 'POST',
      body: JSON.stringify({ name, parentId, taskCardId }),
    })
  }

  function renameFolder(folderId: string, name: string): Promise<SiteDocumentFolder> {
    return authFetch(`/api/site-projects/${folderId}/name`, { method: 'PATCH', body: JSON.stringify({ name }) })
  }

  function setFolderTaskLink(folderId: string, taskCardId: string): Promise<SiteDocumentFolder> {
    return authFetch(`/api/site-projects/${folderId}/task-link`, {
      method: 'PUT',
      body: JSON.stringify({ taskCardId }),
    })
  }

  function clearFolderTaskLink(folderId: string): Promise<void> {
    return authFetch(`/api/site-projects/${folderId}/task-link`, { method: 'DELETE' })
  }

  function deleteFolder(folderId: string): Promise<void> {
    return authFetch(`/api/site-projects/${folderId}`, { method: 'DELETE' })
  }

  function uploadFile(siteId: string, parentId: string | null, file: File): Promise<SiteDocumentFile> {
    const query = parentId ? `?parentId=${parentId}` : ''
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/sites/${siteId}/projects/attachments${query}`, formData)
  }

  function deleteFile(attachmentId: string): Promise<void> {
    return authFetch(`/api/site-project-attachments/${attachmentId}`, { method: 'DELETE' })
  }

  function getFileContentBlob(attachmentId: string): Promise<{ blob: Blob; filename: string | null }> {
    return authFetchBlob(`/api/site-project-attachments/${attachmentId}/content`)
  }

  return {
    listContents,
    getBreadcrumbs,
    createFolder,
    renameFolder,
    setFolderTaskLink,
    clearFolderTaskLink,
    deleteFolder,
    uploadFile,
    deleteFile,
    getFileContentBlob,
  }
}
