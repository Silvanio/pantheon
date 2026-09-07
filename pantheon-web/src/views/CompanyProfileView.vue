<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies } from '../composables/useCompanies'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import AuthShell from '../components/AuthShell.vue'

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
  <AuthShell heading="Últimos detalhes." description="Esses dados aparecem para clientes e prestadores convidados para suas obras.">
    <h1 class="mb-1 text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('company.profile.title') }}</h1>
    <p class="mb-6 text-sm text-steel-500 dark:text-steel-400">{{ t('company.profile.subtitle') }}</p>

    <form class="space-y-4" @submit.prevent="onSubmit">
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
      <div>
        <label class="field-label">{{ t('company.profile.logo') }}</label>
        <input type="file" accept="image/*" required class="w-full text-sm text-steel-600 dark:text-steel-300" @change="onLogoChange" />
      </div>

      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

      <button type="submit" :disabled="submitting" class="btn-primary w-full py-2.5">
        {{ t('company.profile.submit') }}
      </button>
    </form>
  </AuthShell>
</template>
