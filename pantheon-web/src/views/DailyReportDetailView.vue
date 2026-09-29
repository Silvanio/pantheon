<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  useDailyReports,
  type ActivityStatus,
  type DailyReportApproval,
  type DailyReportDetail,
  type DailyReportImportedInvoice,
  type MediaKind,
  type ReportAttachment,
  type ReportMedia,
} from '../composables/useDailyReports'
import { useEquipment, type Equipment, type EquipmentStatus } from '../composables/useEquipment'
import { useSiteMembers, type ConstructionFunction, type SiteMember } from '../composables/useSiteMembers'
import { useSitePermissions, type AccessLevel } from '../composables/useSitePermissions'
import { useConstructionSites } from '../composables/useConstructionSites'
import { useMaterialDeliveries, type Material } from '../composables/useMaterialDeliveries'
import { usePurchaseRequests } from '../composables/usePurchaseRequests'
import AppHeader from '../components/AppHeader.vue'
import AppSidebar from '../components/AppSidebar.vue'
import SiteBreadcrumb from '../components/SiteBreadcrumb.vue'
import StatusBadge from '../components/StatusBadge.vue'
import TimeClockPicker from '../components/TimeClockPicker.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const {
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
  getMediaThumbnailUrl,
  getMediaContentUrl,
  listAttachments,
  uploadAttachment,
  deleteAttachment,
  getAttachmentContentUrl,
  listDeliveredMaterials,
  listImportedInvoices,
  getPdf,
} = useDailyReports()
const { listEquipment } = useEquipment()
const { listMembers, getMyFunction } = useSiteMembers()
const { getMyPermissions } = useSitePermissions()
const { getSite } = useConstructionSites()
const { markChecked } = useMaterialDeliveries()
const { getInvoiceContentBlob } = usePurchaseRequests()

const reportId = route.params.id as string

// ---------------------------------------------------------------------------
// Draft row types for the three "add/delete only" sub-resources (workforce,
// equipment usage, activities). Each row tracks its server `id` (null until
// saved), the fields as loaded from the server (`original`, null for a
// brand-new row) and a `removed` flag. See the global-save section below for
// how these are diffed and committed in one shot — design.md Decision 1.
// ---------------------------------------------------------------------------
interface WorkforceDraftRow {
  key: string
  id: string | null
  membershipId: string | null
  roleDescription: string
  headcount: number
  original: { roleDescription: string; headcount: number } | null
  removed: boolean
}

interface EquipmentDraftRow {
  key: string
  id: string | null
  equipmentId: string | null
  customName: string
  statusNote: string
  original: { equipmentId: string | null; customName: string; statusNote: string } | null
  removed: boolean
}

interface ActivityDraftRow {
  key: string
  id: string | null
  description: string
  status: ActivityStatus
  original: { description: string; status: ActivityStatus } | null
  removed: boolean
}

type AnexoRow = { kind: 'attachment'; data: ReportAttachment } | { kind: 'invoice'; data: DailyReportImportedInvoice }

const detail = ref<DailyReportDetail | null>(null)
const equipmentCatalog = ref<Equipment[]>([])
const siteMembers = ref<SiteMember[]>([])
const siteName = ref<string | null>(null)
const loading = ref(false)
const loadError = ref('')
const myAccessLevel = ref<AccessLevel | null>(null)
const myFunction = ref<ConstructionFunction | null>(null)

const isDraft = computed(() => detail.value?.report.status === 'DRAFT')

// Whether this viewer's access level allows approving at all (MANAGE or VIEW_AND_APPROVE) —
// mirrors SitePermissionService.canApprove on the backend, same as PurchaseRequestDetailView.
const canApprove = computed(() => myAccessLevel.value === 'MANAGE' || myAccessLevel.value === 'VIEW_AND_APPROVE')

// Only a MANAGE member may submit a draft for approval (see daily-construction-report's "Daily
// report submission" requirement) — VIEW_AND_APPROVE members can see drafts (to act on pending
// approvals across the site) but don't author/submit them.
const canSubmit = computed(() => isDraft.value && myAccessLevel.value === 'MANAGE')

const currentCycle = computed(() => {
  if (!detail.value || detail.value.approvals.length === 0) return 0
  return Math.max(...detail.value.approvals.map((a) => a.cycleNumber))
})

const currentPendingApproval = computed(() => {
  if (!detail.value) return null
  return (
    detail.value.approvals
      .filter((a) => a.cycleNumber === currentCycle.value && a.status === 'PENDING')
      .sort((a, b) => a.stepOrder - b.stepOrder)[0] ?? null
  )
})

const approvalsByCycle = computed(() => {
  if (!detail.value) return []
  const cycles = new Map<number, typeof detail.value.approvals>()
  for (const approval of detail.value.approvals) {
    if (!cycles.has(approval.cycleNumber)) cycles.set(approval.cycleNumber, [])
    cycles.get(approval.cycleNumber)!.push(approval)
  }
  return Array.from(cycles.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([cycleNumber, approvals]) => ({
      cycleNumber,
      approvals: approvals.slice().sort((a, b) => a.stepOrder - b.stepOrder),
    }))
})

const currentCycleApprovals = computed(() => {
  if (!detail.value) return []
  return detail.value.approvals
    .filter((a) => a.cycleNumber === currentCycle.value)
    .slice()
    .sort((a, b) => a.stepOrder - b.stepOrder)
})

const priorCycles = computed(() => approvalsByCycle.value.filter((c) => c.cycleNumber !== currentCycle.value))

// Mirrors the backend's requireStepAuthority: a member holding a SiteMembership on this site
// (even one who is also company staff) may act only when their function matches the current
// pending step; a company-staff user with NO SiteMembership here at all keeps the admin bypass.
const canActOnApproval = computed(() => {
  if (!currentPendingApproval.value) return false
  if (myFunction.value !== null) {
    return myFunction.value === currentPendingApproval.value.approverFunction && canApprove.value
  }
  return myAccessLevel.value === 'MANAGE'
})

function approvalStepBadgeClass(approval: DailyReportApproval): string {
  if (approval.status === 'APPROVED') return 'bg-emerald-500 text-white'
  if (approval.status === 'REJECTED') return 'bg-safety-500 text-white'
  return 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200'
}

// ---------------------------------------------------------------------------
// Core fields (Clima & Expediente) — staged locally, committed on global save.
// ---------------------------------------------------------------------------
const WEATHER_OPTIONS = ['SUNNY', 'PARTLY_CLOUDY', 'CLOUDY', 'RAINY', 'STORM'] as const

interface CoreSnapshot {
  weatherConditionMorning: string | null
  weatherConditionAfternoon: string | null
  weatherBlockedTasks: boolean | null
  workHoursStart: string | null
  workHoursEnd: string | null
  comments: string | null
}

const weatherConditionMorning = ref('')
const weatherConditionAfternoon = ref('')
const weatherBlockedTasks = ref(false)
const workHoursStart = ref('')
const workHoursEnd = ref('')
const comments = ref('')
const coreOriginal = ref<CoreSnapshot | null>(null)

const coreDirty = computed(() => {
  if (!coreOriginal.value) return false
  const o = coreOriginal.value
  return (
    (weatherConditionMorning.value || null) !== o.weatherConditionMorning ||
    (weatherConditionAfternoon.value || null) !== o.weatherConditionAfternoon ||
    weatherBlockedTasks.value !== (o.weatherBlockedTasks ?? false) ||
    (workHoursStart.value || null) !== o.workHoursStart ||
    (workHoursEnd.value || null) !== o.workHoursEnd ||
    (comments.value || null) !== o.comments
  )
})

function weatherIconKind(option: string): 'sun' | 'cloud' | 'rain' {
  if (option === 'SUNNY' || option === 'PARTLY_CLOUDY') return 'sun'
  if (option === 'CLOUDY') return 'cloud'
  return 'rain'
}

// ---------------------------------------------------------------------------
// Workforce — team roster picker + free-text "outra mão de obra" rows.
// ---------------------------------------------------------------------------
const workforceRows = ref<WorkforceDraftRow[]>([])

const activeMembers = computed(() => siteMembers.value.filter((m) => !m.invited))
const customWorkforceRows = computed(() => workforceRows.value.filter((r) => r.membershipId === null && !r.removed))

function isWorkforceRowDirty(row: WorkforceDraftRow): boolean {
  if (row.removed) return row.id !== null
  if (row.id === null) return true
  return row.original !== null && (row.roleDescription !== row.original.roleDescription || row.headcount !== row.original.headcount)
}

function workforceRowFor(membershipId: string): WorkforceDraftRow | undefined {
  return workforceRows.value.find((r) => r.membershipId === membershipId && !r.removed)
}

function tempKey(): string {
  return `tmp-${crypto.randomUUID()}`
}

function addTeamMember(member: SiteMember) {
  if (workforceRowFor(member.membershipId)) return
  const removedRow = workforceRows.value.find((r) => r.membershipId === member.membershipId && r.removed)
  if (removedRow) {
    removedRow.removed = false
    removedRow.headcount = Math.max(1, removedRow.headcount)
    return
  }
  workforceRows.value.push({
    key: tempKey(),
    id: null,
    membershipId: member.membershipId,
    roleDescription: '',
    headcount: 1,
    original: null,
    removed: false,
  })
}

function incrementWorkforce(row: WorkforceDraftRow) {
  row.headcount += 1
}

function decrementWorkforce(row: WorkforceDraftRow) {
  if (row.headcount <= 1) {
    removeWorkforceRow(row)
  } else {
    row.headcount -= 1
  }
}

function addCustomWorkforceRow() {
  workforceRows.value.push({
    key: tempKey(),
    id: null,
    membershipId: null,
    roleDescription: '',
    headcount: 1,
    original: null,
    removed: false,
  })
}

function removeWorkforceRow(row: WorkforceDraftRow) {
  if (row.id === null) {
    workforceRows.value = workforceRows.value.filter((r) => r.key !== row.key)
  } else {
    row.removed = true
  }
}

// ---------------------------------------------------------------------------
// Equipment usage — registry search/filter picker + free-text "outro equipamento" rows.
// ---------------------------------------------------------------------------
const equipmentRows = ref<EquipmentDraftRow[]>([])
const equipmentSearchTerm = ref('')
const equipmentSearching = ref(false)
let equipmentSearchTimer: ReturnType<typeof setTimeout> | null = null

const customEquipmentRows = computed(() => equipmentRows.value.filter((r) => r.equipmentId === null && !r.removed))

function isEquipmentRowDirty(row: EquipmentDraftRow): boolean {
  if (row.removed) return row.id !== null
  if (row.id === null) return true
  return (
    row.original !== null &&
    (row.equipmentId !== row.original.equipmentId || row.customName !== row.original.customName || row.statusNote !== row.original.statusNote)
  )
}

function equipmentRowFor(equipmentId: string): EquipmentDraftRow | undefined {
  return equipmentRows.value.find((r) => r.equipmentId === equipmentId && !r.removed)
}

function equipmentStatusBadgeClass(status: EquipmentStatus): string {
  switch (status) {
    case 'AVAILABLE':
      return 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300'
    case 'IN_USE':
      return 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300'
    case 'MAINTENANCE':
      return 'bg-safety-100 text-safety-700 dark:bg-safety-900/50 dark:text-safety-300'
    default:
      return 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200'
  }
}

async function searchEquipmentCatalog(siteId: string, term: string) {
  equipmentSearching.value = true
  try {
    const trimmed = term.trim()
    if (!trimmed) {
      equipmentCatalog.value = (await listEquipment(siteId, { size: 50 })).content
      return
    }
    const byName = await listEquipment(siteId, { name: trimmed, size: 50 })
    if (byName.content.length > 0) {
      equipmentCatalog.value = byName.content
      return
    }
    equipmentCatalog.value = (await listEquipment(siteId, { type: trimmed, size: 50 })).content
  } finally {
    equipmentSearching.value = false
  }
}

watch(equipmentSearchTerm, (term) => {
  if (equipmentSearchTimer) clearTimeout(equipmentSearchTimer)
  equipmentSearchTimer = setTimeout(() => {
    if (detail.value) searchEquipmentCatalog(detail.value.report.constructionSiteId, term)
  }, 300)
})

function toggleRegisteredEquipment(eq: Equipment) {
  const activeRow = equipmentRowFor(eq.id)
  if (activeRow) {
    removeEquipmentRow(activeRow)
    return
  }
  const removedRow = equipmentRows.value.find((r) => r.equipmentId === eq.id && r.removed)
  if (removedRow) {
    removedRow.removed = false
    return
  }
  equipmentRows.value.push({
    key: tempKey(),
    id: null,
    equipmentId: eq.id,
    customName: '',
    statusNote: '',
    original: null,
    removed: false,
  })
}

function addCustomEquipmentRow() {
  equipmentRows.value.push({
    key: tempKey(),
    id: null,
    equipmentId: null,
    customName: '',
    statusNote: '',
    original: null,
    removed: false,
  })
}

function removeEquipmentRow(row: EquipmentDraftRow) {
  if (row.id === null) {
    equipmentRows.value = equipmentRows.value.filter((r) => r.key !== row.key)
  } else {
    row.removed = true
  }
}

// ---------------------------------------------------------------------------
// Activities — required description + status pill, staged like the sections above.
// ---------------------------------------------------------------------------
const activityRows = ref<ActivityDraftRow[]>([])
const visibleActivityRows = computed(() => activityRows.value.filter((r) => !r.removed))

function isActivityRowDirty(row: ActivityDraftRow): boolean {
  if (row.removed) return row.id !== null
  if (row.id === null) return true
  return row.original !== null && (row.description !== row.original.description || row.status !== row.original.status)
}

function addActivityRow() {
  activityRows.value.push({ key: tempKey(), id: null, description: '', status: 'IN_PROGRESS', original: null, removed: false })
}

function removeActivityRow(row: ActivityDraftRow) {
  if (row.id === null) {
    activityRows.value = activityRows.value.filter((r) => r.key !== row.key)
  } else {
    row.removed = true
  }
}

// The mockup's activity card has no separate "progress note" field, only description + status —
// but ActivityRequest.progressNote is @NotBlank on the backend. Since there's no visible field for
// it in the approved design, it's auto-derived from the status label rather than adding a UI
// element the mockup doesn't have.
function activityProgressNoteFor(status: ActivityStatus): string {
  return status === 'COMPLETED' ? t('dailyReports.activities.statusOptions.COMPLETED') : t('dailyReports.activities.statusOptions.IN_PROGRESS')
}

// ---------------------------------------------------------------------------
// Occurrences — out of scope for this redesign (non-goal); kept functionally as-is.
// ---------------------------------------------------------------------------
const occurrenceError = ref('')
const occurrenceDescription = ref('')

async function onAddOccurrence() {
  occurrenceError.value = ''
  try {
    const occurrence = await addOccurrence(reportId, occurrenceDescription.value)
    detail.value?.occurrences.push(occurrence)
    occurrenceDescription.value = ''
  } catch {
    occurrenceError.value = t('dailyReports.occurrences.error')
  }
}

// ---------------------------------------------------------------------------
// Materials received — manual free-text entry (kept exactly as-is) + read-only
// materials delivered on this report's date (new).
// ---------------------------------------------------------------------------
const materialReceivedError = ref('')
const materialReceivedName = ref('')
const materialReceivedUnit = ref('')
const materialQuantity = ref('')

async function onAddMaterialReceived() {
  materialReceivedError.value = ''
  try {
    const received = await addMaterialReceived(reportId, {
      materialName: materialReceivedName.value,
      unit: materialReceivedUnit.value || null,
      quantity: materialQuantity.value,
    })
    detail.value?.materialsReceived.push(received)
    materialReceivedName.value = ''
    materialReceivedUnit.value = ''
    materialQuantity.value = ''
  } catch {
    materialReceivedError.value = t('dailyReports.materialsReceived.error')
  }
}

const deliveredMaterials = ref<Material[]>([])
const deliveredMaterialsError = ref('')
const markingCheckedId = ref<string | null>(null)

async function onMarkDeliveredChecked(material: Material) {
  deliveredMaterialsError.value = ''
  markingCheckedId.value = material.id
  try {
    const updated = await markChecked(material.id, [])
    const idx = deliveredMaterials.value.findIndex((m) => m.id === material.id)
    if (idx !== -1) deliveredMaterials.value[idx] = updated
  } catch {
    deliveredMaterialsError.value = t('dailyReports.deliveredMaterials.error')
  } finally {
    markingCheckedId.value = null
  }
}

// ---------------------------------------------------------------------------
// Media — drag-and-drop + click-to-browse upload (immediate), staged captions.
// ---------------------------------------------------------------------------
const media = ref<ReportMedia[]>([])
const mediaError = ref('')
const mediaCaptionDrafts = ref<Record<string, string>>({})
const mediaPreviewUrls = ref<Record<string, string>>({})
const isDraggingMedia = ref(false)
const mediaLightbox = ref<ReportMedia | null>(null)
const mediaLightboxUrl = ref('')
const mediaLightboxLoading = ref(false)
const mediaLightboxError = ref('')

async function openMediaLightbox(item: ReportMedia) {
  if (item.type !== 'PHOTO') return
  mediaLightbox.value = item
  mediaLightboxLoading.value = true
  mediaLightboxError.value = ''
  try {
    const blob = await getMediaContentUrl(reportId, item.id)
    mediaLightboxUrl.value = URL.createObjectURL(blob)
  } catch {
    mediaLightboxError.value = t('dailyReports.media.previewError')
  } finally {
    mediaLightboxLoading.value = false
  }
}

function closeMediaLightbox() {
  if (mediaLightboxUrl.value) URL.revokeObjectURL(mediaLightboxUrl.value)
  mediaLightbox.value = null
  mediaLightboxUrl.value = ''
  mediaLightboxError.value = ''
}

function isCaptionDirty(item: ReportMedia): boolean {
  return (mediaCaptionDrafts.value[item.id] ?? '') !== (item.caption ?? '')
}

async function uploadMediaFiles(files: File[]) {
  mediaError.value = ''
  for (const file of files) {
    const kind: MediaKind = file.type.startsWith('video/') ? 'VIDEO' : 'PHOTO'
    try {
      const uploaded = await uploadMedia(reportId, file, kind, null)
      media.value.push(uploaded)
      mediaCaptionDrafts.value[uploaded.id] = ''
      if (uploaded.type === 'PHOTO') {
        const blob = await getMediaThumbnailUrl(reportId, uploaded.id)
        mediaPreviewUrls.value[uploaded.id] = URL.createObjectURL(blob)
      }
    } catch {
      mediaError.value = t('dailyReports.media.error')
    }
  }
}

function onMediaDragOver() {
  isDraggingMedia.value = true
}

function onMediaDragLeave() {
  isDraggingMedia.value = false
}

async function onMediaDrop(event: DragEvent) {
  isDraggingMedia.value = false
  const files = event.dataTransfer?.files
  if (files && files.length > 0) await uploadMediaFiles(Array.from(files))
}

async function onMediaInputChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (files && files.length > 0) await uploadMediaFiles(Array.from(files))
  input.value = ''
}

async function onRemoveMedia(item: ReportMedia) {
  mediaError.value = ''
  try {
    await deleteMedia(reportId, item.id)
    media.value = media.value.filter((m) => m.id !== item.id)
    delete mediaCaptionDrafts.value[item.id]
    const url = mediaPreviewUrls.value[item.id]
    if (url) {
      URL.revokeObjectURL(url)
      delete mediaPreviewUrls.value[item.id]
    }
  } catch {
    mediaError.value = t('dailyReports.media.removeError')
  }
}

// ---------------------------------------------------------------------------
// Attachments — real DailyReportAttachment rows merged with same-day imported
// Pedido de Compra invoices (read-only, visually tagged).
// ---------------------------------------------------------------------------
const attachments = ref<ReportAttachment[]>([])
const importedInvoices = ref<DailyReportImportedInvoice[]>([])
const attachmentError = ref('')

const anexoRows = computed<AnexoRow[]>(() => {
  const rows: AnexoRow[] = [
    ...attachments.value.map((data) => ({ kind: 'attachment' as const, data })),
    ...importedInvoices.value.map((data) => ({ kind: 'invoice' as const, data })),
  ]
  return rows.sort((a, b) => (a.data.uploadedAt < b.data.uploadedAt ? 1 : -1))
})

async function onAttachmentFilesSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  if (files && files.length > 0) {
    attachmentError.value = ''
    for (const file of Array.from(files)) {
      try {
        const uploaded = await uploadAttachment(reportId, file)
        attachments.value.push(uploaded)
      } catch {
        attachmentError.value = t('dailyReports.attachments.error')
      }
    }
  }
  input.value = ''
}

async function onDownloadAttachment(attachment: ReportAttachment) {
  attachmentError.value = ''
  try {
    const blob = await getAttachmentContentUrl(reportId, attachment.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.target = '_blank'
    link.rel = 'noopener'
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    attachmentError.value = t('dailyReports.attachments.error')
  }
}

async function onDownloadInvoice(invoice: DailyReportImportedInvoice) {
  attachmentError.value = ''
  try {
    const { blob, filename } = await getInvoiceContentBlob(invoice.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename ?? invoice.originalName
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    attachmentError.value = t('dailyReports.attachments.error')
  }
}

async function onRemoveAttachment(attachment: ReportAttachment) {
  attachmentError.value = ''
  try {
    await deleteAttachment(reportId, attachment.id)
    attachments.value = attachments.value.filter((a) => a.id !== attachment.id)
  } catch {
    attachmentError.value = t('dailyReports.attachments.removeError')
  }
}

function onDownloadAnexo(row: AnexoRow) {
  if (row.kind === 'attachment') {
    onDownloadAttachment(row.data)
  } else {
    onDownloadInvoice(row.data)
  }
}

function onRemoveAnexo(row: AnexoRow) {
  if (row.kind === 'attachment') {
    onRemoveAttachment(row.data)
  }
}

function anexoInvoiceLabel(row: AnexoRow): string {
  if (row.kind !== 'invoice') return ''
  return t('dailyReports.attachments.importedFrom', { name: row.data.purchaseRequestName ?? '—' })
}

// ---------------------------------------------------------------------------
// Approval actions (unchanged behavior, restyled — see template).
// ---------------------------------------------------------------------------
const approvalActionError = ref('')
const rejectReason = ref('')
const showRejectForm = ref(false)

async function onApproveStep() {
  approvalActionError.value = ''
  try {
    await approveStep(reportId)
    await load()
  } catch {
    approvalActionError.value = t('dailyReports.approvalError')
  }
}

async function onRejectStep() {
  approvalActionError.value = ''
  try {
    await rejectStep(reportId, rejectReason.value)
    showRejectForm.value = false
    rejectReason.value = ''
    await load()
  } catch {
    approvalActionError.value = t('dailyReports.approvalError')
  }
}

// ---------------------------------------------------------------------------
// Submit / delete / PDF (unchanged behavior).
// ---------------------------------------------------------------------------
const submitError = ref('')
const submitting = ref(false)
const confirmingDelete = ref(false)
const deleting = ref(false)
const deleteError = ref('')
const pdfError = ref('')
const downloadingPdf = ref(false)

async function onSubmitReport() {
  submitError.value = ''
  submitting.value = true
  try {
    await submitReport(reportId)
    await load()
  } catch {
    submitError.value = t('dailyReports.detail.submitError')
  } finally {
    submitting.value = false
  }
}

async function onDelete() {
  deleteError.value = ''
  deleting.value = true
  try {
    const siteId = detail.value?.report.constructionSiteId
    await deleteReport(reportId)
    router.push({ path: siteId ? `/sites/${siteId}` : '/', query: siteId ? { tab: 'dailyReport' } : undefined })
  } catch {
    deleteError.value = t('dailyReports.detail.deleteError')
    deleting.value = false
    confirmingDelete.value = false
  }
}

async function onDownloadPdf() {
  pdfError.value = ''
  downloadingPdf.value = true
  try {
    const blob = await getPdf(reportId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = detail.value
      ? `diario-obra-${detail.value.report.sequenceNo}-${detail.value.report.reportDate}.pdf`
      : `diario-obra-${reportId}.pdf`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    pdfError.value = t('dailyReports.pdf.error')
  } finally {
    downloadingPdf.value = false
  }
}

// ---------------------------------------------------------------------------
// Global save — design.md Decision 1: client-side orchestration. Every edit made
// above (core fields, workforce/equipment/activity add-remove, media captions)
// is staged in local state; this fires every pending call together.
// ---------------------------------------------------------------------------
const saving = ref(false)
const saveError = ref('')

const isDirty = computed(
  () =>
    coreDirty.value ||
    workforceRows.value.some(isWorkforceRowDirty) ||
    equipmentRows.value.some(isEquipmentRowDirty) ||
    activityRows.value.some(isActivityRowDirty) ||
    media.value.some(isCaptionDirty),
)

function validateBeforeSave(): string | null {
  if (coreDirty.value) {
    if (!weatherConditionMorning.value || !weatherConditionAfternoon.value || !workHoursStart.value || !workHoursEnd.value) {
      return t('dailyReports.core.requiredError')
    }
  }
  for (const row of workforceRows.value) {
    if (row.removed || row.membershipId !== null || !isWorkforceRowDirty(row)) continue
    if (!row.roleDescription.trim()) return t('dailyReports.workforce.customRequiredError')
  }
  for (const row of equipmentRows.value) {
    if (row.removed || row.equipmentId !== null || !isEquipmentRowDirty(row)) continue
    if (!row.customName.trim()) return t('dailyReports.equipmentUsage.customRequiredError')
  }
  for (const row of activityRows.value) {
    if (row.removed || !isActivityRowDirty(row)) continue
    if (!row.description.trim()) return t('dailyReports.activities.descriptionRequiredError')
  }
  return null
}

async function onGlobalSave() {
  if (!detail.value) return
  const validationError = validateBeforeSave()
  if (validationError) {
    saveError.value = validationError
    return
  }
  saveError.value = ''
  saving.value = true
  try {
    const tasks: Array<() => Promise<unknown>> = []

    if (coreDirty.value) {
      tasks.push(() =>
        updateCore(reportId, {
          weatherConditionMorning: weatherConditionMorning.value || null,
          weatherConditionAfternoon: weatherConditionAfternoon.value || null,
          weatherBlockedTasks: weatherBlockedTasks.value,
          workHoursStart: workHoursStart.value || null,
          workHoursEnd: workHoursEnd.value || null,
          comments: comments.value || null,
        }),
      )
    }

    for (const row of workforceRows.value) {
      if (!isWorkforceRowDirty(row)) continue
      if (row.removed) {
        const entryId = row.id as string
        tasks.push(() => deleteWorkforceEntry(reportId, entryId))
      } else if (row.id === null) {
        tasks.push(() =>
          addWorkforceEntry(reportId, {
            membershipId: row.membershipId,
            roleDescription: row.membershipId ? null : row.roleDescription,
            headcount: row.headcount,
          }),
        )
      } else {
        // Add the replacement before deleting the old row — if the add fails, the original
        // entry is left intact instead of being lost (a delete-then-add order would silently
        // drop this row on any add failure, since the old one is already gone by then).
        const entryId = row.id
        tasks.push(async () => {
          await addWorkforceEntry(reportId, {
            membershipId: row.membershipId,
            roleDescription: row.membershipId ? null : row.roleDescription,
            headcount: row.headcount,
          })
          await deleteWorkforceEntry(reportId, entryId)
        })
      }
    }

    for (const row of equipmentRows.value) {
      if (!isEquipmentRowDirty(row)) continue
      if (row.removed) {
        const usageId = row.id as string
        tasks.push(() => deleteEquipmentUsage(reportId, usageId))
      } else if (row.id === null) {
        tasks.push(() =>
          addEquipmentUsage(reportId, {
            equipmentId: row.equipmentId,
            customName: row.equipmentId ? null : row.customName.trim(),
            statusNote: row.statusNote.trim() || null,
          }),
        )
      } else {
        // Same add-before-delete safety as workforce, above.
        const usageId = row.id
        tasks.push(async () => {
          await addEquipmentUsage(reportId, {
            equipmentId: row.equipmentId,
            customName: row.equipmentId ? null : row.customName.trim(),
            statusNote: row.statusNote.trim() || null,
          })
          await deleteEquipmentUsage(reportId, usageId)
        })
      }
    }

    for (const row of activityRows.value) {
      if (!isActivityRowDirty(row)) continue
      if (row.removed) {
        const activityId = row.id as string
        tasks.push(() => deleteActivity(reportId, activityId))
      } else if (row.id === null) {
        tasks.push(() =>
          addActivity(reportId, {
            description: row.description.trim(),
            progressNote: activityProgressNoteFor(row.status),
            status: row.status,
          }),
        )
      } else {
        // Same add-before-delete safety as workforce/equipment, above.
        const activityId = row.id
        tasks.push(async () => {
          await addActivity(reportId, {
            description: row.description.trim(),
            progressNote: activityProgressNoteFor(row.status),
            status: row.status,
          })
          await deleteActivity(reportId, activityId)
        })
      }
    }

    for (const item of media.value) {
      if (!isCaptionDirty(item)) continue
      const caption = (mediaCaptionDrafts.value[item.id] ?? '').trim()
      tasks.push(() => updateMediaCaption(reportId, item.id, caption || null))
    }

    const results = await Promise.allSettled(tasks.map((task) => task()))
    const hadFailure = results.some((r) => r.status === 'rejected')

    await load()

    if (hadFailure) {
      saveError.value = t('dailyReports.detail.saveError')
    }
  } catch {
    saveError.value = t('dailyReports.detail.saveError')
  } finally {
    saving.value = false
  }
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------
function initials(name: string | null): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

function formatDateBR(dateStr: string): string {
  const [y, m, d] = dateStr.split('-')
  return `${d}/${m}/${y}`
}

function formatFullDate(dateStr: string): string {
  const date = new Date(`${dateStr}T00:00:00`)
  const weekday = date.toLocaleDateString('pt-BR', { weekday: 'long' })
  const rest = date.toLocaleDateString('pt-BR', { day: 'numeric', month: 'long', year: 'numeric' })
  return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)}, ${rest}`
}

function formatDateTime(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  const datePart = date.toLocaleDateString('pt-BR')
  const timePart = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
  return `${datePart} às ${timePart}`
}

// ---------------------------------------------------------------------------
// Load
// ---------------------------------------------------------------------------
async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getDetail(reportId)
    const report = detail.value.report

    weatherConditionMorning.value = report.weatherConditionMorning ?? ''
    weatherConditionAfternoon.value = report.weatherConditionAfternoon ?? ''
    weatherBlockedTasks.value = report.weatherBlockedTasks ?? false
    workHoursStart.value = report.workHoursStart ?? ''
    workHoursEnd.value = report.workHoursEnd ?? ''
    comments.value = report.comments ?? ''
    coreOriginal.value = {
      weatherConditionMorning: report.weatherConditionMorning,
      weatherConditionAfternoon: report.weatherConditionAfternoon,
      weatherBlockedTasks: report.weatherBlockedTasks,
      workHoursStart: report.workHoursStart,
      workHoursEnd: report.workHoursEnd,
      comments: report.comments,
    }

    workforceRows.value = detail.value.workforceEntries.map((entry) => ({
      key: entry.id,
      id: entry.id,
      membershipId: entry.membershipId,
      roleDescription: entry.roleDescription ?? '',
      headcount: entry.headcount,
      original: { roleDescription: entry.roleDescription ?? '', headcount: entry.headcount },
      removed: false,
    }))

    equipmentRows.value = detail.value.equipmentUsage.map((usage) => ({
      key: usage.id,
      id: usage.id,
      equipmentId: usage.equipmentId,
      customName: usage.customName ?? '',
      statusNote: usage.statusNote ?? '',
      original: { equipmentId: usage.equipmentId, customName: usage.customName ?? '', statusNote: usage.statusNote ?? '' },
      removed: false,
    }))

    activityRows.value = detail.value.activities.map((activity) => ({
      key: activity.id,
      id: activity.id,
      description: activity.description,
      status: activity.status,
      original: { description: activity.description, status: activity.status },
      removed: false,
    }))

    const siteId = report.constructionSiteId
    equipmentSearchTerm.value = ''

    await Promise.all([
      searchEquipmentCatalog(siteId, ''),
      listMembers(siteId).then((members) => {
        siteMembers.value = members
      }),
      Promise.all([getMyPermissions(siteId), getMyFunction(siteId)]).then(([permissions, fn]) => {
        myAccessLevel.value = permissions.DAILY_REPORT
        myFunction.value = fn
      }),
      getSite(siteId)
        .then((s) => {
          siteName.value = s.name
        })
        .catch(() => {}),
    ])

    const [mediaList, attachmentList, deliveredList, invoiceList] = await Promise.all([
      listMedia(reportId),
      listAttachments(reportId),
      listDeliveredMaterials(reportId).catch(() => []),
      listImportedInvoices(reportId).catch(() => []),
    ])
    media.value = mediaList
    attachments.value = attachmentList
    deliveredMaterials.value = deliveredList
    importedInvoices.value = invoiceList

    const captionDrafts: Record<string, string> = {}
    for (const item of mediaList) captionDrafts[item.id] = item.caption ?? ''
    mediaCaptionDrafts.value = captionDrafts

    for (const url of Object.values(mediaPreviewUrls.value)) URL.revokeObjectURL(url)
    mediaPreviewUrls.value = {}
    await Promise.all(
      mediaList
        .filter((m) => m.type === 'PHOTO')
        .map(async (m) => {
          const blob = await getMediaThumbnailUrl(reportId, m.id)
          mediaPreviewUrls.value[m.id] = URL.createObjectURL(blob)
        }),
    )
  } catch {
    loadError.value = t('dailyReports.detail.loadError')
  } finally {
    loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  for (const url of Object.values(mediaPreviewUrls.value)) URL.revokeObjectURL(url)
  if (mediaLightboxUrl.value) URL.revokeObjectURL(mediaLightboxUrl.value)
})
</script>

<template>
  <div class="flex min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppSidebar />
    <div class="min-w-0 flex-1">
      <AppHeader>
        <template v-if="detail" #left>
          <SiteBreadcrumb
            :site-id="detail.report.constructionSiteId"
            :site-name="siteName"
            tab="dailyReport"
            :label="t('dailyReports.history.title')"
          />
        </template>
      </AppHeader>

      <div v-if="detail" class="mx-auto flex w-full max-w-[1600px] flex-col gap-5 px-4 py-6 sm:px-6 lg:px-6 xl:px-8">
        <!-- Full-width, above the aside+main split — not confined to main's narrower column,
             so it lines up with the aside above it instead of stopping short. -->
        <p v-if="!isDraft" class="rounded-md bg-blueprint-50 px-4 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
          {{ t('dailyReports.detail.submittedNotice') }}
        </p>

        <div class="flex flex-col gap-5 lg:flex-row lg:gap-6">
          <!-- Report name/status/date + global actions — a side panel on wide screens (test:
               frees the main column's full width for content instead of a centered sticky bar),
               stacked above the content on narrow ones where a fixed side column doesn't fit.
               lg:top-20 = AppHeader's h-16 plus a 16px gap, same spacing rule as the bar it replaces. -->
          <aside class="lg:order-2 lg:w-[300px] lg:shrink-0">
          <div class="card card-pad flex flex-col gap-4 lg:sticky lg:top-20">
            <div class="flex flex-col gap-2">
              <div class="flex flex-wrap items-center gap-3">
                <h1 class="text-2xl font-extrabold tracking-tight text-steel-800 dark:text-steel-50">
                  {{ t('dailyReports.history.reportLabel') }} #{{ detail.report.sequenceNo }}
                </h1>
                <StatusBadge kind="dailyReport" :status="detail.report.status" />
              </div>
              <span class="text-sm text-steel-500 dark:text-steel-400">{{ formatFullDate(detail.report.reportDate) }}</span>
            </div>

            <div class="h-px bg-steel-100 dark:bg-steel-800 lg:block"></div>

            <div class="flex flex-wrap items-center gap-2.5 lg:flex-col lg:items-stretch lg:gap-2">
              <div
                class="flex items-center gap-1.5 text-xs font-semibold lg:mb-1"
                :class="saving ? 'text-steel-500 dark:text-steel-400' : isDirty ? 'text-amber-700 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'"
              >
                <span
                  class="h-1.5 w-1.5 rounded-full"
                  :class="saving ? 'bg-steel-400' : isDirty ? 'bg-amber-500' : 'bg-emerald-500'"
                ></span>
                {{ saving ? t('dailyReports.detail.saving') : isDirty ? t('dailyReports.detail.unsaved') : t('dailyReports.detail.allSaved') }}
              </div>
              <button v-if="isDraft" type="button" :disabled="saving || !isDirty" class="btn-primary lg:w-full" @click="onGlobalSave">
                {{ saving ? t('dailyReports.detail.saving') : t('dailyReports.detail.saveButton') }}
              </button>
              <button v-if="canSubmit" type="button" :disabled="submitting" class="btn-secondary lg:w-full" @click="onSubmitReport">
                {{ t('dailyReports.detail.submitButton') }}
              </button>
              <button type="button" :disabled="downloadingPdf" class="btn-secondary lg:w-full" @click="onDownloadPdf">
                {{ t('dailyReports.pdf.downloadButton') }}
              </button>
              <div v-if="isDraft" class="relative lg:mt-2">
                <button type="button" :disabled="deleting" class="btn-danger lg:w-full" @click="confirmingDelete = !confirmingDelete">
                  {{ t('dailyReports.detail.deleteButton') }}
                </button>
                <div v-if="confirmingDelete" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-3 shadow-lg" @click.stop>
                  <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('dailyReports.detail.deleteConfirm') }}</p>
                  <div class="flex justify-end gap-2">
                    <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDelete = false">{{ t('dailyReports.detail.deleteCancel') }}</button>
                    <button type="button" :disabled="deleting" class="btn-danger py-1 text-xs" @click="onDelete">{{ t('dailyReports.detail.deleteButton') }}</button>
                  </div>
                </div>
              </div>
            </div>

            <template v-if="detail.approvals.length > 0">
              <div class="h-px bg-steel-100 dark:bg-steel-800"></div>
              <div>
                <div class="mb-4 flex items-center gap-2.5">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </div>
                  <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.approvalHistoryTitle') }}</h2>
                </div>

                <ol class="flex flex-col">
                  <li v-for="(approval, idx) in currentCycleApprovals" :key="approval.id" class="flex gap-3.5" :class="idx < currentCycleApprovals.length - 1 ? 'pb-4' : ''">
                    <div class="flex flex-col items-center">
                      <div class="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-extrabold" :class="approvalStepBadgeClass(approval)">
                        <svg v-if="approval.status === 'APPROVED'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" class="h-3.5 w-3.5">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M20 6L9 17l-5-5" />
                        </svg>
                        <svg v-else-if="approval.status === 'REJECTED'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" class="h-3.5 w-3.5">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M6 6l12 12M18 6L6 18" />
                        </svg>
                        <template v-else>{{ approval.stepOrder }}</template>
                      </div>
                      <div v-if="idx < currentCycleApprovals.length - 1" class="mt-1 w-0.5 flex-1" :class="approval.status === 'APPROVED' ? 'bg-emerald-200 dark:bg-emerald-900' : 'bg-steel-200 dark:bg-steel-700'"></div>
                    </div>
                    <div class="min-w-0 flex-1 pb-1 pt-0.5">
                      <p class="text-sm font-bold text-steel-800 dark:text-steel-50">{{ t(`dailyReports.approverFunction.${approval.approverFunction}`) }}</p>
                      <p v-if="approval.status === 'PENDING'" class="text-xs text-steel-500 dark:text-steel-400">{{ t('dailyReports.pendingStepSubtitle') }}</p>
                      <p v-else class="text-xs text-steel-500 dark:text-steel-400">
                        {{ t(approval.status === 'APPROVED' ? 'dailyReports.decidedByApproved' : 'dailyReports.decidedByRejected', { name: approval.decidedByName ?? '—', datetime: formatDateTime(approval.decidedAt) }) }}
                        <template v-if="approval.status === 'REJECTED' && approval.comment"> · {{ approval.comment }}</template>
                      </p>
                      <template v-if="approval.status === 'PENDING' && currentPendingApproval && approval.id === currentPendingApproval.id && canActOnApproval">
                        <div class="mt-2.5 flex flex-wrap items-center gap-2">
                          <button type="button" class="btn-success px-4 py-1.5 text-xs" @click="onApproveStep">{{ t('dailyReports.approveStepButton') }}</button>
                          <button type="button" class="btn-danger px-4 py-1.5 text-xs" @click="showRejectForm = !showRejectForm">{{ t('dailyReports.rejectStepButton') }}</button>
                        </div>
                        <form v-if="showRejectForm" class="mt-2.5 flex flex-wrap items-center gap-2" @submit.prevent="onRejectStep">
                          <input v-model="rejectReason" type="text" required :placeholder="t('dailyReports.rejectReasonPlaceholder')" class="field-input flex-1 text-sm" />
                          <button type="submit" class="btn-danger px-3 py-1.5 text-xs">{{ t('dailyReports.confirmReject') }}</button>
                        </form>
                      </template>
                    </div>
                  </li>
                </ol>
                <p v-if="approvalActionError" class="mt-3 text-sm text-safety-600 dark:text-safety-500">{{ approvalActionError }}</p>

                <div v-if="priorCycles.length > 0" class="mt-6 border-t border-steel-100 pt-4 dark:border-steel-800">
                  <p class="mb-2 text-xs font-bold uppercase tracking-wide text-steel-400 dark:text-steel-500">{{ t('dailyReports.priorCyclesTitle') }}</p>
                  <div v-for="cycle in priorCycles" :key="cycle.cycleNumber" class="mb-3 last:mb-0">
                    <p class="mb-1 text-xs font-semibold text-steel-500 dark:text-steel-400">{{ t('dailyReports.cycleLabel') }} {{ cycle.cycleNumber }}</p>
                    <ul class="space-y-1 border-l-2 border-steel-200 pl-3 dark:border-steel-700">
                      <li v-for="approval in cycle.approvals" :key="approval.id" class="flex items-center justify-between text-xs">
                        <span class="text-steel-600 dark:text-steel-300">
                          {{ t('dailyReports.stepLabel') }} {{ approval.stepOrder }} — {{ t(`dailyReports.approverFunction.${approval.approverFunction}`) }}
                          <template v-if="approval.decidedByName"> · {{ approval.decidedByName }}</template>
                        </span>
                        <StatusBadge kind="approval" :status="approval.status" />
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </aside>

        <main class="min-w-0 flex-1 space-y-5 lg:order-1">
          <p v-if="submitError" class="text-sm text-safety-600 dark:text-safety-500">{{ submitError }}</p>
          <p v-if="deleteError" class="text-sm text-safety-600 dark:text-safety-500">{{ deleteError }}</p>
          <p v-if="pdfError" class="text-sm text-safety-600 dark:text-safety-500">{{ pdfError }}</p>
          <p v-if="saveError" class="text-sm text-safety-600 dark:text-safety-500">{{ saveError }}</p>

        <!-- Clima & Expediente -->
        <section class="card card-pad">
          <div class="mb-4 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <circle cx="12" cy="12" r="4" />
                <path stroke-linecap="round" d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.core.title') }}</h2>
          </div>

          <div class="mb-5 grid grid-cols-1 gap-6 sm:grid-cols-2">
            <div>
              <div class="mb-2 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">
                {{ t('dailyReports.core.weatherConditionMorning') }} <span class="text-safety-500">*</span>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="option in WEATHER_OPTIONS"
                  :key="option"
                  type="button"
                  :disabled="!isDraft"
                  class="flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-sm font-semibold transition disabled:cursor-not-allowed"
                  :class="
                    weatherConditionMorning === option
                      ? 'bg-blueprint-600 text-white'
                      : 'border border-steel-300 text-steel-600 hover:bg-steel-50 dark:border-steel-700 dark:text-steel-300 dark:hover:bg-steel-800'
                  "
                  @click="isDraft && (weatherConditionMorning = weatherConditionMorning === option ? '' : option)"
                >
                  <svg v-if="weatherIconKind(option) === 'sun'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
                    <circle cx="12" cy="12" r="4" />
                    <path stroke-linecap="round" d="M12 3v1.5M12 19.5V21M4.2 4.2l1 1M18.8 18.8l1 1M3 12h1.5M19.5 12H21M4.2 19.8l1-1M18.8 5.2l1-1" />
                  </svg>
                  <svg v-else-if="weatherIconKind(option) === 'cloud'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17.5 19a4.5 4.5 0 000-9 6 6 0 00-11.4 1.8A4 4 0 007 19h10.5z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17.5 16a4.5 4.5 0 000-9 6 6 0 00-11.4 1.8A4 4 0 007 16h10.5zM8 20l-1 2M12 20l-1 2M16 20l-1 2" />
                  </svg>
                  {{ t(`dailyReports.core.weatherOptions.${option}`) }}
                </button>
              </div>
            </div>
            <div>
              <div class="mb-2 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">
                {{ t('dailyReports.core.weatherConditionAfternoon') }} <span class="text-safety-500">*</span>
              </div>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="option in WEATHER_OPTIONS"
                  :key="option"
                  type="button"
                  :disabled="!isDraft"
                  class="flex items-center gap-1.5 rounded-lg px-3.5 py-2 text-sm font-semibold transition disabled:cursor-not-allowed"
                  :class="
                    weatherConditionAfternoon === option
                      ? 'bg-blueprint-600 text-white'
                      : 'border border-steel-300 text-steel-600 hover:bg-steel-50 dark:border-steel-700 dark:text-steel-300 dark:hover:bg-steel-800'
                  "
                  @click="isDraft && (weatherConditionAfternoon = weatherConditionAfternoon === option ? '' : option)"
                >
                  <svg v-if="weatherIconKind(option) === 'sun'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
                    <circle cx="12" cy="12" r="4" />
                    <path stroke-linecap="round" d="M12 3v1.5M12 19.5V21M4.2 4.2l1 1M18.8 18.8l1 1M3 12h1.5M19.5 12H21M4.2 19.8l1-1M18.8 5.2l1-1" />
                  </svg>
                  <svg v-else-if="weatherIconKind(option) === 'cloud'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17.5 19a4.5 4.5 0 000-9 6 6 0 00-11.4 1.8A4 4 0 007 19h10.5z" />
                  </svg>
                  <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17.5 16a4.5 4.5 0 000-9 6 6 0 00-11.4 1.8A4 4 0 007 16h10.5zM8 20l-1 2M12 20l-1 2M16 20l-1 2" />
                  </svg>
                  {{ t(`dailyReports.core.weatherOptions.${option}`) }}
                </button>
              </div>
            </div>
          </div>

          <label class="mb-5 flex items-center gap-2 text-sm text-steel-600 dark:text-steel-300" :class="isDraft ? 'cursor-pointer' : 'opacity-80'">
            <input v-model="weatherBlockedTasks" type="checkbox" :disabled="!isDraft" class="field-checkbox" />
            {{ t('dailyReports.core.weatherBlockedTasks') }}
          </label>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div>
              <div class="mb-1.5 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.workHoursStart') }} <span class="text-safety-500">*</span></div>
              <TimeClockPicker v-if="isDraft" v-model="workHoursStart" />
              <div v-else class="field-input flex items-center text-steel-800 dark:text-steel-50">{{ workHoursStart || '—' }}</div>
            </div>
            <div>
              <div class="mb-1.5 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.workHoursEnd') }} <span class="text-safety-500">*</span></div>
              <TimeClockPicker v-if="isDraft" v-model="workHoursEnd" />
              <div v-else class="field-input flex items-center text-steel-800 dark:text-steel-50">{{ workHoursEnd || '—' }}</div>
            </div>
          </div>

          <div class="mt-4">
            <div class="mb-1.5 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.comments') }}</div>
            <textarea v-if="isDraft" v-model="comments" rows="3" class="field-input"></textarea>
            <div v-else class="rounded-lg border border-steel-200 bg-steel-50 px-3.5 py-3 text-sm text-steel-600 dark:border-steel-700 dark:bg-steel-800/60 dark:text-steel-300">
              {{ comments || '—' }}
            </div>
          </div>
        </section>

        <!-- Mão de obra -->
        <section class="card card-pad">
          <div class="mb-4 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <circle cx="9" cy="8" r="3" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M16 8a3 3 0 110 6M22 20c0-2.6-1.7-4.8-4-5.6" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.workforce.title') }}</h2>
          </div>

          <div class="mb-2.5 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.workforce.teamSectionLabel') }}</div>
          <p v-if="activeMembers.length === 0" class="mb-5 text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.workforce.noMembers') }}</p>
          <div v-else class="mb-5 flex flex-col gap-2">
            <div
              v-for="member in activeMembers"
              :key="member.membershipId"
              class="flex items-center gap-3 rounded-xl border px-3.5 py-2.5"
              :class="workforceRowFor(member.membershipId) ? 'border-blueprint-600 bg-blueprint-50 dark:bg-blueprint-500/10' : 'border-steel-200 dark:border-steel-700'"
            >
              <div
                class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-bold"
                :class="workforceRowFor(member.membershipId) ? 'bg-blueprint-600 text-white' : 'bg-steel-100 text-steel-500 dark:bg-steel-700 dark:text-steel-300'"
              >
                {{ initials(member.displayName) }}
              </div>
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-bold text-steel-800 dark:text-steel-50">{{ member.displayName ?? member.email ?? '—' }}</div>
                <div class="truncate text-xs text-steel-500 dark:text-steel-400">{{ member.trade || t(`siteTeam.function.${member.function}`) }}</div>
              </div>
              <template v-if="workforceRowFor(member.membershipId)">
                <div v-if="isDraft" class="flex shrink-0 items-center gap-1.5 rounded-lg border border-steel-300 bg-white dark:border-steel-600 dark:bg-steel-900">
                  <button type="button" class="flex h-7 w-7 items-center justify-center text-steel-600 dark:text-steel-300" @click="decrementWorkforce(workforceRowFor(member.membershipId)!)">–</button>
                  <span class="w-4 text-center text-sm font-bold text-steel-800 dark:text-steel-50">{{ workforceRowFor(member.membershipId)!.headcount }}</span>
                  <button type="button" class="flex h-7 w-7 items-center justify-center text-steel-600 dark:text-steel-300" @click="incrementWorkforce(workforceRowFor(member.membershipId)!)">+</button>
                </div>
                <span v-else class="shrink-0 text-sm font-bold text-steel-600 dark:text-steel-300">{{ workforceRowFor(member.membershipId)!.headcount }}</span>
              </template>
              <button v-else-if="isDraft" type="button" class="btn-secondary shrink-0 px-3.5 py-1.5 text-xs" @click="addTeamMember(member)">
                {{ t('dailyReports.workforce.addButton') }}
              </button>
            </div>
          </div>

          <div class="mb-2.5 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.workforce.customSectionLabel') }}</div>
          <div v-if="customWorkforceRows.length > 0" class="mb-3.5 flex flex-col gap-2">
            <div v-for="row in customWorkforceRows" :key="row.key" class="flex items-center gap-2.5 rounded-xl border border-dashed border-steel-300 px-3.5 py-2.5 dark:border-steel-600">
              <input
                v-model="row.roleDescription"
                type="text"
                :disabled="!isDraft"
                :placeholder="t('dailyReports.workforce.customRolePlaceholder')"
                class="min-w-0 flex-1 border-0 bg-transparent p-0 text-sm text-steel-800 outline-none dark:text-steel-50"
              />
              <input v-model.number="row.headcount" type="number" min="1" :disabled="!isDraft" class="field-input w-16 shrink-0 py-1 text-center text-sm" />
              <span class="shrink-0 text-xs text-steel-400 dark:text-steel-500">{{ t('dailyReports.workforce.peopleUnit') }}</span>
              <button
                v-if="isDraft"
                type="button"
                class="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-steel-400 hover:bg-steel-100 dark:hover:bg-steel-700"
                :aria-label="t('dailyReports.workforce.removeLabel')"
                @click="removeWorkforceRow(row)"
              >
                ✕
              </button>
            </div>
          </div>
          <button
            v-if="isDraft"
            type="button"
            class="flex items-center gap-1.5 rounded-lg border border-dashed border-steel-300 px-4 py-2 text-sm font-bold text-blueprint-600 dark:border-steel-600 dark:text-blueprint-400"
            @click="addCustomWorkforceRow"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
              <path stroke-linecap="round" d="M12 5v14M5 12h14" />
            </svg>
            {{ t('dailyReports.workforce.addCustomButton') }}
          </button>
          <p v-if="customWorkforceRows.length === 0 && activeMembers.length === 0" class="mt-2 text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.workforce.empty') }}</p>
        </section>

        <!-- Equipamentos -->
        <section class="card card-pad">
          <div class="mb-4 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <rect x="3" y="4" width="14" height="10" rx="2" />
                <path stroke-linecap="round" stroke-linejoin="round" d="M17 8h3l1.5 2.5V14H17M6 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM18.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.equipmentUsage.title') }}</h2>
          </div>

          <div class="mb-3.5 flex items-center gap-2.5 rounded-lg border border-steel-300 px-3.5 py-2.5 dark:border-steel-600">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0 text-steel-400">
              <circle cx="11" cy="11" r="7" />
              <path stroke-linecap="round" d="M21 21l-4.3-4.3" />
            </svg>
            <input
              v-model="equipmentSearchTerm"
              type="text"
              :placeholder="t('dailyReports.equipmentUsage.searchPlaceholder')"
              class="min-w-0 flex-1 border-0 bg-transparent p-0 text-sm text-steel-800 outline-none dark:text-steel-50"
            />
          </div>

          <p v-if="!equipmentSearching && equipmentCatalog.length === 0" class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.equipmentUsage.noResults') }}</p>
          <div v-else class="mb-4 flex flex-col overflow-hidden rounded-xl border border-steel-200 dark:border-steel-700">
            <label
              v-for="eq in equipmentCatalog"
              :key="eq.id"
              class="flex items-center gap-3 border-b border-steel-100 px-3.5 py-3 last:border-b-0 dark:border-steel-800"
              :class="isDraft ? 'cursor-pointer hover:bg-steel-50 dark:hover:bg-steel-800/60' : ''"
            >
              <input type="checkbox" :checked="!!equipmentRowFor(eq.id)" :disabled="!isDraft" class="field-checkbox" @change="isDraft && toggleRegisteredEquipment(eq)" />
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-bold text-steel-800 dark:text-steel-50">{{ eq.name }}</div>
                <div class="truncate text-xs text-steel-400 dark:text-steel-500">{{ eq.type || '—' }}</div>
              </div>
              <span class="badge shrink-0" :class="equipmentStatusBadgeClass(eq.status)">{{ t(`equipment.status.${eq.status}`) }}</span>
            </label>
          </div>

          <div class="mb-2.5 text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.equipmentUsage.customSectionLabel') }}</div>
          <div v-if="customEquipmentRows.length > 0" class="mb-3.5 flex flex-col gap-2">
            <div v-for="row in customEquipmentRows" :key="row.key" class="flex items-center gap-2.5 rounded-xl border border-dashed border-steel-300 px-3.5 py-2.5 dark:border-steel-600">
              <input
                v-model="row.customName"
                type="text"
                :disabled="!isDraft"
                :placeholder="t('dailyReports.equipmentUsage.customNamePlaceholder')"
                class="min-w-0 flex-1 border-0 bg-transparent p-0 text-sm text-steel-800 outline-none dark:text-steel-50"
              />
              <input
                v-model="row.statusNote"
                type="text"
                :disabled="!isDraft"
                :placeholder="t('dailyReports.equipmentUsage.customNotePlaceholder')"
                class="w-52 shrink-0 border-0 bg-transparent p-0 text-sm text-steel-500 outline-none dark:text-steel-400"
              />
              <button
                v-if="isDraft"
                type="button"
                class="flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-steel-400 hover:bg-steel-100 dark:hover:bg-steel-700"
                :aria-label="t('dailyReports.workforce.removeLabel')"
                @click="removeEquipmentRow(row)"
              >
                ✕
              </button>
            </div>
          </div>
          <button
            v-if="isDraft"
            type="button"
            class="flex items-center gap-1.5 rounded-lg border border-dashed border-steel-300 px-4 py-2 text-sm font-bold text-blueprint-600 dark:border-steel-600 dark:text-blueprint-400"
            @click="addCustomEquipmentRow"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
              <path stroke-linecap="round" d="M12 5v14M5 12h14" />
            </svg>
            {{ t('dailyReports.equipmentUsage.addCustomButton') }}
          </button>
        </section>

        <!-- Atividades -->
        <section class="card card-pad">
          <div class="mb-4 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 11l3 3L22 4M9 12v7a2 2 0 01-2 2H4a2 2 0 01-2-2V7a2 2 0 012-2h11" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.activities.title') }}</h2>
          </div>

          <p v-if="visibleActivityRows.length === 0" class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.activities.empty') }}</p>
          <div v-else class="mb-4 flex flex-col gap-3">
            <div v-for="row in visibleActivityRows" :key="row.key" class="rounded-xl border border-steel-200 p-3.5 dark:border-steel-700">
              <div class="mb-2 flex items-start justify-between gap-2.5">
                <span class="text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.activities.description') }} <span class="text-safety-500">*</span></span>
                <button
                  v-if="isDraft"
                  type="button"
                  class="flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-steel-400 hover:bg-steel-100 dark:hover:bg-steel-700"
                  :aria-label="t('dailyReports.activities.removeLabel')"
                  @click="removeActivityRow(row)"
                >
                  ✕
                </button>
              </div>
              <textarea
                v-if="isDraft"
                v-model="row.description"
                rows="2"
                required
                :placeholder="t('dailyReports.activities.descriptionPlaceholder')"
                class="field-input mb-3 text-sm"
              ></textarea>
              <p v-else class="mb-3 text-sm text-steel-800 dark:text-steel-50">{{ row.description }}</p>
              <div class="flex gap-2">
                <button
                  type="button"
                  :disabled="!isDraft"
                  class="rounded-lg px-3 py-1.5 text-xs font-bold transition disabled:cursor-not-allowed"
                  :class="row.status === 'IN_PROGRESS' ? 'bg-steel-100 text-steel-600 dark:bg-steel-700 dark:text-steel-200' : 'border border-steel-200 text-steel-500 dark:border-steel-700 dark:text-steel-400'"
                  @click="isDraft && (row.status = 'IN_PROGRESS')"
                >
                  {{ t('dailyReports.activities.statusOptions.IN_PROGRESS') }}
                </button>
                <button
                  type="button"
                  :disabled="!isDraft"
                  class="rounded-lg px-3 py-1.5 text-xs font-bold transition disabled:cursor-not-allowed"
                  :class="row.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300' : 'border border-emerald-200 text-emerald-600 dark:border-emerald-800 dark:text-emerald-400'"
                  @click="isDraft && (row.status = 'COMPLETED')"
                >
                  {{ t('dailyReports.activities.statusOptions.COMPLETED') }}
                </button>
              </div>
            </div>
          </div>
          <button
            v-if="isDraft"
            type="button"
            class="flex items-center gap-1.5 rounded-lg border border-dashed border-steel-300 px-4 py-2 text-sm font-bold text-blueprint-600 dark:border-steel-600 dark:text-blueprint-400"
            @click="addActivityRow"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[15px] w-[15px]">
              <path stroke-linecap="round" d="M12 5v14M5 12h14" />
            </svg>
            {{ t('dailyReports.activities.addActivityButton') }}
          </button>
        </section>

        <!-- Ocorrências (non-goal: restyled only, not part of the approved mockup) -->
        <section class="card card-pad">
          <div class="mb-3.5 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86L2.1 18.05A1 1 0 003 19.5h18a1 1 0 00.9-1.45L13.71 3.86a1 1 0 00-1.72 0z" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.occurrences.title') }}</h2>
          </div>
          <p v-if="detail.occurrences.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.occurrences.empty') }}</p>
          <ul v-else class="mb-3 space-y-1.5">
            <li v-for="occurrence in detail.occurrences" :key="occurrence.id" class="rounded-md border border-steel-200 px-3 py-1.5 text-sm text-steel-800 dark:border-steel-700 dark:text-steel-50">
              {{ occurrence.description }}
            </li>
          </ul>
          <form v-if="isDraft" class="flex flex-wrap items-end gap-2" @submit.prevent="onAddOccurrence">
            <input v-model="occurrenceDescription" type="text" required :placeholder="t('dailyReports.occurrences.description')" class="field-input flex-1" />
            <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.occurrences.addButton') }}</button>
          </form>
          <p v-if="occurrenceError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ occurrenceError }}</p>
        </section>

        <!-- Materiais recebidos: delivered-from-PR (new, read-only) + manual entry (unchanged) -->
        <section class="card card-pad">
          <div class="mb-1.5 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 7h11v8H3V7zM14 10h4l3 3v2h-7v-5zM6.5 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM17.5 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.materialsReceived.title') }}</h2>
          </div>
          <p class="mb-4 text-xs text-steel-400 dark:text-steel-500">{{ t('dailyReports.deliveredMaterials.subtitle', { date: formatDateBR(detail.report.reportDate) }) }}</p>

          <p v-if="deliveredMaterials.length === 0" class="mb-5 text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.deliveredMaterials.empty') }}</p>
          <div v-else class="mb-5 flex flex-col gap-2.5">
            <div v-for="material in deliveredMaterials" :key="material.id" class="flex flex-wrap items-center gap-3.5 rounded-xl border border-steel-200 px-3.5 py-3 dark:border-steel-700">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-steel-50 text-steel-500 dark:bg-steel-800 dark:text-steel-400">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[17px] w-[17px]">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 7h11v8H3V7zM14 10h4l3 3v2h-7v-5z" />
                </svg>
              </div>
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-bold text-steel-800 dark:text-steel-50">{{ material.name }}</div>
                <div class="truncate text-xs text-steel-400 dark:text-steel-500">
                  {{ material.quantity }}<template v-if="material.type"> · {{ material.type }}</template>
                  <template v-if="material.sourcePurchaseRequestId">
                    ·
                    <router-link :to="`/purchase-requests/${material.sourcePurchaseRequestId}`" class="text-blueprint-600 hover:underline dark:text-blueprint-400">
                      {{ material.sourcePurchaseRequestName ?? t('materialDelivery.table.purchaseRequest') }}
                    </router-link>
                  </template>
                </div>
              </div>
              <StatusBadge kind="material" :status="material.status" />
              <button
                v-if="material.status === 'DELIVERED'"
                type="button"
                :disabled="markingCheckedId === material.id"
                class="btn-success shrink-0 px-3.5 py-1.5 text-xs"
                @click="onMarkDeliveredChecked(material)"
              >
                {{ t('dailyReports.deliveredMaterials.markCheckedButton') }}
              </button>
            </div>
          </div>
          <p v-if="deliveredMaterialsError" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ deliveredMaterialsError }}</p>

          <div class="border-t border-steel-100 pt-5 dark:border-steel-800">
            <h3 class="mb-1 text-sm font-bold text-steel-700 dark:text-steel-200">{{ t('dailyReports.materialsReceived.manualTitle') }}</h3>
            <p class="mb-3 text-xs text-steel-400 dark:text-steel-500">{{ t('dailyReports.materialsReceived.subtitle') }}</p>
            <p v-if="detail.materialsReceived.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.materialsReceived.empty') }}</p>
            <ul v-else class="mb-3 space-y-1.5">
              <li v-for="received in detail.materialsReceived" :key="received.id" class="flex justify-between rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
                <span class="text-steel-800 dark:text-steel-50">{{ received.materialName }}</span>
                <span class="text-steel-500 dark:text-steel-400">{{ received.quantity }}{{ received.unit ? ` ${received.unit}` : '' }}</span>
              </li>
            </ul>
            <form v-if="isDraft" class="flex flex-wrap items-end gap-2" @submit.prevent="onAddMaterialReceived">
              <input v-model="materialReceivedName" required :placeholder="t('dailyReports.materialsReceived.material')" class="field-input" />
              <input v-model="materialReceivedUnit" :placeholder="t('dailyReports.materialsReceived.unit')" class="field-input w-24" />
              <input v-model="materialQuantity" type="number" step="0.001" min="0" required :placeholder="t('dailyReports.materialsReceived.quantity')" class="field-input w-32" />
              <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.materialsReceived.addButton') }}</button>
            </form>
            <p v-if="materialReceivedError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ materialReceivedError }}</p>
          </div>
        </section>

        <!-- Mídia & Anexos -->
        <section class="card card-pad">
          <div class="mb-4 flex items-center gap-2.5">
            <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 7h3l2-2h6l2 2h3a1 1 0 011 1v11a1 1 0 01-1 1H4a1 1 0 01-1-1V8a1 1 0 011-1z" />
                <circle cx="12" cy="13" r="3.5" />
              </svg>
            </div>
            <h2 class="text-base font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.media.title') }}</h2>
          </div>

          <div
            v-if="isDraft"
            class="mb-5 rounded-2xl border-2 border-dashed px-9 py-9 text-center transition"
            :class="isDraggingMedia ? 'border-blueprint-500 bg-blueprint-50 dark:bg-blueprint-500/10' : 'border-steel-300 dark:border-steel-600'"
            @dragover.prevent="onMediaDragOver"
            @dragleave.prevent="onMediaDragLeave"
            @drop.prevent="onMediaDrop"
          >
            <label class="flex cursor-pointer flex-col items-center gap-1">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" class="mb-1 h-[30px] w-[30px] text-steel-400">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 0L7 9m5-5l5 5" />
                <path stroke-linecap="round" d="M4 16v3a1 1 0 001 1h14a1 1 0 001-1v-3" />
              </svg>
              <span class="text-sm font-bold text-steel-700 dark:text-steel-200">{{ t('dailyReports.media.dropzoneTitle') }}</span>
              <span class="text-xs text-steel-400 dark:text-steel-500">{{ t('dailyReports.media.dropzoneSubtitle') }}</span>
              <input type="file" accept="image/*,video/*" multiple class="hidden" @change="onMediaInputChange" />
            </label>
          </div>

          <div v-if="media.length > 0" class="mb-6 grid grid-cols-2 gap-3.5 sm:grid-cols-3 lg:grid-cols-4">
            <div v-for="item in media" :key="item.id">
              <div class="relative h-28 overflow-hidden rounded-xl border border-steel-200 dark:border-steel-700">
                <img
                  v-if="item.type === 'PHOTO' && mediaPreviewUrls[item.id]"
                  :src="mediaPreviewUrls[item.id]"
                  class="h-full w-full cursor-pointer object-cover"
                  @click="openMediaLightbox(item)"
                />
                <div v-else class="flex h-full w-full items-center justify-center bg-steel-100 text-steel-400 dark:bg-steel-800 dark:text-steel-500">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="h-7 w-7">
                    <rect x="3" y="5" width="14" height="14" rx="2" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17 9l4-2v10l-4-2" />
                  </svg>
                </div>
                <button
                  v-if="isDraft"
                  type="button"
                  class="absolute right-1.5 top-1.5 flex h-6 w-6 items-center justify-center rounded-md bg-black/55 text-xs text-white hover:bg-black/70"
                  :aria-label="t('dailyReports.media.removeLabel')"
                  @click="onRemoveMedia(item)"
                >
                  ✕
                </button>
              </div>
              <input
                v-model="mediaCaptionDrafts[item.id]"
                type="text"
                :disabled="!isDraft"
                :placeholder="t('dailyReports.media.captionPlaceholder')"
                class="mt-1.5 w-full rounded-lg border border-steel-200 px-2 py-1.5 text-xs text-steel-800 dark:border-steel-700 dark:bg-steel-900 dark:text-steel-50"
              />
            </div>
            <label
              v-if="isDraft"
              class="flex h-28 cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed border-steel-300 text-steel-400 transition hover:border-blueprint-400 hover:text-blueprint-500 dark:border-steel-600"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[22px] w-[22px]">
                <path stroke-linecap="round" d="M12 5v14M5 12h14" />
              </svg>
              <input type="file" accept="image/*,video/*" multiple class="hidden" @change="onMediaInputChange" />
            </label>
          </div>
          <p v-if="mediaError" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ mediaError }}</p>

          <div class="mb-2.5 flex items-center justify-between">
            <span class="text-xs font-bold uppercase tracking-wide text-steel-600 dark:text-steel-300">{{ t('dailyReports.attachments.title') }}</span>
            <label v-if="isDraft" class="flex cursor-pointer items-center gap-1 text-xs font-bold text-blueprint-600 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5">
                <path stroke-linecap="round" d="M12 5v14M5 12h14" />
              </svg>
              {{ t('dailyReports.attachments.uploadButton') }}
              <input type="file" multiple class="hidden" @change="onAttachmentFilesSelected" />
            </label>
          </div>
          <p v-if="anexoRows.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.attachments.empty') }}</p>
          <div v-else class="flex flex-col gap-2">
            <div
              v-for="row in anexoRows"
              :key="`${row.kind}-${row.data.id}`"
              class="flex items-center gap-3 rounded-xl border px-3.5 py-2.5"
              :class="row.kind === 'invoice' ? 'border-blueprint-200 bg-blueprint-50 dark:border-blueprint-800 dark:bg-blueprint-500/10' : 'border-steel-200 dark:border-steel-700'"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke-width="1.8" class="h-[18px] w-[18px] shrink-0"
                :stroke="row.kind === 'invoice' ? '#2f53f0' : 'currentColor'"
                :class="row.kind === 'invoice' ? '' : 'text-steel-500 dark:text-steel-400'"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M14 3v5a1 1 0 001 1h5M6 3h8l5 5v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5a2 2 0 012-2z" />
              </svg>
              <div class="min-w-0 flex-1">
                <button type="button" class="block max-w-full truncate text-left text-sm font-semibold text-steel-800 hover:underline dark:text-steel-50" @click="onDownloadAnexo(row)">
                  {{ row.data.originalName }}
                </button>
                <span v-if="row.kind === 'invoice'" class="mt-0.5 block text-[11px] font-bold text-blueprint-600 dark:text-blueprint-400">{{ anexoInvoiceLabel(row) }}</span>
              </div>
              <button v-if="row.kind === 'attachment' && isDraft" type="button" class="shrink-0 text-xs font-medium text-safety-600 hover:underline dark:text-safety-500" @click="onRemoveAnexo(row)">
                {{ t('dailyReports.attachments.removeButton') }}
              </button>
            </div>
          </div>
          <p v-if="attachmentError" class="mt-2 text-sm text-safety-600 dark:text-safety-500">{{ attachmentError }}</p>
        </section>
        </main>
        </div>
      </div>

      <p v-else-if="loadError" class="app-container py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
    </div>

    <div v-if="mediaLightbox" class="fixed inset-0 z-40 flex items-center justify-center bg-black/60 p-4" @click.self="closeMediaLightbox">
      <div class="modal-panel flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden">
        <div class="flex shrink-0 items-center justify-between gap-3 border-b border-steel-200 px-4 py-3 dark:border-steel-700">
          <p class="min-w-0 flex-1 truncate text-sm font-medium text-steel-800 dark:text-steel-50">{{ mediaLightbox.caption || t('dailyReports.media.title') }}</p>
          <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="closeMediaLightbox">{{ t('dailyReports.media.close') }}</button>
        </div>
        <div class="flex min-h-0 flex-1 items-center justify-center overflow-auto p-4">
          <p v-if="mediaLightboxLoading" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.media.loading') }}</p>
          <p v-else-if="mediaLightboxError" class="text-sm text-safety-600 dark:text-safety-500">{{ mediaLightboxError }}</p>
          <img v-else :src="mediaLightboxUrl" :alt="mediaLightbox.caption ?? ''" class="max-h-[80vh] max-w-full object-contain" />
        </div>
      </div>
    </div>
  </div>
</template>
