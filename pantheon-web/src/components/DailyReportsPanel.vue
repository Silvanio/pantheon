<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDailyReports, type DailyReport } from '../composables/useDailyReports'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listReports, createReport } = useDailyReports()

const reports = ref<DailyReport[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const reportDate = ref('')

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
  <section class="card card-pad">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary" @click="showForm = !showForm">
        {{ t('dailyReports.history.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-5 space-y-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="field-label">{{ t('dailyReports.history.form.date') }}</label>
        <input v-model="reportDate" type="date" required class="field-input" />
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
    <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <li v-for="report in reports" :key="report.id">
        <router-link :to="{ path: `/daily-reports/${report.id}` }" class="flex items-center justify-between rounded-lg border border-steel-200 px-4 py-3 text-sm transition hover:bg-steel-50 dark:border-steel-700 dark:hover:bg-steel-700">
          <span class="font-medium text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.reportLabel') }} #{{ report.sequenceNo }} — {{ report.reportDate }}</span>
          <span class="text-steel-500 dark:text-steel-400">{{ t(`dailyReports.status.${report.status}`) }}</span>
        </router-link>
      </li>
    </ul>
  </section>
</template>
