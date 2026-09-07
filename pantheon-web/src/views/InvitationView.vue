<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { HttpError, useAuth } from '../composables/useAuth'
import { useInvitations, type Invitation } from '../composables/useInvitations'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import AuthShell from '../components/AuthShell.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { isAuthenticated, setToken } = useAuth()
const { getInvitation, completeRegistration, acceptInvitation } = useInvitations()
const { invalidate } = useCompanyOnboarding()

const token = computed(() => String(route.params.token ?? ''))

const loading = ref(true)
const invitation = ref<Invitation | null>(null)
const loadError = ref('')

const displayName = ref('')
const password = ref('')
const submitting = ref(false)
const actionError = ref('')

const needsRegistration = computed(
  () => invitation.value?.requiresRegistration === true && !isAuthenticated.value,
)
const needsLogin = computed(
  () => invitation.value?.requiresRegistration === false && !isAuthenticated.value,
)
const canAccept = computed(() => invitation.value !== null && !invitation.value.accepted && isAuthenticated.value)

onMounted(async () => {
  try {
    invitation.value = await getInvitation(token.value)
  } catch {
    loadError.value = t('invitation.notFound')
  } finally {
    loading.value = false
  }
})

async function onCompleteRegistration() {
  actionError.value = ''
  submitting.value = true
  try {
    const result = await completeRegistration(token.value, {
      password: password.value,
      displayName: displayName.value,
    })
    setToken(result.token)
  } catch (error) {
    actionError.value =
      error instanceof HttpError && error.status === 409
        ? t('invitation.registrationNotApplicable')
        : t('invitation.registrationError')
  } finally {
    submitting.value = false
  }
}

async function onAccept() {
  actionError.value = ''
  submitting.value = true
  try {
    await acceptInvitation(token.value)
    invalidate()
    router.push('/')
  } catch (error) {
    actionError.value =
      error instanceof HttpError && error.status === 403
        ? t('invitation.wrongAccount')
        : t('invitation.acceptError')
  } finally {
    submitting.value = false
  }
}

function goToLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}
</script>

<template>
  <AuthShell heading="Você foi convidado." description="Aceite o convite para acessar a obra ou empresa no Pantheon.">
    <p v-if="loading" class="text-sm text-steel-500 dark:text-steel-400">{{ t('invitation.loading') }}</p>

    <p v-else-if="loadError" class="text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>

    <template v-else-if="invitation">
      <h1 class="mb-2 text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('invitation.title') }}</h1>
      <p class="mb-6 text-sm text-steel-600 dark:text-steel-300">
        {{
          t(invitation.membershipType === 'SITE' ? 'invitation.summarySite' : 'invitation.summaryCompany', {
            inviter: invitation.inviterName ?? t('invitation.someone'),
            target: invitation.targetName,
          })
        }}
      </p>

      <p v-if="invitation.accepted" class="text-sm text-steel-600 dark:text-steel-300">
        {{ t('invitation.alreadyAccepted') }}
        <router-link to="/" class="font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">{{ t('invitation.goToApp') }}</router-link>
      </p>

      <form v-else-if="needsRegistration" class="space-y-4" @submit.prevent="onCompleteRegistration">
        <p class="text-sm text-steel-600 dark:text-steel-300">{{ t('invitation.registerPrompt', { email: invitation.email }) }}</p>
        <div>
          <label class="field-label" for="displayName">{{ t('invitation.name') }}</label>
          <input id="displayName" v-model="displayName" type="text" required class="field-input" />
        </div>
        <div>
          <label class="field-label" for="password">{{ t('invitation.password') }}</label>
          <input id="password" v-model="password" type="password" required minlength="8" class="field-input" />
        </div>
        <p v-if="actionError" class="text-sm text-safety-600 dark:text-safety-500">{{ actionError }}</p>
        <button type="submit" :disabled="submitting" class="btn-primary w-full py-2.5">
          {{ t('invitation.continue') }}
        </button>
      </form>

      <div v-else-if="needsLogin" class="space-y-4">
        <p class="text-sm text-steel-600 dark:text-steel-300">{{ t('invitation.loginPrompt', { email: invitation.email }) }}</p>
        <button type="button" class="btn-primary w-full py-2.5" @click="goToLogin">
          {{ t('invitation.goToLogin') }}
        </button>
      </div>

      <div v-else-if="canAccept" class="space-y-4">
        <p v-if="actionError" class="text-sm text-safety-600 dark:text-safety-500">{{ actionError }}</p>
        <button type="button" :disabled="submitting" class="btn-primary w-full py-2.5" @click="onAccept">
          {{ t('invitation.accept') }}
        </button>
      </div>
    </template>
  </AuthShell>
</template>
