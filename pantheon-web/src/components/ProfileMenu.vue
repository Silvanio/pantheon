<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuth } from '../composables/useAuth'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'

const router = useRouter()
const { t } = useI18n()
const { logout } = useAuth()
const { status, activeCompany } = useCompanyOnboarding()

const companyId = computed(() => (status.value ? activeCompany(status.value)?.companyId ?? null : null))
const showMenu = ref(false)

function onLogout() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="relative">
    <button type="button" class="btn-secondary py-1.5" @click="showMenu = !showMenu">
      {{ t('dashboard.profileMenu.toggle') }}
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
      </svg>
    </button>
    <div
      v-if="showMenu"
      class="absolute right-0 z-10 mt-2 w-56 rounded-xl border border-steel-200 bg-white py-1 shadow-lg dark:border-steel-700 dark:bg-steel-800"
      @click="showMenu = false"
    >
      <router-link
        v-if="companyId"
        :to="`/companies/${companyId}/plan`"
        class="block px-4 py-2 text-sm text-steel-700 hover:bg-steel-50 dark:text-steel-200 dark:hover:bg-steel-700"
      >
        {{ t('dashboard.profileMenu.changePlan') }}
      </router-link>
      <router-link
        v-if="companyId"
        :to="`/companies/${companyId}/settings`"
        class="block px-4 py-2 text-sm text-steel-700 hover:bg-steel-50 dark:text-steel-200 dark:hover:bg-steel-700"
      >
        {{ t('dashboard.profileMenu.editCompany') }}
      </router-link>
      <router-link
        v-else
        to="/profile"
        class="block px-4 py-2 text-sm text-steel-700 hover:bg-steel-50 dark:text-steel-200 dark:hover:bg-steel-700"
      >
        {{ t('dashboard.profileMenu.editRegistration') }}
      </router-link>
      <button type="button" class="block w-full px-4 py-2 text-left text-sm text-steel-700 hover:bg-steel-50 dark:text-steel-200 dark:hover:bg-steel-700" @click="onLogout">
        {{ t('dashboard.profileMenu.logout') }}
      </button>
    </div>
  </div>
</template>
