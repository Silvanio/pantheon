<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useSiteMembers, type ConstructionFunction, type SiteMember } from '../composables/useSiteMembers'
import { HttpError } from '../composables/useAuth'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listMembers, addMember } = useSiteMembers()

const members = ref<SiteMember[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const noticeMessage = ref('')

const functions: ConstructionFunction[] = ['CLIENT', 'ARCHITECT', 'ENGINEER', 'SITE_FOREMAN', 'SERVICE_PROVIDER']
const memberFunction = ref<ConstructionFunction>('CLIENT')
const email = ref('')
const cpf = ref('')
const trade = ref('')
const displayName = ref('')
const contactEmail = ref('')

const isServiceProvider = computed(() => memberFunction.value === 'SERVICE_PROVIDER')
const isClient = computed(() => memberFunction.value === 'CLIENT')

async function loadMembers() {
  loading.value = true
  try {
    members.value = await listMembers(props.siteId)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  email.value = ''
  cpf.value = ''
  trade.value = ''
  displayName.value = ''
  contactEmail.value = ''
}

async function onSubmit() {
  errorMessage.value = ''
  noticeMessage.value = ''
  submitting.value = true
  try {
    if (isServiceProvider.value && !email.value) {
      await addMember(props.siteId, {
        function: memberFunction.value,
        trade: trade.value || null,
        displayName: displayName.value || null,
        contactEmail: contactEmail.value || null,
      })
      noticeMessage.value = t('siteTeam.form.added')
    } else {
      await addMember(props.siteId, {
        function: memberFunction.value,
        email: email.value,
        cpf: isClient.value ? cpf.value || null : null,
      })
      noticeMessage.value = t('siteTeam.form.invited', { email: email.value })
    }
    resetForm()
    showForm.value = false
    await loadMembers()
  } catch (error) {
    errorMessage.value =
      error instanceof HttpError && error.status === 409 ? t('siteTeam.form.alreadyMember') : t('siteTeam.form.error')
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
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('siteTeam.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteTeam.subtitle') }}</p>
      </div>
      <button
        type="button"
        class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        @click="showForm = !showForm"
      >
        {{ t('siteTeam.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-4 space-y-3 rounded-md border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteTeam.function.label') }}</label>
        <select v-model="memberFunction" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50">
          <option v-for="fn in functions" :key="fn" :value="fn">{{ t(`siteTeam.function.${fn}`) }}</option>
        </select>
      </div>

      <template v-if="isServiceProvider">
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteTeam.form.displayName') }}</label>
          <input v-model="displayName" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteTeam.form.trade') }}</label>
          <input v-model="trade" type="text" :placeholder="t('siteTeam.form.tradePlaceholder')" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteTeam.form.contactEmail') }}</label>
          <input v-model="contactEmail" type="email" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
      </template>
      <template v-else>
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteTeam.form.email') }}</label>
          <input v-model="email" type="email" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <div v-if="isClient">
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteTeam.form.cpf') }}</label>
          <input v-model="cpf" type="text" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
      </template>

      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
          {{ t('siteTeam.form.submit') }}
        </button>
        <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">
          {{ t('siteTeam.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="noticeMessage" class="mb-4 rounded-md bg-blueprint-50 px-3 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
      {{ noticeMessage }}
    </p>

    <p v-if="!loading && members.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteTeam.empty') }}</p>
    <ul v-else class="space-y-2">
      <li v-for="member in members" :key="member.membershipId" class="flex items-center justify-between rounded-md border border-steel-200 px-4 py-3 dark:border-steel-700">
        <div>
          <p class="flex items-center gap-2 font-medium text-steel-800 dark:text-steel-50">
            {{ member.displayName ?? member.email }}
            <span v-if="member.invited" class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800 dark:bg-amber-900 dark:text-amber-200">
              {{ t('members.badge.invited') }}
            </span>
          </p>
          <p v-if="member.email" class="text-sm text-steel-500 dark:text-steel-400">{{ member.email }}</p>
        </div>
        <div class="text-right text-sm">
          <p class="text-steel-700 dark:text-steel-200">{{ t(`siteTeam.function.${member.function}`) }}</p>
          <p v-if="member.trade" class="text-steel-500 dark:text-steel-400">{{ member.trade }}</p>
        </div>
      </li>
    </ul>
  </section>
</template>
