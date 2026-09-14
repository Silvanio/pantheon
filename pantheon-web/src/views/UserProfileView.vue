<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserProfile } from '../composables/useUserProfile'
import AppHeader from '../components/AppHeader.vue'

const router = useRouter()
const { t } = useI18n()
const { getMyProfile, updateMyProfile } = useUserProfile()

const email = ref('')
const cnpjCpf = ref('')
const legalName = ref('')
const address = ref('')
const postalCode = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const submitting = ref(false)

async function onSubmit() {
  errorMessage.value = ''
  successMessage.value = ''
  submitting.value = true
  try {
    await updateMyProfile({
      cnpjCpf: cnpjCpf.value,
      legalName: legalName.value,
      address: address.value,
      postalCode: postalCode.value,
    })
    successMessage.value = t('userProfile.saved')
  } catch {
    errorMessage.value = t('userProfile.error')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const profile = await getMyProfile()
  email.value = profile.email
  cnpjCpf.value = profile.cnpjCpf ?? ''
  legalName.value = profile.legalName ?? ''
  address.value = profile.address ?? ''
  postalCode.value = profile.postalCode ?? ''
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
          {{ t('userProfile.back') }}
        </button>
      </template>
    </AppHeader>

    <main class="app-container max-w-2xl! space-y-6 py-8">
      <h1 class="text-3xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('userProfile.title') }}</h1>
      <p class="text-steel-500 dark:text-steel-400">{{ t('userProfile.subtitle') }}</p>

      <section class="card card-pad">
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="sm:col-span-2">
              <label class="field-label">{{ t('userProfile.email') }}</label>
              <input :value="email" type="email" disabled class="field-input cursor-not-allowed opacity-60" />
            </div>
            <div>
              <label class="field-label">{{ t('userProfile.cnpjCpf') }}</label>
              <input v-model="cnpjCpf" type="text" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('userProfile.legalName') }}</label>
              <input v-model="legalName" type="text" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('userProfile.address') }}</label>
              <input v-model="address" type="text" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('userProfile.postalCode') }}</label>
              <input v-model="postalCode" type="text" required class="field-input" />
            </div>
          </div>
          <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
          <p v-if="successMessage" class="text-sm text-blueprint-600 dark:text-blueprint-400">{{ successMessage }}</p>
          <button type="submit" :disabled="submitting" class="btn-primary">
            {{ t('userProfile.submit') }}
          </button>
        </form>
      </section>
    </main>
  </div>
</template>
