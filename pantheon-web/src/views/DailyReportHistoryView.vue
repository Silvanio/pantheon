<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDailyReports, type DailyReport } from '../composables/useDailyReports'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { listReports, createReport } = useDailyReports()

const siteId = route.params.siteId as string
const reports = ref<DailyReport[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const reportDate = ref('')

async function load() {
  loading.value = true
  try {
    reports.value = await listReports(siteId)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    const report = await createReport(siteId, reportDate.value)
    router.push({ path: `/daily-reports/${report.id}` })
  } catch {
    errorMessage.value = t('dailyReports.history.form.error')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.push(`/sites/${siteId}`)">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('dailyReports.history.back') }}
        </button>
      </template>
    </AppHeader>
    <main class="app-container max-w-5xl! space-y-6 py-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-3xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.title') }}</h1>
          <p class="mt-1.5 text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.subtitle') }}</p>
        </div>
        <button type="button" class="btn-primary" @click="showForm = !showForm">
          {{ t('dailyReports.history.newButton') }}
        </button>
      </div>

      <form v-if="showForm" class="card card-pad space-y-3" @submit.prevent="onSubmit">
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
      <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <li v-for="report in reports" :key="report.id">
          <router-link :to="{ path: `/daily-reports/${report.id}` }" class="card flex items-center justify-between px-4 py-3.5 transition hover:shadow-md">
            <span class="font-medium text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.reportLabel') }} #{{ report.sequenceNo }} — {{ report.reportDate }}</span>
            <span class="text-sm text-steel-500 dark:text-steel-400">{{ t(`dailyReports.status.${report.status}`) }}</span>
          </router-link>
        </li>
      </ul>
    </main>
  </div>
</template>
