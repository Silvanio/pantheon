<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies, type Plan, type PlanCode } from '../composables/useCompanies'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { listPlans, selectPlan } = useCompanies()
const { invalidate } = useCompanyOnboarding()

const companyId = route.params.companyId as string
const plans = ref<Plan[]>([])
const selectedPlanCode = ref<PlanCode | null>(null)
const errorMessage = ref('')
const submitting = ref(false)
const loading = ref(true)

onMounted(async () => {
  try {
    plans.value = await listPlans()
  } finally {
    loading.value = false
  }
})

async function onConfirm() {
  if (!selectedPlanCode.value) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await selectPlan(companyId, selectedPlanCode.value)
    invalidate()
    router.push(`/companies/${companyId}/profile`)
  } catch {
    errorMessage.value = t('company.plan.error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 px-4 py-8 dark:bg-steel-900">
    <div class="w-full max-w-2xl rounded-xl border border-steel-200 bg-white p-8 shadow-sm dark:border-steel-700 dark:bg-steel-800">
      <div class="mb-6 flex items-center gap-2">
        <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
        <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
      </div>

      <h1 class="mb-1 text-xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.plan.title') }}</h1>
      <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">{{ t('company.plan.subtitle') }}</p>

      <div v-if="!loading" class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <button
          v-for="plan in plans"
          :key="plan.id"
          type="button"
          class="rounded-lg border p-4 text-left transition"
          :class="
            selectedPlanCode === plan.code
              ? 'border-blueprint-500 bg-blueprint-50 dark:bg-blueprint-900/30'
              : 'border-steel-200 hover:bg-steel-50 dark:border-steel-600 dark:hover:bg-steel-700'
          "
          @click="selectedPlanCode = plan.code"
        >
          <div class="font-semibold text-steel-800 dark:text-steel-50">{{ plan.name }}</div>
          <div class="mt-1 text-sm text-steel-500 dark:text-steel-400">
            {{ plan.activeSiteLimit ? t('company.plan.limitedSites', { count: plan.activeSiteLimit }) : t('company.plan.unlimitedSites') }}
          </div>
        </button>
      </div>

      <p v-if="errorMessage" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

      <button
        type="button"
        :disabled="!selectedPlanCode || submitting"
        class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        @click="onConfirm"
      >
        {{ t('company.plan.confirm') }}
      </button>
    </div>
  </div>
</template>
