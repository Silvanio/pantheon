import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export interface SiteDocumentProject {
  id: string
  constructionSiteId: string
  name: string
  createdBy: string
  createdAt: string
}

export interface SiteDocumentProjectAttachment {
  id: string
  siteDocumentProjectId: string
  originalName: string
  contentType: string
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

async function authFetchBlob(path: string): Promise<Blob> {
  const { token } = useAuth()
  const response = await fetch(`${SERVICE_BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${token.value}` },
  })
  if (!response.ok) {
    throw new HttpError(response.status, `Request to ${path} failed with status ${response.status}`)
  }
  return response.blob()
}

export function useSiteDocumentProjects() {
  function listProjects(siteId: string): Promise<SiteDocumentProject[]> {
    return authFetch(`/api/sites/${siteId}/projects`)
  }

  function createProject(siteId: string, name: string): Promise<SiteDocumentProject> {
    return authFetch(`/api/sites/${siteId}/projects`, { method: 'POST', body: JSON.stringify({ name }) })
  }

  function listAttachments(projectId: string): Promise<SiteDocumentProjectAttachment[]> {
    return authFetch(`/api/site-projects/${projectId}/attachments`)
  }

  function uploadAttachment(projectId: string, file: File): Promise<SiteDocumentProjectAttachment> {
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/site-projects/${projectId}/attachments`, formData)
  }

  function getAttachmentContentBlob(projectId: string, attachmentId: string): Promise<Blob> {
    return authFetchBlob(`/api/site-projects/${projectId}/attachments/${attachmentId}/content`)
  }

  return { listProjects, createProject, listAttachments, uploadAttachment, getAttachmentContentBlob }
}
