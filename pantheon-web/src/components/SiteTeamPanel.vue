<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useSiteMembers,
  type ConstructionFunction,
  type EmailConflictCheck,
  type PersonSearchResult,
  type SiteMember,
} from '../composables/useSiteMembers'
import { HttpError } from '../composables/useAuth'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listMembers, addMember, removeMember, searchPeople, checkEmailConflicts } = useSiteMembers()

const members = ref<SiteMember[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const noticeMessage = ref('')
const confirmingRemovalOf = ref<string | null>(null)

const functions: ConstructionFunction[] = ['CLIENT', 'ARCHITECT', 'ENGINEER', 'SITE_FOREMAN', 'SERVICE_PROVIDER']
const memberFunction = ref<ConstructionFunction>('CLIENT')
const email = ref('')
const cpf = ref('')
const phone = ref('')
const trade = ref('')
const displayName = ref('')
const contactEmail = ref('')

const isServiceProvider = computed(() => memberFunction.value === 'SERVICE_PROVIDER')

// Autocomplete: as the admin types a CPF or email, offer people already known to the
// company (other obras' team, company staff) so their data doesn't need retyping.
const suggestions = ref<PersonSearchResult[]>([])
const matchedExisting = ref(false)
const MIN_SEARCH_LENGTH = 3
const SEARCH_DEBOUNCE_MS = 300
let searchTimer: ReturnType<typeof setTimeout> | null = null

function scheduleSearch(query: string) {
  matchedExisting.value = false
  if (searchTimer) clearTimeout(searchTimer)
  const trimmed = query.trim()
  if (trimmed.length < MIN_SEARCH_LENGTH) {
    suggestions.value = []
    return
  }
  searchTimer = setTimeout(async () => {
    try {
      suggestions.value = await searchPeople(props.siteId, trimmed)
    } catch {
      suggestions.value = []
    }
  }, SEARCH_DEBOUNCE_MS)
}

function selectSuggestion(person: PersonSearchResult) {
  displayName.value = person.name ?? ''
  email.value = person.email ?? ''
  cpf.value = person.cpf ?? ''
  phone.value = person.phone ?? ''
  matchedExisting.value = true
  suggestions.value = []
  scheduleEmailCheck(email.value)
}

// Blocks submit when the email is already an active member of *this* obra, or already tied to
// a person with a membership in a *different* company. Reusing a person already known to this
// same company from a different obra (the whole point of the autocomplete above) stays allowed.
const emailConflict = ref<EmailConflictCheck | null>(null)
let emailCheckTimer: ReturnType<typeof setTimeout> | null = null

function scheduleEmailCheck(value: string) {
  emailConflict.value = null
  if (emailCheckTimer) clearTimeout(emailCheckTimer)
  if (!value.includes('@')) return
  emailCheckTimer = setTimeout(async () => {
    try {
      emailConflict.value = await checkEmailConflicts(props.siteId, value.trim())
    } catch {
      emailConflict.value = null
    }
  }, SEARCH_DEBOUNCE_MS)
}

const submitBlocked = computed(
  () => !!emailConflict.value && (emailConflict.value.activeOnThisSite || emailConflict.value.existsInAnotherCompany),
)

function onEmailInput() {
  scheduleSearch(email.value)
  scheduleEmailCheck(email.value)
}

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
  phone.value = ''
  trade.value = ''
  displayName.value = ''
  contactEmail.value = ''
  suggestions.value = []
  matchedExisting.value = false
  emailConflict.value = null
}

async function onSubmit() {
  if (submitBlocked.value) return
  errorMessage.value = ''
  noticeMessage.value = ''
  submitting.value = true
  try {
    if (isServiceProvider.value && !email.value) {
      await addMember(props.siteId, {
        function: memberFunction.value,
        displayName: displayName.value,
        trade: trade.value || null,
        contactEmail: contactEmail.value || null,
        cpf: cpf.value || null,
        phone: phone.value || null,
      })
      noticeMessage.value = t('siteTeam.form.added')
    } else {
      await addMember(props.siteId, {
        function: memberFunction.value,
        displayName: displayName.value,
        email: email.value,
        cpf: cpf.value || null,
        phone: phone.value || null,
      })
      noticeMessage.value = t('siteTeam.form.invited', { email: email.value })
    }
    resetForm()
    showForm.value = false
    await loadMembers()
  } catch (error) {
    if (error instanceof HttpError && error.status === 409) {
      errorMessage.value = t('siteTeam.form.alreadyMember')
    } else if (error instanceof HttpError && error.status === 400) {
      errorMessage.value = t('siteTeam.form.invalidCpf')
    } else {
      errorMessage.value = t('siteTeam.form.error')
    }
  } finally {
    submitting.value = false
  }
}

async function onRemove(membershipId: string) {
  errorMessage.value = ''
  try {
    await removeMember(props.siteId, membershipId)
    confirmingRemovalOf.value = null
    await loadMembers()
  } catch {
    errorMessage.value = t('siteTeam.removeError')
    confirmingRemovalOf.value = null
  }
}

onMounted(loadMembers)
</script>

<template>
  <section class="card card-pad">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('siteTeam.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteTeam.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary" @click="showForm = !showForm">
        {{ t('siteTeam.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-5 grid grid-cols-1 gap-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700 sm:grid-cols-2" @submit.prevent="onSubmit">
      <div class="sm:col-span-2">
        <label class="field-label">{{ t('siteTeam.function.label') }}</label>
        <select v-model="memberFunction" class="field-input">
          <option v-for="fn in functions" :key="fn" :value="fn">{{ t(`siteTeam.function.${fn}`) }}</option>
        </select>
      </div>

      <div class="sm:col-span-2">
        <label class="field-label">{{ t('siteTeam.form.displayName') }}</label>
        <input v-model="displayName" type="text" required class="field-input" />
      </div>

      <div class="relative">
        <label class="field-label">
          {{ t('siteTeam.form.email') }}
          <span v-if="isServiceProvider" class="font-normal text-steel-400">— {{ t('siteTeam.form.emailOptionalHint') }}</span>
        </label>
        <input
          v-model="email"
          type="email"
          :required="!isServiceProvider"
          class="field-input"
          autocomplete="off"
          @input="onEmailInput"
        />
        <ul v-if="suggestions.length > 0" class="modal-panel absolute z-10 mt-1 w-full py-1 shadow-lg">
          <li v-for="person in suggestions" :key="`${person.email}-${person.cpf}`">
            <button
              type="button"
              class="block w-full px-3 py-2 text-left text-sm hover:bg-steel-50 dark:hover:bg-steel-700"
              @click="selectSuggestion(person)"
            >
              <span class="block font-medium text-steel-800 dark:text-steel-50">{{ person.name }}</span>
              <span class="block truncate text-xs text-steel-500 dark:text-steel-400">{{ person.email }}</span>
            </button>
          </li>
        </ul>
      </div>
      <div class="relative">
        <label class="field-label">{{ t('siteTeam.form.cpf') }}</label>
        <input v-model="cpf" type="text" class="field-input" autocomplete="off" @input="scheduleSearch(cpf)" />
      </div>

      <div>
        <label class="field-label">{{ t('siteTeam.form.phone') }}</label>
        <input v-model="phone" type="tel" class="field-input" />
      </div>
      <template v-if="isServiceProvider">
        <div>
          <label class="field-label">{{ t('siteTeam.form.trade') }}</label>
          <input v-model="trade" type="text" :placeholder="t('siteTeam.form.tradePlaceholder')" class="field-input" />
        </div>
        <div class="sm:col-span-2">
          <label class="field-label">{{ t('siteTeam.form.contactEmail') }}</label>
          <input v-model="contactEmail" type="email" class="field-input" />
        </div>
      </template>

      <p v-if="matchedExisting && !submitBlocked" class="text-sm text-blueprint-600 dark:text-blueprint-400 sm:col-span-2">
        {{ t('siteTeam.form.knownPerson') }}
      </p>
      <p v-if="emailConflict?.activeOnThisSite" class="text-sm text-safety-600 dark:text-safety-500 sm:col-span-2">
        {{ t('siteTeam.form.alreadyMember') }}
      </p>
      <p v-else-if="emailConflict?.existsInAnotherCompany" class="text-sm text-safety-600 dark:text-safety-500 sm:col-span-2">
        {{ t('siteTeam.form.emailInAnotherCompany') }}
      </p>
      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500 sm:col-span-2">{{ errorMessage }}</p>
      <div class="flex gap-2 sm:col-span-2">
        <button type="submit" :disabled="submitting || submitBlocked" class="btn-primary">
          {{ matchedExisting ? t('siteTeam.form.sendInvite') : t('siteTeam.form.submit') }}
        </button>
        <button type="button" class="btn-secondary" @click="showForm = false">
          {{ t('siteTeam.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="noticeMessage" class="mb-4 rounded-lg bg-blueprint-50 px-3 py-2 text-sm text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300">
      {{ noticeMessage }}
    </p>

    <p v-if="!loading && members.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteTeam.empty') }}</p>
    <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <li v-for="member in members" :key="member.membershipId" class="relative flex items-center justify-between rounded-lg border border-steel-200 px-4 py-3 dark:border-steel-700">
        <div class="min-w-0">
          <p class="flex items-center gap-2 truncate font-medium text-steel-800 dark:text-steel-50">
            <span class="truncate">{{ member.displayName ?? member.email }}</span>
            <span v-if="member.invited" class="badge-amber shrink-0">{{ t('members.badge.invited') }}</span>
          </p>
          <p v-if="member.email" class="truncate text-sm text-steel-500 dark:text-steel-400">{{ member.email }}</p>
        </div>
        <div class="flex shrink-0 items-start gap-2 pl-3">
          <div class="text-right text-sm">
            <p class="text-steel-700 dark:text-steel-200">{{ t(`siteTeam.function.${member.function}`) }}</p>
            <p v-if="member.trade" class="text-steel-500 dark:text-steel-400">{{ member.trade }}</p>
          </div>
          <button
            type="button"
            class="btn-ghost px-2 py-1 text-xs text-safety-600 dark:text-safety-500"
            @click="confirmingRemovalOf = member.membershipId"
          >
            {{ t('siteTeam.remove') }}
          </button>
        </div>

        <div
          v-if="confirmingRemovalOf === member.membershipId"
          class="modal-panel absolute right-0 top-full z-10 mt-2 w-64 p-3 shadow-lg"
        >
          <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('siteTeam.removeConfirm') }}</p>
          <div class="flex justify-end gap-2">
            <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingRemovalOf = null">
              {{ t('siteTeam.form.cancel') }}
            </button>
            <button type="button" class="btn-danger py-1 text-xs" @click="onRemove(member.membershipId)">
              {{ t('siteTeam.remove') }}
            </button>
          </div>
        </div>
      </li>
    </ul>
  </section>
</template>
