<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies } from '../composables/useCompanies'
import CompanyStaffPanel from '../components/CompanyStaffPanel.vue'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getCompany, completeProfile } = useCompanies()

const companyId = route.params.companyId as string

const legalName = ref('')
const tradeName = ref('')
const cnpj = ref('')
const address = ref('')
const logo = ref<File | null>(null)
const errorMessage = ref('')
const successMessage = ref('')
const submitting = ref(false)

function onLogoChange(event: Event) {
  logo.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function onSubmit() {
  errorMessage.value = ''
  successMessage.value = ''
  submitting.value = true
  try {
    await completeProfile(companyId, {
      legalName: legalName.value,
      tradeName: tradeName.value,
      cnpj: cnpj.value,
      address: address.value,
      logo: logo.value,
    })
    successMessage.value = t('company.settings.saved')
  } catch {
    errorMessage.value = t('company.profile.error')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const company = await getCompany(companyId)
  legalName.value = company.legalName ?? ''
  tradeName.value = company.tradeName ?? ''
  cnpj.value = company.cnpj ?? ''
  address.value = company.address ?? ''
})
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.push('/')">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('company.settings.back') }}
        </button>
      </template>
    </AppHeader>

    <main class="app-container max-w-4xl! space-y-6 py-8">
      <h1 class="text-3xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('company.settings.title') }}</h1>

      <section class="card card-pad">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.profile.title') }}</h2>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label class="field-label">{{ t('company.profile.legalName') }}</label>
              <input v-model="legalName" type="text" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('company.profile.tradeName') }}</label>
              <input v-model="tradeName" type="text" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('company.profile.cnpj') }}</label>
              <input v-model="cnpj" type="text" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('company.profile.address') }}</label>
              <input v-model="address" type="text" required class="field-input" />
            </div>
          </div>
          <div>
            <label class="field-label">{{ t('company.settings.newLogo') }}</label>
            <input type="file" accept="image/*" class="w-full text-sm text-steel-600 dark:text-steel-300" @change="onLogoChange" />
          </div>
          <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
          <p v-if="successMessage" class="text-sm text-blueprint-600 dark:text-blueprint-400">{{ successMessage }}</p>
          <button type="submit" :disabled="submitting" class="btn-primary">
            {{ t('company.profile.submit') }}
          </button>
        </form>
      </section>

      <section class="card card-pad">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.plan.title') }}</h2>
          <router-link :to="`/companies/${companyId}/plan`" class="btn-secondary py-1.5">
            {{ t('company.settings.changePlan') }}
          </router-link>
        </div>
      </section>

      <CompanyStaffPanel :company-id="companyId" />
    </main>
  </div>
</template>
