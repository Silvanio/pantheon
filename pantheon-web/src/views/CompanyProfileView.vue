<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies } from '../composables/useCompanies'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { completeProfile } = useCompanies()
const { invalidate } = useCompanyOnboarding()

const companyId = route.params.companyId as string

const legalName = ref('')
const tradeName = ref('')
const cnpj = ref('')
const address = ref('')
const logo = ref<File | null>(null)
const errorMessage = ref('')
const submitting = ref(false)

function onLogoChange(event: Event) {
  logo.value = (event.target as HTMLInputElement).files?.[0] ?? null
}

async function onSubmit() {
  if (!logo.value) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await completeProfile(companyId, {
      legalName: legalName.value,
      tradeName: tradeName.value,
      cnpj: cnpj.value,
      address: address.value,
      logo: logo.value,
    })
    invalidate()
    router.push('/')
  } catch {
    errorMessage.value = t('company.profile.error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-steel-50 px-4 py-8 dark:bg-steel-900">
    <div class="w-full max-w-md rounded-xl border border-steel-200 bg-white p-8 shadow-sm dark:border-steel-700 dark:bg-steel-800">
      <div class="mb-6 flex items-center gap-2">
        <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
        <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
      </div>

      <h1 class="mb-1 text-xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.profile.title') }}</h1>
      <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">{{ t('company.profile.subtitle') }}</p>

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
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.profile.logo') }}</label>
          <input type="file" accept="image/*" required class="w-full text-sm text-steel-600 dark:text-steel-300" @change="onLogoChange" />
        </div>

        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        >
          {{ t('company.profile.submit') }}
        </button>
      </form>
    </div>
  </div>
</template>
