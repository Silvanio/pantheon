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
import { useSiteMembers, type SiteMember } from '../composables/useSiteMembers'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const {
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
} = useDailyReports()
const { listEquipment } = useEquipment()
const { listMembers } = useSiteMembers()

const reportId = route.params.id as string
const detail = ref<DailyReportDetail | null>(null)
const equipmentCatalog = ref<Equipment[]>([])
const siteMembers = ref<SiteMember[]>([])
const loading = ref(false)
const loadError = ref('')

const isDraft = computed(() => detail.value?.report.status === 'DRAFT')

// core section
const coreErrorMessage = ref('')
const coreSaving = ref(false)
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

    const siteId = detail.value.report.constructionSiteId
    equipmentCatalog.value = await listEquipment(siteId)
    siteMembers.value = await listMembers(siteId)

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
  } catch {
    coreErrorMessage.value = t('dailyReports.core.error')
  } finally {
    coreSaving.value = false
  }
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
  if (!window.confirm(t('dailyReports.detail.submitConfirm'))) return
  submitError.value = ''
  submitting.value = true
  try {
    const updated = await submitReport(reportId)
    if (detail.value) detail.value.report = updated
  } catch {
    submitError.value = t('dailyReports.detail.submitError')
  } finally {
    submitting.value = false
  }
}

function equipmentName(equipmentId: string): string {
  return equipmentCatalog.value.find((e) => e.id === equipmentId)?.name ?? equipmentId
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.back()">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('dailyReports.history.back') }}
        </button>
      </template>
    </AppHeader>

    <main v-if="detail" class="app-container max-w-5xl! space-y-6 py-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">
            {{ t('dailyReports.history.reportLabel') }} #{{ detail.report.sequenceNo }} — {{ detail.report.reportDate }}
          </h1>
          <p class="mt-1 text-sm text-steel-500 dark:text-steel-400">{{ t(`dailyReports.status.${detail.report.status}`) }}</p>
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
            v-if="isDraft"
            type="button"
            :disabled="submitting"
            class="btn-danger"
            @click="onSubmitReport"
          >
            {{ t('dailyReports.detail.submitButton') }}
          </button>
        </div>
      </div>
      <p v-if="!isDraft" class="rounded-md bg-blueprint-50 px-4 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
        {{ t('dailyReports.detail.submittedNotice') }}
      </p>
      <p v-if="submitError" class="text-sm text-safety-600 dark:text-safety-500">{{ submitError }}</p>
      <p v-if="pdfError" class="text-sm text-safety-600 dark:text-safety-500">{{ pdfError }}</p>

      <!-- Core: weather, hours, comments -->
      <section class="card card-pad">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.core.title') }}</h2>
        <template v-if="isDraft">
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.weatherCondition') }}</label>
              <input v-model="weatherCondition" type="text" :placeholder="t('dailyReports.core.weatherConditionPlaceholder')" class="field-input" />
            </div>
            <div class="flex items-end">
              <label class="flex items-center gap-2 text-sm text-steel-600 dark:text-steel-300">
                <input v-model="weatherBlockedTasks" type="checkbox" />
                {{ t('dailyReports.core.weatherBlockedTasks') }}
              </label>
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.workHoursStart') }}</label>
              <input v-model="workHoursStart" type="time" class="field-input" />
            </div>
            <div>
              <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.workHoursEnd') }}</label>
              <input v-model="workHoursEnd" type="time" class="field-input" />
            </div>
          </div>
          <div class="mt-3">
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.core.comments') }}</label>
            <textarea v-model="comments" rows="3" class="field-input"></textarea>
          </div>
          <p v-if="coreErrorMessage" class="mt-2 text-sm text-safety-600 dark:text-safety-500">{{ coreErrorMessage }}</p>
          <button type="button" :disabled="coreSaving" class="btn-primary mt-3" @click="onSaveCore">
            {{ t('dailyReports.core.save') }}
          </button>
        </template>
        <dl v-else class="grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
          <div><dt class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.core.weatherCondition') }}</dt><dd class="text-steel-800 dark:text-steel-50">{{ detail.report.weatherCondition ?? '—' }}</dd></div>
          <div><dt class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.core.workHoursStart') }} / {{ t('dailyReports.core.workHoursEnd') }}</dt><dd class="text-steel-800 dark:text-steel-50">{{ detail.report.workHoursStart ?? '—' }} - {{ detail.report.workHoursEnd ?? '—' }}</dd></div>
          <div class="sm:col-span-2"><dt class="text-steel-500 dark:text-steel-400">{{ t('dailyReports.core.comments') }}</dt><dd class="text-steel-800 dark:text-steel-50">{{ detail.report.comments ?? '—' }}</dd></div>
        </dl>
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
          v-if="isDraft"
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
        <p v-if="isDraft" class="mt-1 text-xs text-steel-500 dark:text-steel-400">{{ t('dailyReports.signatures.signRequiresSubmit') }}</p>
        <p v-if="signError" class="mt-1 text-sm text-safety-600 dark:text-safety-500">{{ signError }}</p>
      </section>
    </main>

    <p v-else-if="loadError" class="app-container max-w-5xl! py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
  </div>
</template>
