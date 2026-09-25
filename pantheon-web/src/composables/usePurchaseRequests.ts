import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { FornecedorInput } from './useFornecedores'
import type { PurchaseRequestApproverFunction } from './usePurchaseRequestApprovalLevels'

export type PurchaseRequestItemStatus = 'PENDING' | 'CONVERTED'
export type PurchaseRequestStatus = 'INICIADO' | 'ORCADO' | 'CONFERIDO' | 'CONCLUIDO'
export type PurchaseRequestApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** A `Page<T>` envelope as returned by Spring's paginated list endpoints. */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface LinkedOrcamentoSummary {
  id: string
  fornecedorNome: string
}

export interface PurchaseRequest {
  id: string
  constructionSiteId: string
  name: string
  status: PurchaseRequestStatus
  createdBy: string
  createdByName: string | null
  createdAt: string
  submittedAt: string | null
  approvedAt: string | null
  completedAt: string | null
  lastRejectionReason: string | null
  linkedOrcamentos: LinkedOrcamentoSummary[]
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
  selectedOrcamentoLineItemId: string | null
}

export interface PurchaseRequestItemCreationData {
  name: string
  type: string | null
  quantity: string
  unit: string | null
}

export interface PurchaseRequestApproval {
  id: string
  purchaseRequestId: string
  cycleNumber: number
  stepOrder: number
  approverFunction: PurchaseRequestApproverFunction
  status: PurchaseRequestApprovalStatus
  decidedBySiteMembershipId: string | null
  decidedAt: string | null
  comment: string | null
  createdAt: string
}

export interface PurchaseRequestDetail {
  purchaseRequest: PurchaseRequest
  items: PurchaseRequestItem[]
  approvals: PurchaseRequestApproval[]
}

export interface PurchaseRequestListFilter {
  date?: string
  status?: PurchaseRequestStatus
  page?: number
  size?: number
}

export interface ComparisonColumn {
  orcamentoId: string
  supplierName: string
}

export interface ComparisonCell {
  orcamentoId: string
  lineItemId: string
  unitPrice: string | null
  quantity: string
  selected: boolean
}

export interface ComparisonRow {
  itemId: string
  itemName: string
  quantity: string
  unit: string | null
  cells: ComparisonCell[]
}

export interface PurchaseRequestComparison {
  columns: ComparisonColumn[]
  rows: ComparisonRow[]
}

export interface PurchaseRequestInvoice {
  id: string
  purchaseRequestId: string
  originalName: string
  contentType: string
  uploadedBy: string
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

export function usePurchaseRequests() {
  function listPurchaseRequests(
    siteId: string,
    filter: PurchaseRequestListFilter = {},
  ): Promise<PageResponse<PurchaseRequest>> {
    const params = new URLSearchParams()
    if (filter.date) params.set('date', filter.date)
    if (filter.status) params.set('status', filter.status)
    params.set('page', String(filter.page ?? 0))
    params.set('size', String(filter.size ?? 20))
    return authFetch(`/api/construction-sites/${siteId}/purchase-requests?${params.toString()}`)
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

  function addPurchaseRequestItems(
    purchaseRequestId: string,
    items: PurchaseRequestItemCreationData[],
  ): Promise<PurchaseRequestItem[]> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/items`, {
      method: 'POST',
      body: JSON.stringify({ items }),
    })
  }

  function deletePurchaseRequest(purchaseRequestId: string): Promise<void> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}`, { method: 'DELETE' })
  }

  function removePurchaseRequestItem(purchaseRequestId: string, itemId: string): Promise<void> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/items/${itemId}`, { method: 'DELETE' })
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

  function setItemSelection(
    purchaseRequestId: string,
    itemId: string,
    orcamentoLineItemId: string | null,
  ): Promise<PurchaseRequestItem> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/items/${itemId}/selection`, {
      method: 'PUT',
      body: JSON.stringify({ orcamentoLineItemId }),
    })
  }

  function getComparison(purchaseRequestId: string): Promise<PurchaseRequestComparison> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/comparison`)
  }

  function getSupplierPdfBlob(
    purchaseRequestId: string,
    orcamentoId: string,
  ): Promise<{ blob: Blob; filename: string | null }> {
    return authFetchBlob(`/api/purchase-requests/${purchaseRequestId}/orcamentos/${orcamentoId}/pdf`)
  }

  function getSummaryPdfBlob(purchaseRequestId: string): Promise<{ blob: Blob; filename: string | null }> {
    return authFetchBlob(`/api/purchase-requests/${purchaseRequestId}/summary-pdf`)
  }

  function submitForApproval(purchaseRequestId: string): Promise<PurchaseRequest> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/submit`, { method: 'POST' })
  }

  function approveStep(purchaseRequestId: string, comment?: string): Promise<PurchaseRequest> {
    const query = comment ? `?comment=${encodeURIComponent(comment)}` : ''
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/approve-step${query}`, { method: 'POST' })
  }

  function rejectStep(purchaseRequestId: string, reason: string): Promise<PurchaseRequest> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/reject-step`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    })
  }

  function conclude(purchaseRequestId: string): Promise<PurchaseRequest> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/conclude`, { method: 'POST' })
  }

  function uploadInvoice(purchaseRequestId: string, file: File): Promise<PurchaseRequestInvoice> {
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/purchase-requests/${purchaseRequestId}/invoices`, formData)
  }

  function listInvoices(purchaseRequestId: string): Promise<PurchaseRequestInvoice[]> {
    return authFetch(`/api/purchase-requests/${purchaseRequestId}/invoices`)
  }

  function deleteInvoice(invoiceId: string): Promise<void> {
    return authFetch(`/api/purchase-request-invoices/${invoiceId}`, { method: 'DELETE' })
  }

  function getInvoiceContentBlob(invoiceId: string): Promise<{ blob: Blob; filename: string | null }> {
    return authFetchBlob(`/api/purchase-request-invoices/${invoiceId}/content`)
  }

  return {
    listPurchaseRequests,
    createPurchaseRequest,
    getPurchaseRequest,
    addPurchaseRequestItems,
    deletePurchaseRequest,
    removePurchaseRequestItem,
    convertToOrcamento,
    setItemSelection,
    getComparison,
    getSupplierPdfBlob,
    getSummaryPdfBlob,
    submitForApproval,
    approveStep,
    rejectStep,
    conclude,
    uploadInvoice,
    listInvoices,
    deleteInvoice,
    getInvoiceContentBlob,
  }
}
