import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

export type MaterialRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'PARTIALLY_RECEIVED' | 'RECEIVED'

export interface MaterialRequest {
  id: string
  constructionSiteId: string
  status: MaterialRequestStatus
  requestedBy: string
  decidedBy: string | null
  decisionNote: string | null
  decidedAt: string | null
}

export interface MaterialRequestItem {
  id: string
  materialId: string
  requestedQuantity: string
}

export interface ReceiptVerification {
  id: string
  materialRequestItemId: string
  receivedQuantity: string
  requestedQuantity: string | null
  divergent: boolean
  note: string | null
  verifiedAt: string
}

export interface MaterialRequestDetail {
  request: MaterialRequest
  items: MaterialRequestItem[]
  verifications: ReceiptVerification[]
}

export interface MaterialRequestItemInput {
  materialId: string
  requestedQuantity: string
}

export type OrcamentoStatus = 'DRAFT' | 'SENT' | 'APPROVED' | 'REJECTED'
export type AttachmentKind = 'PAYMENT_PROOF' | 'INVOICE'

export interface Orcamento {
  id: string
  materialRequestId: string
  status: OrcamentoStatus
  createdBy: string
  createdAt: string
  sentAt: string | null
  decidedAt: string | null
  rejectionReason: string | null
}

export interface OrcamentoLineItem {
  id: string
  materialRequestItemId: string
  unitPrice: string
}

export interface OrcamentoAttachment {
  id: string
  orcamentoId: string
  kind: AttachmentKind
  originalName: string
  contentType: string
  createdAt: string
}

export interface OrcamentoDetail {
  orcamento: Orcamento
  items: OrcamentoLineItem[]
  attachments: OrcamentoAttachment[]
}

export interface OrcamentoLineItemInput {
  materialRequestItemId: string
  unitPrice: string
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

export function useMaterialRequests() {
  function listRequests(siteId: string, status?: MaterialRequestStatus | ''): Promise<MaterialRequest[]> {
    const query = status ? `?status=${status}` : ''
    return authFetch(`/api/construction-sites/${siteId}/material-requests${query}`)
  }

  function createRequest(siteId: string, items: MaterialRequestItemInput[]): Promise<MaterialRequest> {
    return authFetch(`/api/construction-sites/${siteId}/material-requests`, {
      method: 'POST',
      body: JSON.stringify({ items }),
    })
  }

  function getDetail(requestId: string): Promise<MaterialRequestDetail> {
    return authFetch(`/api/material-requests/${requestId}`)
  }

  function approve(requestId: string): Promise<MaterialRequest> {
    return authFetch(`/api/material-requests/${requestId}/approve`, { method: 'POST' })
  }

  function reject(requestId: string, reason: string): Promise<MaterialRequest> {
    return authFetch(`/api/material-requests/${requestId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    })
  }

  function recordVerification(
    requestId: string,
    materialRequestItemId: string,
    receivedQuantity: string,
    note: string | null,
  ): Promise<ReceiptVerification> {
    return authFetch(`/api/material-requests/${requestId}/receipt-verifications`, {
      method: 'POST',
      body: JSON.stringify({ materialRequestItemId, receivedQuantity, note }),
    })
  }

  function uploadVerificationPhoto(requestId: string, verificationId: string, file: File): Promise<unknown> {
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/material-requests/${requestId}/receipt-verifications/${verificationId}/photos`, formData)
  }

  function listOrcamentos(requestId: string): Promise<Orcamento[]> {
    return authFetch(`/api/material-requests/${requestId}/orcamentos`)
  }

  function createOrcamento(requestId: string, items: OrcamentoLineItemInput[]): Promise<Orcamento> {
    return authFetch(`/api/material-requests/${requestId}/orcamentos`, {
      method: 'POST',
      body: JSON.stringify({ items }),
    })
  }

  function getOrcamento(id: string): Promise<OrcamentoDetail> {
    return authFetch(`/api/orcamentos/${id}`)
  }

  function sendOrcamento(id: string): Promise<Orcamento> {
    return authFetch(`/api/orcamentos/${id}/send`, { method: 'POST' })
  }

  function approveOrcamento(id: string): Promise<Orcamento> {
    return authFetch(`/api/orcamentos/${id}/approve`, { method: 'POST' })
  }

  function rejectOrcamento(id: string, reason: string): Promise<Orcamento> {
    return authFetch(`/api/orcamentos/${id}/reject`, { method: 'POST', body: JSON.stringify({ reason }) })
  }

  function uploadOrcamentoAttachment(id: string, kind: AttachmentKind, file: File): Promise<OrcamentoAttachment> {
    const formData = new FormData()
    formData.set('kind', kind)
    formData.set('file', file)
    return authUpload(`/api/orcamentos/${id}/attachments`, formData)
  }

  return {
    listRequests,
    createRequest,
    getDetail,
    approve,
    reject,
    recordVerification,
    uploadVerificationPhoto,
    listOrcamentos,
    createOrcamento,
    getOrcamento,
    sendOrcamento,
    approveOrcamento,
    rejectOrcamento,
    uploadOrcamentoAttachment,
  }
}
