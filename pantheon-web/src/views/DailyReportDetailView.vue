<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  useDailyReports,
  type ActivityStatus,
  type DailyReportDetail,
  type MediaKind,
  type ReportAttachment,
  type ReportMedia,
  type ReportSignature,
} from '../composables/useDailyReports'
import { useEquipment, type Equipment } from '../composables/useEquipment'
import { useSiteMembers, type ConstructionFunction, type SiteMember } from '../composables/useSiteMembers'
import { useSitePermissions, type AccessLevel } from '../composables/useSitePermissions'
import { useConstructionSites } from '../composables/useConstructionSites'
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
} = useDailyReports()
const { listEquipment } = useEquipment()
const { listMembers, getMyFunction } = useSiteMembers()
const { getMyPermissions } = useSitePermissions()
const { getSite } = useConstructionSites()

const reportId = route.params.id as string
const detail = ref<DailyReportDetail | null>(null)
const equipmentCatalog = ref<Equipment[]>([])
const siteMembers = ref<SiteMember[]>([])
const siteName = ref<string | null>(null)
const loading = ref(false)
const loadError = ref('')
const myAccessLevel = ref<AccessLevel | null>(null)
const myFunction = ref<ConstructionFunction | null>(null)

const isDraft = computed(() => detail.value?.report.status === 'DRAFT')
const isApproved = computed(() => detail.value?.report.status === 'APPROVED')

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

// core section
const WEATHER_OPTIONS = ['SUNNY', 'PARTLY_CLOUDY', 'CLOUDY', 'RAINY', 'STORM'] as const

const coreErrorMessage = ref('')
const coreSaving = ref(false)
const editingCore = ref(true)
const weatherCondition = ref('')
const weatherBlockedTasks = ref(false)
const workHoursStart = ref('')
const workHoursEnd = ref('')
const comments = ref('')

// workforce
const workforceError = ref('')
const selectedMembershipId = ref('')
const roleDescription = ref('')
const headcount = ref(1)

// equipment usage
const equipmentUsageError = ref('')
const selectedEquipmentId = ref('')
const equipmentStatusNote = ref('')

// activities
const activityError = ref('')
const activityDescription = ref('')
const activityProgressNote = ref('')
const activityStatus = ref<ActivityStatus>('IN_PROGRESS')

// occurrences
const occurrenceError = ref('')
const occurrenceDescription = ref('')

// materials received
const materialReceivedError = ref('')
const materialReceivedName = ref('')
const materialReceivedUnit = ref('')
const materialQuantity = ref('')

const submitError = ref('')
const submitting = ref(false)

const approvalActionError = ref('')
const rejectReason = ref('')
const showRejectForm = ref(false)

const confirmingDelete = ref(false)
const deleting = ref(false)
const deleteError = ref('')

// media
const media = ref<ReportMedia[]>([])
const mediaError = ref('')
const mediaCaption = ref('')
const mediaType = ref<MediaKind>('PHOTO')
const mediaFile = ref<File | null>(null)
const mediaPreviewUrls = ref<Record<string, string>>({})

// attachments
const attachments = ref<ReportAttachment[]>([])
const attachmentError = ref('')
const attachmentFile = ref<File | null>(null)

// signatures
const signatures = ref<ReportSignature[]>([])
const signError = ref('')
const signing = ref(false)

// pdf
const pdfError = ref('')
const downloadingPdf = ref(false)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getDetail(reportId)
    weatherCondition.value = detail.value.report.weatherCondition ?? ''
    weatherBlockedTasks.value = detail.value.report.weatherBlockedTasks ?? false
    workHoursStart.value = detail.value.report.workHoursStart ?? ''
    workHoursEnd.value = detail.value.report.workHoursEnd ?? ''
    comments.value = detail.value.report.comments ?? ''
    editingCore.value = !weatherCondition.value || !workHoursStart.value || !workHoursEnd.value

    const siteId = detail.value.report.constructionSiteId
    equipmentCatalog.value = (await listEquipment(siteId, { size: 200 })).content
    siteMembers.value = await listMembers(siteId)
    const [permissions, fn] = await Promise.all([getMyPermissions(siteId), getMyFunction(siteId)])
    myAccessLevel.value = permissions.DAILY_REPORT
    myFunction.value = fn
    getSite(siteId).then((s) => (siteName.value = s.name)).catch(() => {})

    media.value = await listMedia(reportId)
    attachments.value = await listAttachments(reportId)
    signatures.value = await listSignatures(reportId)
    await Promise.all(
      media.value
        .filter((m) => m.type === 'PHOTO')
        .map(async (m) => {
          const blob = await getMediaContentUrl(reportId, m.id)
          mediaPreviewUrls.value[m.id] = URL.createObjectURL(blob)
        }),
    )
  } catch {
    loadError.value = t('dailyReports.detail.loadError')
  } finally {
    loading.value = false
  }
}

function onMediaFileChange(event: Event) {
  mediaFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function onUploadMedia() {
  if (!mediaFile.value) return
  mediaError.value = ''
  try {
    const uploaded = await uploadMedia(reportId, mediaFile.value, mediaType.value, mediaCaption.value || null)
    media.value.push(uploaded)
    if (uploaded.type === 'PHOTO') {
      const blob = await getMediaContentUrl(reportId, uploaded.id)
      mediaPreviewUrls.value[uploaded.id] = URL.createObjectURL(blob)
    }
    mediaCaption.value = ''
    mediaFile.value = null
  } catch {
    mediaError.value = t('dailyReports.media.error')
  }
}

function onAttachmentFileChange(event: Event) {
  attachmentFile.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function onUploadAttachment() {
  if (!attachmentFile.value) return
  attachmentError.value = ''
  try {
    const uploaded = await uploadAttachment(reportId, attachmentFile.value)
    attachments.value.push(uploaded)
    attachmentFile.value = null
  } catch {
    attachmentError.value = t('dailyReports.attachments.error')
  }
}

async function onDownloadAttachment(attachment: ReportAttachment) {
  const blob = await getAttachmentContentUrl(reportId, attachment.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.target = '_blank'
  link.rel = 'noopener'
  link.click()
  URL.revokeObjectURL(url)
}

async function onSign() {
  signError.value = ''
  signing.value = true
  try {
    const signature = await signReport(reportId)
    signatures.value.push(signature)
  } catch {
    signError.value = t('dailyReports.signatures.error')
  } finally {
    signing.value = false
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
    link.target = '_blank'
    link.rel = 'noopener'
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    pdfError.value = t('dailyReports.pdf.error')
  } finally {
    downloadingPdf.value = false
  }
}

async function onSaveCore() {
  coreErrorMessage.value = ''
  if (!weatherCondition.value || !workHoursStart.value || !workHoursEnd.value) {
    coreErrorMessage.value = t('dailyReports.core.requiredError')
    return
  }
  coreSaving.value = true
  try {
    const updated = await updateCore(reportId, {
      weatherCondition: weatherCondition.value || null,
      weatherBlockedTasks: weatherBlockedTasks.value,
      workHoursStart: workHoursStart.value || null,
      workHoursEnd: workHoursEnd.value || null,
      comments: comments.value || null,
    })
    if (detail.value) detail.value.report = updated
    editingCore.value = false
  } catch {
    coreErrorMessage.value = t('dailyReports.core.error')
  } finally {
    coreSaving.value = false
  }
}

function onEditCore() {
  editingCore.value = true
}

async function onAddWorkforce() {
  workforceError.value = ''
  try {
    const entry = await addWorkforceEntry(reportId, {
      membershipId: selectedMembershipId.value || null,
      roleDescription: roleDescription.value || null,
      headcount: headcount.value,
    })
    detail.value?.workforceEntries.push(entry)
    selectedMembershipId.value = ''
    roleDescription.value = ''
    headcount.value = 1
  } catch {
    workforceError.value = t('dailyReports.workforce.error')
  }
}

async function onAddEquipmentUsage() {
  equipmentUsageError.value = ''
  try {
    const usage = await addEquipmentUsage(reportId, {
      equipmentId: selectedEquipmentId.value,
      statusNote: equipmentStatusNote.value || null,
    })
    detail.value?.equipmentUsage.push(usage)
    selectedEquipmentId.value = ''
    equipmentStatusNote.value = ''
  } catch {
    equipmentUsageError.value = t('dailyReports.equipmentUsage.error')
  }
}

async function onAddActivity() {
  activityError.value = ''
  try {
    const activity = await addActivity(reportId, {
      description: activityDescription.value,
      progressNote: activityProgressNote.value,
      status: activityStatus.value,
    })
    detail.value?.activities.push(activity)
    activityDescription.value = ''
    activityProgressNote.value = ''
    activityStatus.value = 'IN_PROGRESS'
  } catch {
    activityError.value = t('dailyReports.activities.error')
  }
}

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

function equipmentName(equipmentId: string): string {
  return equipmentCatalog.value.find((e) => e.id === equipmentId)?.name ?? equipmentId
}

onMounted(load)
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

    <main v-if="detail" class="app-container space-y-6 py-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">
            {{ t('dailyReports.history.reportLabel') }} #{{ detail.report.sequenceNo }} — {{ detail.report.reportDate }}
          </h1>
          <div class="mt-1.5">
            <StatusBadge kind="dailyReport" :status="detail.report.status" />
          </div>
        </div>
        <div class="flex gap-2">
          <button
            type="button"
            :disabled="downloadingPdf"
            class="btn-secondary"
            @click="onDownloadPdf"
          >
            {{ t('dailyReports.pdf.downloadButton') }}
          </button>
          <button
            v-if="canSubmit"
            type="button"
            :disabled="submitting"
            class="btn-primary"
            @click="onSubmitReport"
          >
            {{ t('dailyReports.detail.submitButton') }}
          </button>
          <div v-if="isDraft" class="relative">
            <button type="button" :disabled="deleting" class="btn-danger" @click="confirmingDelete = !confirmingDelete">
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
      </div>
      <p v-if="!isDraft" class="rounded-md bg-blueprint-50 px-4 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
        {{ t('dailyReports.detail.submittedNotice') }}
      </p>
      <p v-if="submitError" class="text-sm text-safety-600 dark:text-safety-500">{{ submitError }}</p>
      <p v-if="deleteError" class="text-sm text-safety-600 dark:text-safety-500">{{ deleteError }}</p>
      <p v-if="pdfError" class="text-sm text-safety-600 dark:text-safety-500">{{ pdfError }}</p>

      <!-- Approval -->
      <section v-if="detail.approvals.length > 0" class="card card-pad">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.approvalHistoryTitle') }}</h2>

        <div v-if="currentPendingApproval" class="mb-4 flex items-center gap-4 rounded-xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-800 dark:bg-amber-900/20">
          <div class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-amber-500 text-sm font-bold text-white">
            {{ currentPendingApproval.stepOrder }}
          </div>
          <div class="min-w-0 flex-1">
            <p class="text-sm font-semibold text-steel-800 dark:text-steel-50">
              {{ t('dailyReports.stepLabel') }} {{ currentPendingApproval.stepOrder }} — {{ t(`dailyReports.approverFunction.${currentPendingApproval.approverFunction}`) }}
            </p>
            <p class="text-xs text-amber-700 dark:text-amber-400">{{ t('dailyReports.pendingStepSubtitle') }}</p>
          </div>
          <div v-if="canActOnApproval" class="flex shrink-0 flex-wrap items-center gap-2">
            <button type="button" class="btn-danger px-3 py-1.5" @click="showRejectForm = !showRejectForm">{{ t('dailyReports.rejectStepButton') }}</button>
            <button type="button" class="btn-success px-3 py-1.5" @click="onApproveStep">{{ t('dailyReports.approveStepButton') }}</button>
          </div>
        </div>
        <template v-if="currentPendingApproval && canActOnApproval">
          <form v-if="showRejectForm" class="mb-4 flex flex-wrap items-center gap-2" @submit.prevent="onRejectStep">
            <input v-model="rejectReason" type="text" required :placeholder="t('dailyReports.rejectReasonPlaceholder')" class="field-input flex-1" />
            <button type="submit" class="btn-danger px-3 py-1.5">{{ t('dailyReports.confirmReject') }}</button>
          </form>
          <p v-if="approvalActionError" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ approvalActionError }}</p>
        </template>

        <div v-for="cycle in approvalsByCycle" :key="cycle.cycleNumber" class="mb-4 last:mb-0">
          <p class="mb-1.5 text-xs font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
            {{ t('dailyReports.cycleLabel') }} {{ cycle.cycleNumber }}
          </p>
          <ol class="space-y-1.5 border-l-2 border-steel-200 pl-4 dark:border-steel-700">
            <li v-for="approval in cycle.approvals" :key="approval.id" class="flex items-center justify-between text-sm">
              <span class="text-steel-700 dark:text-steel-200">
                {{ t('dailyReports.stepLabel') }} {{ approval.stepOrder }} — {{ t(`dailyReports.approverFunction.${approval.approverFunction}`) }}
                <span v-if="approval.status === 'REJECTED' && approval.comment" class="text-safety-600 dark:text-safety-500"> · {{ approval.comment }}</span>
              </span>
              <StatusBadge kind="approval" :status="approval.status" />
            </li>
          </ol>
        </div>
      </section>

      <!-- Core: weather, hours, comments -->
      <section class="card card-pad">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.core.title') }}</h2>
        <template v-if="isDraft && editingCore">
          <div class="flex flex-wrap items-start gap-x-6 gap-y-3">
            <div>
              <label class="mb-1.5 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.weatherCondition') }} <span class="text-safety-600 dark:text-safety-500">*</span></label>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="option in WEATHER_OPTIONS"
                  :key="option"
                  type="button"
                  class="rounded-lg border px-3 py-1.5 text-sm font-medium transition"
                  :class="
                    weatherCondition === option
                      ? 'border-blueprint-600 bg-blueprint-600 text-white'
                      : 'border-steel-300 text-steel-600 hover:bg-steel-50 dark:border-steel-700 dark:text-steel-300 dark:hover:bg-steel-800'
                  "
                  @click="weatherCondition = weatherCondition === option ? '' : option"
                >
                  {{ t(`dailyReports.core.weatherOptions.${option}`) }}
                </button>
              </div>
            </div>
            <div>
              <label class="mb-1.5 hidden text-sm font-medium sm:block">&nbsp;</label>
              <label class="flex h-[34px] items-center gap-2 text-sm text-steel-600 dark:text-steel-300">
                <input v-model="weatherBlockedTasks" type="checkbox" class="field-checkbox shrink-0" />
                {{ t('dailyReports.core.weatherBlockedTasks') }}
              </label>
            </div>
          </div>
          <div class="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.workHoursStart') }} <span class="text-safety-600 dark:text-safety-500">*</span></label>
              <TimeClockPicker v-model="workHoursStart" />
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.workHoursEnd') }} <span class="text-safety-600 dark:text-safety-500">*</span></label>
              <TimeClockPicker v-model="workHoursEnd" />
            </div>
          </div>
          <div class="mt-3">
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.comments') }}</label>
            <textarea v-model="comments" rows="3" class="field-input"></textarea>
          </div>
          <p v-if="coreErrorMessage" class="mt-2 text-sm text-safety-600 dark:text-safety-500">{{ coreErrorMessage }}</p>
          <button type="button" :disabled="coreSaving" class="btn-primary mt-3" @click="onSaveCore">
            {{ coreSaving ? t('dailyReports.core.saving') : t('dailyReports.core.save') }}
          </button>
        </template>
        <template v-else>
          <dl class="grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
            <div><dt class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.core.weatherCondition') }}</dt><dd class="text-steel-800 dark:text-steel-50">{{ detail.report.weatherCondition ? t(`dailyReports.core.weatherOptions.${detail.report.weatherCondition}`) : '—' }}</dd></div>
            <div><dt class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.core.workHoursStart') }} / {{ t('dailyReports.core.workHoursEnd') }}</dt><dd class="text-steel-800 dark:text-steel-50">{{ detail.report.workHoursStart ?? '—' }} - {{ detail.report.workHoursEnd ?? '—' }}</dd></div>
            <div class="sm:col-span-2"><dt class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.core.comments') }}</dt><dd class="text-steel-800 dark:text-steel-50">{{ detail.report.comments ?? '—' }}</dd></div>
          </dl>
          <button v-if="isDraft" type="button" class="btn-secondary mt-3" @click="onEditCore">
            {{ t('dailyReports.core.edit') }}
          </button>
        </template>
      </section>

      <!-- Workforce -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.workforce.title') }}</h2>
        <p v-if="detail.workforceEntries.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.workforce.empty') }}</p>
        <ul v-else class="mb-3 space-y-1.5">
          <li v-for="entry in detail.workforceEntries" :key="entry.id" class="flex justify-between rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
            <span class="text-steel-800 dark:text-steel-50">{{ entry.roleDescription }}</span>
            <span class="text-steel-500 dark:text-steel-400">{{ entry.headcount }}</span>
          </li>
        </ul>
        <form v-if="isDraft" class="flex flex-wrap items-end gap-2" @submit.prevent="onAddWorkforce">
          <select v-if="siteMembers.length > 0" v-model="selectedMembershipId" class="field-input">
            <option value="">{{ t('dailyReports.workforce.noMember') }}</option>
            <option v-for="member in siteMembers" :key="member.membershipId" :value="member.membershipId">{{ member.displayName }}</option>
          </select>
          <input
            v-model="roleDescription"
            type="text"
            :required="!selectedMembershipId"
            :placeholder="t('dailyReports.workforce.roleDescriptionPlaceholder')"
            class="field-input flex-1"
          />
          <input v-model.number="headcount" type="number" min="1" required class="field-input w-24" />
          <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.workforce.addButton') }}</button>
        </form>
        <p v-if="workforceError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ workforceError }}</p>
      </section>

      <!-- Equipment usage -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.equipmentUsage.title') }}</h2>
        <p v-if="detail.equipmentUsage.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.equipmentUsage.empty') }}</p>
        <ul v-else class="mb-3 space-y-1.5">
          <li v-for="usage in detail.equipmentUsage" :key="usage.id" class="flex justify-between rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
            <span class="text-steel-800 dark:text-steel-50">{{ equipmentName(usage.equipmentId) }}</span>
            <span class="text-steel-500 dark:text-steel-400">{{ usage.statusNote }}</span>
          </li>
        </ul>
        <form v-if="isDraft" class="flex flex-wrap items-end gap-2" @submit.prevent="onAddEquipmentUsage">
          <select v-model="selectedEquipmentId" required class="field-input">
            <option value="" disabled>{{ t('dailyReports.equipmentUsage.equipment') }}</option>
            <option v-for="eq in equipmentCatalog" :key="eq.id" :value="eq.id">{{ eq.name }}</option>
          </select>
          <input v-model="equipmentStatusNote" type="text" :placeholder="t('dailyReports.equipmentUsage.statusNotePlaceholder')" class="field-input flex-1" />
          <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.equipmentUsage.addButton') }}</button>
        </form>
        <p v-if="equipmentUsageError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ equipmentUsageError }}</p>
      </section>

      <!-- Activities -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.activities.title') }}</h2>
        <p v-if="detail.activities.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.activities.empty') }}</p>
        <ul v-else class="mb-3 space-y-1.5">
          <li v-for="activity in detail.activities" :key="activity.id" class="rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
            <div class="flex justify-between">
              <span class="text-steel-800 dark:text-steel-50">{{ activity.description }}</span>
              <span class="text-steel-500 dark:text-steel-400">{{ t(`dailyReports.activities.statusOptions.${activity.status}`) }}</span>
            </div>
            <p class="text-xs text-steel-500 dark:text-steel-400">{{ activity.progressNote }}</p>
          </li>
        </ul>
        <form v-if="isDraft" class="space-y-2" @submit.prevent="onAddActivity">
          <div class="flex flex-wrap gap-2">
            <input v-model="activityDescription" type="text" required :placeholder="t('dailyReports.activities.description')" class="field-input flex-1" />
            <input v-model="activityProgressNote" type="text" required :placeholder="t('dailyReports.activities.progressNotePlaceholder')" class="field-input w-40" />
            <select v-model="activityStatus" class="field-input">
              <option value="IN_PROGRESS">{{ t('dailyReports.activities.statusOptions.IN_PROGRESS') }}</option>
              <option value="COMPLETED">{{ t('dailyReports.activities.statusOptions.COMPLETED') }}</option>
            </select>
          </div>
          <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.activities.addButton') }}</button>
        </form>
        <p v-if="activityError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ activityError }}</p>
      </section>

      <!-- Occurrences -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.occurrences.title') }}</h2>
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

      <!-- Materials received -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.materialsReceived.title') }}</h2>
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
      </section>

      <!-- Media -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.media.title') }}</h2>
        <p v-if="media.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.media.empty') }}</p>
        <div v-else class="mb-3 flex flex-wrap gap-3">
          <div v-for="item in media" :key="item.id" class="w-40">
            <img v-if="item.type === 'PHOTO' && mediaPreviewUrls[item.id]" :src="mediaPreviewUrls[item.id]" class="h-32 w-40 rounded-md border border-steel-200 object-cover dark:border-steel-700" />
            <div v-else class="flex h-32 w-40 items-center justify-center rounded-md border border-steel-200 text-xs text-steel-500 dark:border-steel-700 dark:text-steel-400">
              {{ t('dailyReports.media.videoLabel') }}
            </div>
            <p v-if="item.caption" class="mt-1 truncate text-xs text-steel-500 dark:text-steel-400">{{ item.caption }}</p>
          </div>
        </div>
        <form class="flex flex-wrap items-end gap-2" @submit.prevent="onUploadMedia">
          <select v-model="mediaType" class="field-input">
            <option value="PHOTO">{{ t('dailyReports.media.photoLabel') }}</option>
            <option value="VIDEO">{{ t('dailyReports.media.videoLabel') }}</option>
          </select>
          <input type="file" accept="image/*,video/*" required @change="onMediaFileChange" class="text-sm text-steel-600 dark:text-steel-300" />
          <input v-model="mediaCaption" type="text" :placeholder="t('dailyReports.media.captionPlaceholder')" class="field-input flex-1" />
          <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.media.uploadButton') }}</button>
        </form>
        <p v-if="mediaError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ mediaError }}</p>
      </section>

      <!-- Attachments -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.attachments.title') }}</h2>
        <p v-if="attachments.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.attachments.empty') }}</p>
        <ul v-else class="mb-3 space-y-1.5">
          <li v-for="attachment in attachments" :key="attachment.id" class="flex items-center justify-between rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
            <span class="text-steel-800 dark:text-steel-50">{{ attachment.originalName }}</span>
            <button type="button" class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="onDownloadAttachment(attachment)">
              {{ t('dailyReports.attachments.downloadButton') }}
            </button>
          </li>
        </ul>
        <form class="flex flex-wrap items-end gap-2" @submit.prevent="onUploadAttachment">
          <input type="file" required @change="onAttachmentFileChange" class="text-sm text-steel-600 dark:text-steel-300" />
          <button type="submit" class="btn-primary px-3 py-1.5">{{ t('dailyReports.attachments.uploadButton') }}</button>
        </form>
        <p v-if="attachmentError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ attachmentError }}</p>
      </section>

      <!-- Signatures -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.signatures.title') }}</h2>
        <p v-if="signatures.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.signatures.empty') }}</p>
        <ul v-else class="mb-3 space-y-1.5">
          <li v-for="signature in signatures" :key="signature.id" class="flex items-center justify-between rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
            <span class="text-steel-800 dark:text-steel-50">{{ signature.function ? t(`siteTeam.function.${signature.function}`) : '—' }}</span>
            <span class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.signatures.signedAt') }}: {{ signature.signedAt }}</span>
          </li>
        </ul>
        <button
          v-if="!isApproved"
          type="button"
          disabled
          class="rounded-md bg-steel-300 px-4 py-2 text-sm font-medium text-white dark:bg-steel-600"
        >
          {{ t('dailyReports.signatures.signButton') }}
        </button>
        <button
          v-else
          type="button"
          :disabled="signing"
          class="btn-primary"
          @click="onSign"
        >
          {{ t('dailyReports.signatures.signButton') }}
        </button>
        <p v-if="!isApproved" class="mt-1 text-xs text-steel-500 dark:text-steel-400">{{ t('dailyReports.signatures.signRequiresSubmit') }}</p>
        <p v-if="signError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ signError }}</p>
      </section>
    </main>

    <p v-else-if="loadError" class="app-container py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
    </div>
  </div>
</template>
