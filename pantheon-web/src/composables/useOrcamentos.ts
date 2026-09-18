import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { Material } from './useMaterialDeliveries'
import type { FornecedorInput } from './useFornecedores'
import type { PageResponse } from './usePurchaseRequests'

export type OrcamentoStatus = 'DRAFT' | 'LOCKED'

export interface Orcamento {
  id: string
  constructionSiteId: string
  status: OrcamentoStatus
  createdBy: string
  createdAt: string
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
  supplier?: string
  page?: number
  size?: number
}

export interface OrcamentoLineItem {
  id: string
  orcamentoId: string
  name: string
  type: string | null
  quantity: string
  unitPrice: string | null
  sourcePurchaseRequestItemId: string | null
  selected: boolean
}

export interface OrcamentoLineItemInput {
  name: string
  type: string | null
  quantity: string
  unitPrice: string | null
}

export interface OrcamentoDetail {
  orcamento: Orcamento
  lineItems: OrcamentoLineItem[]
  materials: Material[]
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
  function listOrcamentos(siteId: string, filter: OrcamentoListFilter = {}): Promise<PageResponse<Orcamento>> {
    const params = new URLSearchParams()
    if (filter.date) params.set('date', filter.date)
    if (filter.purchaseRequestId) params.set('purchaseRequestId', filter.purchaseRequestId)
    if (filter.supplier) params.set('supplier', filter.supplier)
    params.set('page', String(filter.page ?? 0))
    params.set('size', String(filter.size ?? 20))
    return authFetch(`/api/construction-sites/${siteId}/orcamentos?${params.toString()}`)
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

  return {
    listOrcamentos,
    createOrcamento,
    getOrcamento,
    addLineItem,
    updateLineItem,
    removeLineItem,
  }
}
