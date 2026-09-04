<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies } from '../composables/useCompanies'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'

const router = useRouter()
const { t } = useI18n()
const { createCompany } = useCompanies()
const { invalidate } = useCompanyOnboarding()

const companyName = ref('')
const errorMessage = ref('')
const submitting = ref(false)

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    const company = await createCompany(companyName.value)
    invalidate()
    router.push(`/companies/${company.id}/plan`)
  } catch {
    errorMessage.value = t('company.creation.error')
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

      <h1 class="mb-1 text-xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.creation.title') }}</h1>
      <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">{{ t('company.creation.subtitle') }}</p>

      <form class="space-y-4" @submit.prevent="onSubmit">
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300" for="companyName">
            {{ t('company.creation.name') }}
          </label>
          <input
            id="companyName"
            v-model="companyName"
            type="text"
            required
            class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
          />
        </div>

        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

        <button
          type="submit"
          :disabled="submitting"
          class="w-full rounded-md bg-blueprint-600 px-4 py-2 font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        >
          {{ t('company.creation.submit') }}
        </button>
      </form>
    </div>
  </div>
</template>
