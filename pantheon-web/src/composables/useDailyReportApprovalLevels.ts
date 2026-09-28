import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'

/**
 * Functions eligible to be configured as a Diário de Obra approval step. Mirrors
 * `usePurchaseRequestApprovalLevels`'s `PurchaseRequestApproverFunction` set (company staff acts
 * through the `companyStaff` bypass regardless of function, so `ADMIN` is intentionally excluded
 * here too).
 */
export type DailyReportApproverFunction =
  | 'CLIENT'
  | 'ARCHITECT'
  | 'ENGINEER'
  | 'SITE_FOREMAN'
  | 'SERVICE_PROVIDER'
  | 'OTHER'

export interface SiteDailyReportApprovalLevel {
  id: string
  constructionSiteId: string
  stepOrder: number
  approverFunction: DailyReportApproverFunction
}

export interface DailyReportApprovalLevelInput {
  stepOrder: number
  approverFunction: DailyReportApproverFunction
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

export function useDailyReportApprovalLevels() {
  function getApprovalLevels(siteId: string): Promise<SiteDailyReportApprovalLevel[]> {
    return authFetch(`/api/construction-sites/${siteId}/daily-report-approval-levels`)
  }

  function setApprovalLevels(
    siteId: string,
    levels: DailyReportApprovalLevelInput[],
  ): Promise<SiteDailyReportApprovalLevel[]> {
    return authFetch(`/api/construction-sites/${siteId}/daily-report-approval-levels`, {
      method: 'PUT',
      body: JSON.stringify({ levels }),
    })
  }

  return { getApprovalLevels, setApprovalLevels }
}
