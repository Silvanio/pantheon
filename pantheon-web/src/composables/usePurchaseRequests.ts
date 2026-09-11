import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { FornecedorInput } from './useFornecedores'

export type PurchaseRequestItemStatus = 'PENDING' | 'CONVERTED'

export interface PurchaseRequest {
  id: string
  constructionSiteId: string
  name: string
  createdBy: string
  createdAt: string
}

export interface PurchaseRequestItem {
  id: string
  constructionSiteId: string
  purchaseRequestId: string
  name: string
  type: string | null
  quantity: string
  unit: string | null
  status: PurchaseRequestItemStatus
  createdBy: string
  createdAt: string
  convertedToOrcamentoId: string | null
  convertedAt: string | null
}

export interface PurchaseRequestItemCreationData {
  name: string
  type: string | null
  quantity: string
  unit: string | null
}

export interface PurchaseRequestDetail {
  purchaseRequest: PurchaseRequest
  items: PurchaseRequestItem[]
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

export function usePurchaseRequests() {
  function listPurchaseRequests(siteId: string, date?: string): Promise<PurchaseRequest[]> {
    const query = date ? `?date=${date}` : ''
    return authFetch(`/api/construction-sites/${siteId}/purchase-requests${query}`)
  }

  function createPurchaseRequest(siteId: string, items: PurchaseRequestItemCreationData[]): Promise<PurchaseRequest> {
    return authFetch(`/api/construction-sites/${siteId}/purchase-requests`, {
      method: 'POST',
      body: JSON.stringify({ items }),
    })
  }

  function getPurchaseRequest(purchaseRequestId: string): Promise<PurchaseRequestDetail> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}`)
  }

  function convertToOrcamento(
    purchaseRequestId: string,
    itemIds: string[],
    fornecedor: FornecedorInput,
  ): Promise<{ id: string }> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/convert-to-orcamento`, {
      method: 'POST',
      body: JSON.stringify({ itemIds, fornecedor }),
    })
  }

  return { listPurchaseRequests, createPurchaseRequest, getPurchaseRequest, convertToOrcamento }
}
