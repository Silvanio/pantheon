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
  <section class="card card-pad">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.staff.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('company.staff.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary" @click="showForm = !showForm">
        {{ t('company.staff.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-5 space-y-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="field-label">{{ t('company.staff.form.email') }}</label>
        <input v-model="email" type="email" required class="field-input" />
      </div>
      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="btn-primary">
          {{ t('company.staff.form.submit') }}
        </button>
        <button type="button" class="btn-secondary" @click="showForm = false">
          {{ t('company.staff.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="noticeMessage" class="mb-4 rounded-lg bg-blueprint-50 px-3 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
      {{ noticeMessage }}
    </p>

    <p v-if="!loading && members.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('company.staff.empty') }}</p>
    <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <li v-for="member in members" :key="member.membershipId" class="flex items-center justify-between rounded-lg border border-steel-200 px-4 py-3 dark:border-steel-700">
        <div class="min-w-0">
          <p class="flex items-center gap-2 truncate font-medium text-steel-800 dark:text-steel-50">
            <span class="truncate">{{ member.displayName }}</span>
            <span v-if="member.invited" class="badge-amber shrink-0">{{ t('members.badge.invited') }}</span>
          </p>
          <p class="truncate text-sm text-steel-500 dark:text-steel-400">{{ member.email }}</p>
        </div>
        <span class="shrink-0 pl-3 text-sm text-steel-700 dark:text-steel-200">{{ member.role }}</span>
      </li>
    </ul>
  </section>
</template>
