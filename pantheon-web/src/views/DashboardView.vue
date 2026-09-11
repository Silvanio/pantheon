<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuth } from '../composables/useAuth'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import { useConstructionSites, type ConstructionSite } from '../composables/useConstructionSites'
import ThemeToggle from '../components/ThemeToggle.vue'
import EventLog from '../components/EventLog.vue'
import CompanyLogo from '../components/CompanyLogo.vue'
import SitePhoto from '../components/SitePhoto.vue'
import AppHeader from '../components/AppHeader.vue'
import BrandMark from '../components/BrandMark.vue'

const router = useRouter()
const { t } = useI18n()
const { logout } = useAuth()
const { status, activeCompany } = useCompanyOnboarding()
const { listSites, createSite } = useConstructionSites()

const companyId = computed(() => (status.value ? activeCompany(status.value)?.companyId ?? null : null))
const isCompanyAdmin = computed(() => (status.value ? activeCompany(status.value)?.role === 'ADMIN' : false))
const sites = ref<ConstructionSite[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const showMenu = ref(false)

const name = ref('')
const address = ref('')
const startDate = ref('')
const expectedEndDate = ref('')

async function loadSites() {
  if (!companyId.value) return
  loading.value = true
  try {
    sites.value = await listSites(companyId.value)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!companyId.value) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await createSite(companyId.value, {
      name: name.value,
      address: address.value,
      startDate: startDate.value,
      expectedEndDate: expectedEndDate.value || null,
    })
    name.value = ''
    address.value = ''
    startDate.value = ''
    expectedEndDate.value = ''
    showForm.value = false
    await loadSites()
  } catch {
    errorMessage.value = t('dashboard.form.error')
  } finally {
    submitting.value = false
  }
}

function openSite(site: ConstructionSite) {
  router.push(`/sites/${site.id}`)
}

function onLogout() {
  logout()
  router.push('/login')
}

const showEventLog = ref(false)

onMounted(loadSites)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <div v-if="companyId" class="flex items-center gap-2.5">
          <CompanyLogo :company-id="companyId" />
          <span class="text-lg font-semibold tracking-tight text-steel-800 dark:text-steel-50">Pantheon</span>
        </div>
        <BrandMark v-else />
      </template>
      <template #right>
        <ThemeToggle />
        <div class="relative">
          <button
            type="button"
            class="btn-secondary py-1.5"
            @click="showMenu = !showMenu"
          >
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
            <button type="button" class="block w-full px-4 py-2 text-left text-sm text-steel-700 hover:bg-steel-50 dark:text-steel-200 dark:hover:bg-steel-700" @click="onLogout">
              {{ t('dashboard.profileMenu.logout') }}
            </button>
          </div>
        </div>
      </template>
    </AppHeader>

    <main class="app-container space-y-8 py-10">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-3xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('dashboard.title') }}</h1>
          <p class="mt-1.5 text-steel-500 dark:text-steel-400">{{ t('dashboard.subtitle') }}</p>
        </div>
        <div class="flex gap-2">
          <router-link
            v-if="isCompanyAdmin && companyId"
            :to="`/companies/${companyId}/tasks-board`"
            class="btn-secondary"
          >
            {{ t('dashboard.globalTasksBoardButton') }}
          </router-link>
          <button type="button" class="btn-primary" @click="showForm = !showForm">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 5v14M5 12h14" />
            </svg>
            {{ t('dashboard.newSiteButton') }}
          </button>
        </div>
      </div>

      <form v-if="showForm" class="card card-pad space-y-4" @submit.prevent="onSubmit">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label class="field-label">{{ t('constructionSites.form.name') }}</label>
            <input v-model="name" type="text" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('constructionSites.form.address') }}</label>
            <input v-model="address" type="text" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('constructionSites.form.startDate') }}</label>
            <input v-model="startDate" type="date" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('constructionSites.form.expectedEndDate') }}</label>
            <input v-model="expectedEndDate" type="date" class="field-input" />
          </div>
        </div>
        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="btn-primary">
            {{ t('constructionSites.form.submit') }}
          </button>
          <button type="button" class="btn-secondary" @click="showForm = false">
            {{ t('constructionSites.form.cancel') }}
          </button>
        </div>
      </form>

      <div v-if="!loading && sites.length === 0" class="card card-pad flex flex-col items-center gap-3 py-16 text-center">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-blueprint-50 text-blueprint-500 dark:bg-blueprint-900/30 dark:text-blueprint-400">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="h-7 w-7">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6" />
          </svg>
        </div>
        <p class="text-steel-500 dark:text-steel-400">{{ t('constructionSites.empty') }}</p>
      </div>

      <div v-else class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5">
        <button
          v-for="site in sites"
          :key="site.id"
          type="button"
          class="card group overflow-hidden text-left transition hover:-translate-y-0.5 hover:shadow-lg"
          @click="openSite(site)"
        >
          <div class="relative h-40 w-full overflow-hidden">
            <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
            <div class="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent"></div>
          </div>
          <div class="p-4">
            <p class="truncate font-semibold text-steel-800 dark:text-steel-50">{{ site.name }}</p>
            <p class="mt-1 flex items-center gap-1.5 text-sm text-steel-500 dark:text-steel-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5 shrink-0">
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <path stroke-linecap="round" d="M16 2v4M8 2v4M3 10h18" />
              </svg>
              {{ site.startDate }}
            </p>
          </div>
        </button>
      </div>

      <div class="pt-4">
        <button type="button" class="text-xs font-medium text-steel-400 hover:text-steel-600 dark:hover:text-steel-300" @click="showEventLog = !showEventLog">
          {{ showEventLog ? '— Ocultar eventos em tempo real' : '+ Eventos em tempo real' }}
        </button>
        <div v-if="showEventLog" class="mt-3">
          <EventLog />
        </div>
      </div>
    </main>
  </div>
</template>
