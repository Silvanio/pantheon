import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { Material } from './useMaterialDeliveries'
import type { FornecedorInput } from './useFornecedores'

export type OrcamentoStatus = 'DRAFT' | 'IN_APPROVAL' | 'APPROVED' | 'COMPLETED'
export type OrcamentoApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type OrcamentoApproverFunction = 'CLIENT' | 'ARCHITECT' | 'ENGINEER' | 'SITE_FOREMAN' | 'SERVICE_PROVIDER' | 'OTHER'

export interface Orcamento {
  id: string
  constructionSiteId: string
  status: OrcamentoStatus
  createdBy: string
  createdAt: string
  submittedAt: string | null
  approvedAt: string | null
  completedAt: string | null
  currentApprovalCycle: number
  lastRejectionReason: string | null
  fornecedorCnpj: string
  fornecedorNome: string
  fornecedorEndereco: string | null
  fornecedorContatoNome: string | null
  fornecedorContatoTelefone: string | null
  sourcePurchaseRequestId: string | null
  sourcePurchaseRequestName: string | null
}

export interface OrcamentoListFilter {
  date?: string
  purchaseRequestId?: string
}

export interface OrcamentoLineItem {
  id: string
  orcamentoId: string
  name: string
  type: string | null
  quantity: string
  unitPrice: string | null
  sourcePurchaseRequestItemId: string | null
}

export interface OrcamentoLineItemInput {
  name: string
  type: string | null
  quantity: string
  unitPrice: string | null
}

export interface OrcamentoApproval {
  id: string
  orcamentoId: string
  cycleNumber: number
  stepOrder: number
  approverFunction: OrcamentoApproverFunction
  status: OrcamentoApprovalStatus
  decidedBySiteMembershipId: string | null
  decidedAt: string | null
  comment: string | null
  createdAt: string
}

export interface OrcamentoDetail {
  orcamento: Orcamento
  lineItems: OrcamentoLineItem[]
  approvals: OrcamentoApproval[]
  materials: Material[]
}

export interface SiteOrcamentoApprovalLevel {
  id: string
  constructionSiteId: string
  stepOrder: number
  approverFunction: OrcamentoApproverFunction
}

export interface OrcamentoApprovalLevelInput {
  stepOrder: number
  approverFunction: OrcamentoApproverFunction
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

export function useOrcamentos() {
  function listOrcamentos(siteId: string, filter: OrcamentoListFilter = {}): Promise<Orcamento[]> {
    const params = new URLSearchParams()
    if (filter.date) params.set('date', filter.date)
    if (filter.purchaseRequestId) params.set('purchaseRequestId', filter.purchaseRequestId)
    const query = params.toString() ? `?${params.toString()}` : ''
    return authFetch(`/api/construction-sites/${siteId}/orcamentos${query}`)
  }

  function createOrcamento(
    siteId: string,
    items: OrcamentoLineItemInput[],
    fornecedor: FornecedorInput,
  ): Promise<Orcamento> {
    return authFetch(`/api/construction-sites/${siteId}/orcamentos`, {
      method: 'POST',
      body: JSON.stringify({ items, fornecedor }),
    })
  }

  function getOrcamento(orcamentoId: string): Promise<OrcamentoDetail> {
    return authFetch(`/api/orcamentos/${orcamentoId}`)
  }

  function addLineItem(orcamentoId: string, item: OrcamentoLineItemInput): Promise<OrcamentoLineItem> {
    return authFetch(`/api/orcamentos/${orcamentoId}/line-items`, { method: 'POST', body: JSON.stringify(item) })
  }

  function updateLineItem(
    orcamentoId: string,
    lineItemId: string,
    item: OrcamentoLineItemInput,
  ): Promise<OrcamentoLineItem> {
    return authFetch(`/api/orcamentos/${orcamentoId}/line-items/${lineItemId}`, {
      method: 'PUT',
      body: JSON.stringify(item),
    })
  }

  function removeLineItem(orcamentoId: string, lineItemId: string): Promise<void> {
    return authFetch(`/api/orcamentos/${orcamentoId}/line-items/${lineItemId}`, { method: 'DELETE' })
  }

  function submitForApproval(orcamentoId: string): Promise<Orcamento> {
    return authFetch(`/api/orcamentos/${orcamentoId}/submit`, { method: 'POST' })
  }

  function approveStep(orcamentoId: string, comment?: string): Promise<Orcamento> {
    const query = comment ? `?comment=${encodeURIComponent(comment)}` : ''
    return authFetch(`/api/orcamentos/${orcamentoId}/approve-step${query}`, { method: 'POST' })
  }

  function rejectStep(orcamentoId: string, reason: string): Promise<Orcamento> {
    return authFetch(`/api/orcamentos/${orcamentoId}/reject-step`, { method: 'POST', body: JSON.stringify({ reason }) })
  }

  function conclude(orcamentoId: string): Promise<Orcamento> {
    return authFetch(`/api/orcamentos/${orcamentoId}/conclude`, { method: 'POST' })
  }

  function getApprovalLevels(siteId: string): Promise<SiteOrcamentoApprovalLevel[]> {
    return authFetch(`/api/construction-sites/${siteId}/orcamento-approval-levels`)
  }

  function setApprovalLevels(
    siteId: string,
    levels: OrcamentoApprovalLevelInput[],
  ): Promise<SiteOrcamentoApprovalLevel[]> {
    return authFetch(`/api/construction-sites/${siteId}/orcamento-approval-levels`, {
      method: 'PUT',
      body: JSON.stringify({ levels }),
    })
  }

  return {
    listOrcamentos,
    createOrcamento,
    getOrcamento,
    addLineItem,
    updateLineItem,
    removeLineItem,
    submitForApproval,
    approveStep,
    rejectStep,
    conclude,
    getApprovalLevels,
    setApprovalLevels,
  }
}
