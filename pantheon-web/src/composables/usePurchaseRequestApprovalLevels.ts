import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

/**
 * Functions eligible to be configured as a Pedido de Compra approval step. Mirrors the set the
 * old orçamento-approval-level configuration screen offered (company staff acts through
 * `companyStaff` bypass regardless of function, so `ADMIN` is intentionally excluded here).
 */
export type PurchaseRequestApproverFunction =
  | 'CLIENT'
  | 'ARCHITECT'
  | 'ENGINEER'
  | 'SITE_FOREMAN'
  | 'SERVICE_PROVIDER'
  | 'OTHER'

export interface SitePurchaseRequestApprovalLevel {
  id: string
  constructionSiteId: string
  stepOrder: number
  approverFunction: PurchaseRequestApproverFunction
}

export interface PurchaseRequestApprovalLevelInput {
  stepOrder: number
  approverFunction: PurchaseRequestApproverFunction
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

export function usePurchaseRequestApprovalLevels() {
  function getApprovalLevels(siteId: string): Promise<SitePurchaseRequestApprovalLevel[]> {
    return authFetch(`/api/construction-sites/${siteId}/purchase-request-approval-levels`)
  }

  function setApprovalLevels(
    siteId: string,
    levels: PurchaseRequestApprovalLevelInput[],
  ): Promise<SitePurchaseRequestApprovalLevel[]> {
    return authFetch(`/api/construction-sites/${siteId}/purchase-request-approval-levels`, {
      method: 'PUT',
      body: JSON.stringify({ levels }),
    })
  }

  return { getApprovalLevels, setApprovalLevels }
}
