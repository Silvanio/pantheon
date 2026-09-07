<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies, type Plan, type PlanCode } from '../composables/useCompanies'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import AuthShell from '../components/AuthShell.vue'

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
  <AuthShell
    heading="Escolha o plano ideal para sua empresa."
    description="Sem cobrança agora — você pode mudar de plano quando quiser."
    card-class="max-w-3xl"
  >
    <h1 class="mb-1 text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.plan.title') }}</h1>
    <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">{{ t('company.plan.subtitle') }}</p>

    <div v-if="!loading" class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      <button
        v-for="plan in plans"
        :key="plan.id"
        type="button"
        class="relative rounded-xl border-2 p-5 text-left transition"
        :class="
          selectedPlanCode === plan.code
            ? 'border-blueprint-500 bg-blueprint-50 shadow-md dark:bg-blueprint-900/30'
            : 'border-steel-200 hover:border-steel-300 hover:shadow-sm dark:border-steel-700 dark:hover:border-steel-600'
        "
        @click="selectedPlanCode = plan.code"
      >
        <span
          v-if="selectedPlanCode === plan.code"
          class="absolute right-3 top-3 flex h-5 w-5 items-center justify-center rounded-full bg-blueprint-600 text-white"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" class="h-3 w-3">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
          </svg>
        </span>
        <div class="font-semibold text-steel-800 dark:text-steel-50">{{ plan.name }}</div>
        <div class="mt-1.5 text-sm text-steel-500 dark:text-steel-400">
          {{ plan.activeSiteLimit ? t('company.plan.limitedSites', { count: plan.activeSiteLimit }) : t('company.plan.unlimitedSites') }}
        </div>
      </button>
    </div>

    <p v-if="errorMessage" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <button type="button" :disabled="!selectedPlanCode || submitting" class="btn-primary w-full py-2.5" @click="onConfirm">
      {{ t('company.plan.confirm') }}
    </button>
  </AuthShell>
</template>
