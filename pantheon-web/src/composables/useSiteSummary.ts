import { SERVICE_BASE_URL } from '../lib/config'
import { useAuth } from './useAuth'

export interface RecentItem {
  id: string
  title: string
  createdAt: string
}

export interface CountStat {
  total: number
  recent: RecentItem[]
}

export interface PurchaseRequestSummary {
  total: number
  awaitingApproval: number
  recent: RecentItem[]
}

export interface OrcamentoSummary {
  total: number
  draft: number
  recent: RecentItem[]
}

export interface DailyReportSummary {
  total: number
  lastReportDate: string | null
}

export interface EquipmentSummary {
  total: number
  unavailable: number
}

export interface SiteSummary {
  schedulePercentComplete: number | null
  dailyReports: DailyReportSummary | null
  purchaseRequests: PurchaseRequestSummary | null
  orcamentos: OrcamentoSummary | null
  equipment: EquipmentSummary | null
  projects: CountStat | null
  tasks: CountStat | null
  teamMembersCount: number | null
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

export function useSiteSummary() {
  function getSummary(siteId: string): Promise<SiteSummary> {
    return authFetch(`/api/construction-sites/${siteId}/summary`)
  }

  return { getSummary }
}
