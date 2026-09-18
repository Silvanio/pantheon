<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useDailyReports, type DailyReport } from '../composables/useDailyReports'
import { vDatePicker } from '../lib/datePicker'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const router = useRouter()
const { listReports, createReport } = useDailyReports()

const reports = ref<DailyReport[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const reportDate = ref('')

const dateFormatter = new Intl.DateTimeFormat('pt-BR')
function formatDate(value: string): string {
  return dateFormatter.format(new Date(`${value}T00:00:00`))
}

async function load() {
  loading.value = true
  try {
    reports.value = await listReports(props.siteId)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createReport(props.siteId, reportDate.value)
    reportDate.value = ''
    showForm.value = false
    await load()
  } catch {
    errorMessage.value = t('dailyReports.history.form.error')
  } finally {
    submitting.value = false
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

    <form v-if="showForm" class="space-y-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
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
            <th class="pb-3 pr-5 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.table.date') }}</th>
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
            <td class="py-3.5 pr-5 text-[13px] text-steel-500 dark:text-steel-400">{{ formatDate(report.reportDate) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
