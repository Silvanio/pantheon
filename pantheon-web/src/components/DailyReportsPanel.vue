<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useDailyReports, type DailyReport } from '../composables/useDailyReports'
import { vDatePicker } from '../lib/datePicker'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const router = useRouter()
const { listReports, createReport, deleteReport } = useDailyReports()

const PAGE_SIZE_OPTIONS = [1, 5, 10] as const

const reports = ref<DailyReport[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const page = ref(0)
const pageSize = ref<number>(10)
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const reportDate = ref('')

const deletingId = ref<string | null>(null)
const confirmingDeleteId = ref<string | null>(null)
const deleteError = ref('')

const dateFormatter = new Intl.DateTimeFormat('pt-BR')
function formatDate(value: string): string {
  return dateFormatter.format(new Date(`${value}T00:00:00`))
}

async function load() {
  loading.value = true
  try {
    const result = await listReports(props.siteId, { page: page.value, size: pageSize.value })
    reports.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } finally {
    loading.value = false
  }
}

function goToPage(target: number) {
  if (target < 0 || target >= totalPages.value) return
  page.value = target
  load()
}

function onPageSizeChange() {
  page.value = 0
  load()
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createReport(props.siteId, reportDate.value)
    reportDate.value = ''
    showForm.value = false
    page.value = 0
    await load()
  } catch {
    errorMessage.value = t('dailyReports.history.form.error')
  } finally {
    submitting.value = false
  }
}

async function onDelete(reportId: string) {
  deleteError.value = ''
  deletingId.value = reportId
  try {
    await deleteReport(reportId)
    await load()
  } catch {
    deleteError.value = t('dailyReports.history.deleteError')
  } finally {
    deletingId.value = null
    confirmingDeleteId.value = null
  }
}

onMounted(load)
</script>

<template>
  <section class="space-y-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary" @click="showForm = !showForm">
        {{ t('dailyReports.history.newButton') }}
      </button>
    </div>

    <div v-if="showForm" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="showForm = false">
      <form class="modal-panel card-pad w-full max-w-md space-y-3" @submit.prevent="onSubmit">
        <h3 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.newButton') }}</h3>
        <div>
          <label class="field-label">{{ t('dailyReports.history.form.date') }}</label>
          <input v-model="reportDate" v-date-picker type="date" required class="field-input" />
        </div>
        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="btn-primary">
            {{ t('dailyReports.history.form.submit') }}
          </button>
          <button type="button" class="btn-secondary" @click="showForm = false">
            {{ t('dailyReports.history.form.cancel') }}
          </button>
        </div>
      </form>
    </div>

    <p v-if="!loading && reports.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('dailyReports.history.empty') }}
    </p>
    <div v-else class="overflow-hidden rounded-2xl border border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-900">
      <table class="w-full border-collapse">
        <thead>
          <tr>
            <th class="pb-3 pl-5 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.table.report') }}</th>
            <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.table.status') }}</th>
            <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.table.weather') }}</th>
            <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.table.date') }}</th>
            <th class="pb-3 pr-5 pt-4"></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="report in reports"
            :key="report.id"
            class="group cursor-pointer border-t border-steel-100 transition hover:bg-steel-50 dark:border-steel-800 dark:hover:bg-steel-800/60"
            @click="router.push(`/daily-reports/${report.id}`)"
          >
            <td class="py-3.5 pl-5 text-[13.5px] font-bold text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.reportLabel') }} #{{ report.sequenceNo }}</td>
            <td class="py-3.5 text-[13px] text-steel-600 dark:text-steel-300">{{ t(`dailyReports.status.${report.status}`) }}</td>
            <td class="py-3.5 text-[13px] text-steel-600 dark:text-steel-300">{{ report.weatherCondition ?? '—' }}</td>
            <td class="py-3.5 text-[13px] text-steel-500 dark:text-steel-400">{{ formatDate(report.reportDate) }}</td>
            <td class="py-3.5 pr-5 text-right" @click.stop>
              <button
                v-if="report.status === 'DRAFT'"
                type="button"
                :disabled="deletingId === report.id"
                :title="t('dailyReports.history.deleteButton')"
                class="inline-flex h-7 w-7 items-center justify-center rounded-md text-safety-600 transition hover:bg-safety-50 dark:text-safety-500 dark:hover:bg-safety-900/30"
                @click="confirmingDeleteId = report.id"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2m3 0-1 14a2 2 0 01-2 2H7a2 2 0 01-2-2L4 6h16zM10 11v6M14 11v6" />
                </svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="!loading && reports.length > 0" class="flex items-center justify-between gap-3 border-t border-steel-200 pt-4 dark:border-steel-700">
      <div class="flex items-center gap-2">
        <label class="text-xs text-steel-500 dark:text-steel-400">{{ t('common.pagination.pageSizeLabel') }}</label>
        <select v-model.number="pageSize" class="field-input w-auto py-1 text-xs" @change="onPageSizeChange">
          <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">{{ size }}</option>
        </select>
      </div>
      <div v-if="totalPages > 1" class="flex items-center gap-3">
        <p class="text-xs text-steel-500 dark:text-steel-400">
          {{ t('dailyReports.history.pagination.summary', { page: page + 1, totalPages, totalElements }) }}
        </p>
        <div class="flex gap-2">
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page === 0" @click="goToPage(page - 1)">
            {{ t('dailyReports.history.pagination.previous') }}
          </button>
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
            {{ t('dailyReports.history.pagination.next') }}
          </button>
        </div>
      </div>
    </div>

    <p v-if="deleteError" class="text-sm text-safety-600 dark:text-safety-500">{{ deleteError }}</p>

    <div
      v-if="confirmingDeleteId"
      class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4"
      @click.self="confirmingDeleteId = null"
    >
      <div class="modal-panel card-pad w-full max-w-sm">
        <p class="mb-4 text-sm text-steel-600 dark:text-steel-300">{{ t('dailyReports.history.deleteConfirm') }}</p>
        <div class="flex justify-end gap-2">
          <button type="button" class="btn-secondary" @click="confirmingDeleteId = null">
            {{ t('dailyReports.history.form.cancel') }}
          </button>
          <button
            type="button"
            :disabled="deletingId === confirmingDeleteId"
            class="btn-danger"
            @click="confirmingDeleteId && onDelete(confirmingDeleteId)"
          >
            {{ t('dailyReports.history.deleteButton') }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
