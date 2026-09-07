<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies } from '../composables/useCompanies'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import AuthShell from '../components/AuthShell.vue'

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
  <AuthShell heading="Sua empresa, organizada por obra." description="Crie sua empresa para começar a cadastrar obras, equipe e materiais.">
    <h1 class="mb-1 text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.creation.title') }}</h1>
    <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">{{ t('company.creation.subtitle') }}</p>

    <form class="space-y-4" @submit.prevent="onSubmit">
      <div>
        <label class="field-label" for="companyName">{{ t('company.creation.name') }}</label>
        <input id="companyName" v-model="companyName" type="text" required class="field-input" />
      </div>

      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

      <button type="submit" :disabled="submitting" class="btn-primary w-full py-2.5">
        {{ t('company.creation.submit') }}
      </button>
    </form>
  </AuthShell>
</template>
