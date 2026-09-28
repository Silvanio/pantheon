import { SERVICE_BASE_URL } from '../lib/config'
import { HttpError, useAuth } from './useAuth'
import type { PageResponse } from './usePurchaseRequests'
import type { DailyReportApproverFunction } from './useDailyReportApprovalLevels'
import type { Material } from './useMaterialDeliveries'

export type DailyReportStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'APPROVED'
export type ActivityStatus = 'IN_PROGRESS' | 'COMPLETED'
export type DailyReportApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface DailyReport {
  id: string
  constructionSiteId: string
  reportDate: string
  sequenceNo: number
  status: DailyReportStatus
  weatherConditionMorning: string | null
  weatherConditionAfternoon: string | null
  weatherBlockedTasks: boolean | null
  workHoursStart: string | null
  workHoursEnd: string | null
  comments: string | null
  submittedAt: string | null
}

export interface DailyReportCoreUpdate {
  weatherConditionMorning: string | null
  weatherConditionAfternoon: string | null
  weatherBlockedTasks: boolean | null
  workHoursStart: string | null
  workHoursEnd: string | null
  comments: string | null
}

export interface WorkforceEntry {
  id: string
  membershipId: string | null
  roleDescription: string
  headcount: number
}

export interface EquipmentUsage {
  id: string
  equipmentId: string | null
  customName: string | null
  statusNote: string | null
}

export interface Activity {
  id: string
  description: string
  progressNote: string
  status: ActivityStatus
}

export interface Occurrence {
  id: string
  description: string
}

export interface MaterialReceived {
  id: string
  materialName: string
  unit: string | null
  quantity: string
}

export interface DailyReportApproval {
  id: string
  dailyReportId: string
  cycleNumber: number
  stepOrder: number
  approverFunction: DailyReportApproverFunction
  status: DailyReportApprovalStatus
  decidedBySiteMembershipId: string | null
  decidedByName: string | null
  decidedAt: string | null
  comment: string | null
  createdAt: string
}

export interface DailyReportDetail {
  report: DailyReport
  workforceEntries: WorkforceEntry[]
  equipmentUsage: EquipmentUsage[]
  activities: Activity[]
  occurrences: Occurrence[]
  materialsReceived: MaterialReceived[]
  approvals: DailyReportApproval[]
}

export type MediaKind = 'PHOTO' | 'VIDEO'

export interface ReportMedia {
  id: string
  type: MediaKind
  caption: string | null
  contentType: string
  uploadedAt: string
}

export interface ReportAttachment {
  id: string
  originalName: string
  contentType: string
  uploadedAt: string
}

/** A Pedido de Compra "nota fiscal" surfaced read-only in this report's Anexos list because it was
 * uploaded on the same calendar date as the report — see `daily-report-media-and-signoff` spec's
 * "Same-day Pedido de Compra invoice surfaced" scenario. Never copied, only referenced. */
export interface DailyReportImportedInvoice {
  id: string
  purchaseRequestId: string
  purchaseRequestName: string | null
  originalName: string
  contentType: string
  uploadedAt: string
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

export function useDailyReports() {
  function listReports(siteId: string, options: { page?: number; size?: number } = {}): Promise<PageResponse<DailyReport>> {
    const params = new URLSearchParams()
    params.set('page', String(options.page ?? 0))
    params.set('size', String(options.size ?? 20))
    return authFetch(`/api/construction-sites/${siteId}/daily-reports?${params.toString()}`)
  }

  function createReport(siteId: string, reportDate: string): Promise<DailyReport> {
    return authFetch(`/api/construction-sites/${siteId}/daily-reports`, {
      method: 'POST',
      body: JSON.stringify({ reportDate }),
    })
  }

  function getDetail(reportId: string): Promise<DailyReportDetail> {
    return authFetch(`/api/daily-reports/${reportId}`)
  }

  function updateCore(reportId: string, data: DailyReportCoreUpdate): Promise<DailyReport> {
    return authFetch(`/api/daily-reports/${reportId}`, { method: 'PATCH', body: JSON.stringify(data) })
  }

  function submitReport(reportId: string): Promise<DailyReport> {
    return authFetch(`/api/daily-reports/${reportId}/submit`, { method: 'POST' })
  }

  function approveStep(reportId: string, comment?: string): Promise<DailyReport> {
    const query = comment ? `?comment=${encodeURIComponent(comment)}` : ''
    return authFetch(`/api/daily-reports/${reportId}/approve-step${query}`, { method: 'POST' })
  }

  function rejectStep(reportId: string, reason: string): Promise<DailyReport> {
    return authFetch(`/api/daily-reports/${reportId}/reject-step`, {
      method: 'POST',
      body: JSON.stringify({ reason }),
    })
  }

  function deleteReport(reportId: string): Promise<void> {
    return authFetch(`/api/daily-reports/${reportId}`, { method: 'DELETE' })
  }

  function addWorkforceEntry(
    reportId: string,
    data: { membershipId: string | null; roleDescription: string | null; headcount: number },
  ): Promise<WorkforceEntry> {
    return authFetch(`/api/daily-reports/${reportId}/workforce-entries`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function deleteWorkforceEntry(reportId: string, entryId: string): Promise<void> {
    return authFetch(`/api/daily-reports/${reportId}/workforce-entries/${entryId}`, { method: 'DELETE' })
  }

  function addEquipmentUsage(
    reportId: string,
    data: { equipmentId: string | null; customName: string | null; statusNote: string | null },
  ): Promise<EquipmentUsage> {
    return authFetch(`/api/daily-reports/${reportId}/equipment-usage`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function deleteEquipmentUsage(reportId: string, usageId: string): Promise<void> {
    return authFetch(`/api/daily-reports/${reportId}/equipment-usage/${usageId}`, { method: 'DELETE' })
  }

  function addActivity(
    reportId: string,
    data: { description: string; progressNote: string; status: ActivityStatus },
  ): Promise<Activity> {
    return authFetch(`/api/daily-reports/${reportId}/activities`, { method: 'POST', body: JSON.stringify(data) })
  }

  function deleteActivity(reportId: string, activityId: string): Promise<void> {
    return authFetch(`/api/daily-reports/${reportId}/activities/${activityId}`, { method: 'DELETE' })
  }

  function addOccurrence(reportId: string, description: string): Promise<Occurrence> {
    return authFetch(`/api/daily-reports/${reportId}/occurrences`, {
      method: 'POST',
      body: JSON.stringify({ description }),
    })
  }

  function addMaterialReceived(
    reportId: string,
    data: { materialName: string; unit: string | null; quantity: string },
  ): Promise<MaterialReceived> {
    return authFetch(`/api/daily-reports/${reportId}/materials-received`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function listMedia(reportId: string): Promise<ReportMedia[]> {
    return authFetch(`/api/daily-reports/${reportId}/media`)
  }

  function uploadMedia(
    reportId: string,
    file: File,
    type: MediaKind,
    caption: string | null,
  ): Promise<ReportMedia> {
    const params = new URLSearchParams({ type })
    if (caption) params.set('caption', caption)
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/daily-reports/${reportId}/media?${params.toString()}`, formData)
  }

  function updateMediaCaption(reportId: string, mediaId: string, caption: string | null): Promise<ReportMedia> {
    return authFetch(`/api/daily-reports/${reportId}/media/${mediaId}`, {
      method: 'PATCH',
      body: JSON.stringify({ caption }),
    })
  }

  function deleteMedia(reportId: string, mediaId: string): Promise<void> {
    return authFetch(`/api/daily-reports/${reportId}/media/${mediaId}`, { method: 'DELETE' })
  }

  function getMediaContentUrl(reportId: string, mediaId: string): Promise<Blob> {
    return authFetchBlob(`/api/daily-reports/${reportId}/media/${mediaId}/content`)
  }

  function listAttachments(reportId: string): Promise<ReportAttachment[]> {
    return authFetch(`/api/daily-reports/${reportId}/attachments`)
  }

  function uploadAttachment(reportId: string, file: File): Promise<ReportAttachment> {
    const formData = new FormData()
    formData.set('file', file)
    return authUpload(`/api/daily-reports/${reportId}/attachments`, formData)
  }

  function deleteAttachment(reportId: string, attachmentId: string): Promise<void> {
    return authFetch(`/api/daily-reports/${reportId}/attachments/${attachmentId}`, { method: 'DELETE' })
  }

  function getAttachmentContentUrl(reportId: string, attachmentId: string): Promise<Blob> {
    return authFetchBlob(`/api/daily-reports/${reportId}/attachments/${attachmentId}/content`)
  }

  function listDeliveredMaterials(reportId: string): Promise<Material[]> {
    return authFetch(`/api/daily-reports/${reportId}/delivered-materials`)
  }

  function listImportedInvoices(reportId: string): Promise<DailyReportImportedInvoice[]> {
    return authFetch(`/api/daily-reports/${reportId}/imported-invoices`)
  }

  function getPdf(reportId: string): Promise<Blob> {
    return authFetchBlob(`/api/daily-reports/${reportId}/pdf`)
  }

  return {
    listReports,
    createReport,
    getDetail,
    updateCore,
    submitReport,
    approveStep,
    rejectStep,
    deleteReport,
    addWorkforceEntry,
    deleteWorkforceEntry,
    addEquipmentUsage,
    deleteEquipmentUsage,
    addActivity,
    deleteActivity,
    addOccurrence,
    addMaterialReceived,
    listMedia,
    uploadMedia,
    updateMediaCaption,
    deleteMedia,
    getMediaContentUrl,
    listAttachments,
    uploadAttachment,
    deleteAttachment,
    getAttachmentContentUrl,
    listDeliveredMaterials,
    listImportedInvoices,
    getPdf,
  }
}
