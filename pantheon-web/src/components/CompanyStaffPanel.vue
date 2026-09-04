<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCompanies, type CompanyMember } from '../composables/useCompanies'
import { HttpError } from '../composables/useAuth'

const props = defineProps<{ companyId: string }>()

const { t } = useI18n()
const { listStaff, addStaffMember } = useCompanies()

const members = ref<CompanyMember[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const noticeMessage = ref('')
const email = ref('')

async function loadMembers() {
  loading.value = true
  try {
    members.value = await listStaff(props.companyId)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  noticeMessage.value = ''
  submitting.value = true
  try {
    const invitedEmail = email.value
    await addStaffMember(props.companyId, invitedEmail)
    email.value = ''
    showForm.value = false
    noticeMessage.value = t('company.staff.form.invited', { email: invitedEmail })
    await loadMembers()
  } catch (error) {
    errorMessage.value =
      error instanceof HttpError && error.status === 409
        ? t('company.staff.form.alreadyMember')
        : t('company.staff.form.error')
  } finally {
    submitting.value = false
  }
}

onMounted(loadMembers)
</script>

<template>
  <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.staff.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('company.staff.subtitle') }}</p>
      </div>
      <button
        type="button"
        class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        @click="showForm = !showForm"
      >
        {{ t('company.staff.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-4 space-y-3 rounded-md border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('company.staff.form.email') }}</label>
        <input v-model="email" type="email" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      </div>
      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
          {{ t('company.staff.form.submit') }}
        </button>
        <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">
          {{ t('company.staff.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="noticeMessage" class="mb-4 rounded-md bg-blueprint-50 px-3 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
      {{ noticeMessage }}
    </p>

    <p v-if="!loading && members.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('company.staff.empty') }}</p>
    <ul v-else class="space-y-2">
      <li v-for="member in members" :key="member.membershipId" class="flex items-center justify-between rounded-md border border-steel-200 px-4 py-3 dark:border-steel-700">
        <div>
          <p class="flex items-center gap-2 font-medium text-steel-800 dark:text-steel-50">
            {{ member.displayName }}
            <span v-if="member.invited" class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-900 dark:text-amber-200">
              {{ t('members.badge.invited') }}
            </span>
          </p>
          <p class="text-sm text-steel-500 dark:text-steel-400">{{ member.email }}</p>
        </div>
        <span class="text-sm text-steel-700 dark:text-steel-200">{{ member.role }}</span>
      </li>
    </ul>
  </section>
</template>
