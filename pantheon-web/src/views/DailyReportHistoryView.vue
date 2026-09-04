<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useDailyReports, type DailyReport } from '../composables/useDailyReports'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { listReports, createReport } = useDailyReports()

const siteId = route.params.siteId as string
const projectId = route.query.projectId as string | undefined
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
    router.push({ path: `/daily-reports/${report.id}`, query: { projectId } })
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
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
        <button type="button" class="text-sm text-blueprint-600 dark:text-blueprint-400" @click="router.push('/')">
          ← {{ t('dailyReports.history.back') }}
        </button>
      </div>
    </header>
    <main class="mx-auto max-w-4xl space-y-6 px-6 py-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.title') }}</h1>
          <p class="mt-1 text-steel-500 dark:text-steel-400">{{ t('dailyReports.history.subtitle') }}</p>
        </div>
        <button
          type="button"
          class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
          @click="showForm = !showForm"
        >
          {{ t('dailyReports.history.newButton') }}
        </button>
      </div>

      <form
        v-if="showForm"
        class="space-y-3 rounded-xl border border-steel-200 bg-white p-6 dark:border-steel-700 dark:bg-steel-800"
        @submit.prevent="onSubmit"
      >
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('dailyReports.history.form.date') }}</label>
          <input v-model="reportDate" type="date" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
            {{ t('dailyReports.history.form.submit') }}
          </button>
          <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">
            {{ t('dailyReports.history.form.cancel') }}
          </button>
        </div>
      </form>

      <p v-if="!loading && reports.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
        {{ t('dailyReports.history.empty') }}
      </p>
      <ul v-else class="space-y-2">
        <li v-for="report in reports" :key="report.id">
          <router-link
            :to="{ path: `/daily-reports/${report.id}`, query: { projectId } }"
            class="flex items-center justify-between rounded-md border border-steel-200 bg-white px-4 py-3 transition hover:bg-steel-50 dark:border-steel-700 dark:bg-steel-800 dark:hover:bg-steel-700"
          >
            <span class="font-medium text-steel-800 dark:text-steel-50">{{ t('dailyReports.history.reportLabel') }} #{{ report.sequenceNo }} — {{ report.reportDate }}</span>
            <span class="text-sm text-steel-500 dark:text-steel-400">{{ t(`dailyReports.status.${report.status}`) }}</span>
          </router-link>
        </li>
      </ul>
    </main>
  </div>
</template>
