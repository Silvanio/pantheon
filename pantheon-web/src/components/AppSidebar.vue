<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuth } from '../composables/useAuth'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { logout, userEmail } = useAuth()
const { status, activeCompany } = useCompanyOnboarding()

const companyId = computed(() => (status.value ? activeCompany(status.value)?.companyId ?? null : null))
const initials = computed(() => (userEmail.value ?? 'U').trim().charAt(0).toUpperCase())

const isDashboard = () => route.name === 'dashboard'
const isCompanySettings = () => typeof route.name === 'string' && route.name.startsWith('company-settings')

function onLogout() {
  logout()
  router.push('/login')
}
</script>

<template>
  <aside class="sticky top-0 flex h-screen w-[76px] shrink-0 flex-col items-center gap-1 bg-ink-950 py-5">
    <button
      type="button"
      class="mb-6 flex h-10 w-10 items-center justify-center rounded-xl bg-blueprint-500 text-white transition hover:bg-blueprint-400"
      :title="t('appSidebar.home')"
      @click="router.push('/')"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-5 w-5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6" />
      </svg>
    </button>

    <button
      type="button"
      class="flex h-11 w-11 items-center justify-center rounded-xl transition"
      :class="isDashboard() ? 'bg-ink-800 text-white' : 'text-steel-400 hover:bg-ink-800 hover:text-white'"
      :title="t('appSidebar.myObras')"
      @click="router.push('/')"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[19px] w-[19px]">
        <path stroke-linecap="round" stroke-linejoin="round" d="M3 12l9-9 9 9M5 10v10h14V10" />
      </svg>
    </button>

    <button
      v-if="companyId"
      type="button"
      class="flex h-11 w-11 items-center justify-center rounded-xl transition"
      :class="isCompanySettings() ? 'bg-ink-800 text-white' : 'text-steel-400 hover:bg-ink-800 hover:text-white'"
      :title="t('appSidebar.companySettings')"
      @click="router.push(`/companies/${companyId}/settings`)"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[19px] w-[19px]">
        <circle cx="12" cy="12" r="3" />
        <path
          d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06A1.65 1.65 0 004.6 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06A1.65 1.65 0 008.09 4.6 1.65 1.65 0 009 3.51V3.4a2 2 0 014 0v.09c.14.63.5 1.16 1 1.51.55.24 1.23.16 1.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06c-.49.49-.57 1.17-.33 1.82.35.5.88.86 1.51 1H21a2 2 0 010 4h-.09c-.63.14-1.16.5-1.51 1z"
        />
      </svg>
    </button>

    <div class="flex-1"></div>

    <button
      type="button"
      class="mb-1 flex h-10 w-10 items-center justify-center rounded-xl text-steel-400 transition hover:bg-safety-500/15 hover:text-safety-500"
      :title="t('appSidebar.logout')"
      @click="onLogout"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-[18px] w-[18px]">
        <path stroke-linecap="round" stroke-linejoin="round" d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" />
      </svg>
    </button>

    <button
      type="button"
      class="flex h-9 w-9 items-center justify-center rounded-full bg-blueprint-600 text-[13px] font-bold text-white transition hover:bg-blueprint-500"
      :title="t('appSidebar.profile')"
      @click="router.push('/profile')"
    >
      {{ initials }}
    </button>
  </aside>
</template>
