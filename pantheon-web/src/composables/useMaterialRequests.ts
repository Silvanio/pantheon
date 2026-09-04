import { SERVICE_BASE_URL } from '../lib/config'
import { useAuth } from './useAuth'

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
    throw new Error(`Request to ${path} failed with status ${response.status}`)
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

  return { listRequests, createRequest, getDetail, approve, reject, recordVerification }
}
