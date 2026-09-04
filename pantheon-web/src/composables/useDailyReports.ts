import { SERVICE_BASE_URL } from '../lib/config'
import { useAuth } from './useAuth'

export type DailyReportStatus = 'DRAFT' | 'SUBMITTED'
export type ActivityStatus = 'IN_PROGRESS' | 'COMPLETED'

export interface DailyReport {
  id: string
  constructionSiteId: string
  reportDate: string
  sequenceNo: number
  status: DailyReportStatus
  weatherCondition: string | null
  weatherBlockedTasks: boolean | null
  workHoursStart: string | null
  workHoursEnd: string | null
  comments: string | null
  submittedAt: string | null
}

export interface DailyReportCoreUpdate {
  weatherCondition: string | null
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
  equipmentId: string
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
  materialId: string
  quantity: string
}

export interface DailyReportDetail {
  report: DailyReport
  workforceEntries: WorkforceEntry[]
  equipmentUsage: EquipmentUsage[]
  activities: Activity[]
  occurrences: Occurrence[]
  materialsReceived: MaterialReceived[]
}

export type MediaKind = 'PHOTO' | 'VIDEO'

export interface ReportMedia {
  id: string
  type: MediaKind
  caption: string | null
  contentType: string
}

export interface ReportAttachment {
  id: string
  originalName: string
  contentType: string
}

export interface ReportSignature {
  id: string
  membershipId: string
  function: string | null
  signedAt: string
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
    throw new Error(`Request to ${path} failed with status ${response.status}`)
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
    throw new Error(`Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

export function useDailyReports() {
  function listReports(siteId: string): Promise<DailyReport[]> {
    return authFetch(`/api/construction-sites/${siteId}/daily-reports`)
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

  function addWorkforceEntry(
    reportId: string,
    data: { membershipId: string | null; roleDescription: string | null; headcount: number },
  ): Promise<WorkforceEntry> {
    return authFetch(`/api/daily-reports/${reportId}/workforce-entries`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function addEquipmentUsage(
    reportId: string,
    data: { equipmentId: string; statusNote: string | null },
  ): Promise<EquipmentUsage> {
    return authFetch(`/api/daily-reports/${reportId}/equipment-usage`, {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  function addActivity(
    reportId: string,
    data: { description: string; progressNote: string; status: ActivityStatus },
  ): Promise<Activity> {
    return authFetch(`/api/daily-reports/${reportId}/activities`, { method: 'POST', body: JSON.stringify(data) })
  }

  function addOccurrence(reportId: string, description: string): Promise<Occurrence> {
    return authFetch(`/api/daily-reports/${reportId}/occurrences`, {
      method: 'POST',
      body: JSON.stringify({ description }),
    })
  }

  function addMaterialReceived(
    reportId: string,
    data: { materialId: string; quantity: string },
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

  function getAttachmentContentUrl(reportId: string, attachmentId: string): Promise<Blob> {
    return authFetchBlob(`/api/daily-reports/${reportId}/attachments/${attachmentId}/content`)
  }

  function listSignatures(reportId: string): Promise<ReportSignature[]> {
    return authFetch(`/api/daily-reports/${reportId}/signatures`)
  }

  function signReport(reportId: string): Promise<ReportSignature> {
    return authFetch(`/api/daily-reports/${reportId}/signatures`, { method: 'POST' })
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
    addWorkforceEntry,
    addEquipmentUsage,
    addActivity,
    addOccurrence,
    addMaterialReceived,
    listMedia,
    uploadMedia,
    getMediaContentUrl,
    listAttachments,
    uploadAttachment,
    getAttachmentContentUrl,
    listSignatures,
    signReport,
    getPdf,
  }
}
