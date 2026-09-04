<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies } from '../composables/useCompanies'
import CompanyStaffPanel from '../components/CompanyStaffPanel.vue'

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
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
        <button type="button" class="text-sm text-blueprint-600 dark:text-blueprint-400" @click="router.push('/')">
          ← {{ t('company.settings.back') }}
        </button>
      </div>
    </header>

    <main class="mx-auto max-w-4xl space-y-6 px-6 py-8">
      <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.settings.title') }}</h1>

      <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.profile.title') }}</h2>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.profile.legalName') }}</label>
            <input v-model="legalName" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.profile.tradeName') }}</label>
            <input v-model="tradeName" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.profile.cnpj') }}</label>
            <input v-model="cnpj" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.profile.address') }}</label>
            <input v-model="address" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.settings.newLogo') }}</label>
            <input type="file" accept="image/*" class="w-full text-sm text-steel-600 dark:text-steel-300" @change="onLogoChange" />
          </div>
          <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
          <p v-if="successMessage" class="text-sm text-blueprint-600 dark:text-blueprint-400">{{ successMessage }}</p>
          <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
            {{ t('company.profile.submit') }}
          </button>
        </form>
      </section>

      <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.plan.title') }}</h2>
          <router-link
            :to="`/companies/${companyId}/plan`"
            class="rounded-md border border-steel-300 px-3 py-1.5 text-sm font-medium text-steel-600 transition hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700"
          >
            {{ t('company.settings.changePlan') }}
          </router-link>
        </div>
      </section>

      <CompanyStaffPanel :company-id="companyId" />
    </main>
  </div>
</template>
