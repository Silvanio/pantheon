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

const router = useRouter()
const { t } = useI18n()
const { logout } = useAuth()
const { status, activeCompany } = useCompanyOnboarding()
const { listSites, createSite } = useConstructionSites()

const companyId = computed(() => (status.value ? activeCompany(status.value)?.companyId ?? null : null))
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

onMounted(loadSites)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <div class="flex items-center gap-2">
          <CompanyLogo v-if="companyId" :company-id="companyId" />
          <div v-else class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
          <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
        </div>
        <div class="flex items-center gap-3">
          <ThemeToggle />
          <div class="relative">
            <button
              type="button"
              class="rounded-md border border-steel-300 px-3 py-1.5 text-sm font-medium text-steel-600 transition hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700"
              @click="showMenu = !showMenu"
            >
              {{ t('dashboard.profileMenu.toggle') }}
            </button>
            <div
              v-if="showMenu"
              class="absolute right-0 z-10 mt-2 w-56 rounded-md border border-steel-200 bg-white py-1 shadow-lg dark:border-steel-700 dark:bg-steel-800"
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
        </div>
      </div>
    </header>

    <main class="mx-auto max-w-5xl space-y-6 px-6 py-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('dashboard.title') }}</h1>
          <p class="mt-1 text-steel-500 dark:text-steel-400">{{ t('dashboard.subtitle') }}</p>
        </div>
        <button
          type="button"
          class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
          @click="showForm = !showForm"
        >
          {{ t('dashboard.newSiteButton') }}
        </button>
      </div>

      <form
        v-if="showForm"
        class="space-y-3 rounded-xl border border-steel-200 bg-white p-6 dark:border-steel-700 dark:bg-steel-800"
        @submit.prevent="onSubmit"
      >
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.name') }}</label>
          <input v-model="name" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.address') }}</label>
          <input v-model="address" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.startDate') }}</label>
            <input v-model="startDate" type="date" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
          </div>
          <div>
            <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.expectedEndDate') }}</label>
            <input v-model="expectedEndDate" type="date" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
          </div>
        </div>
        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
            {{ t('constructionSites.form.submit') }}
          </button>
          <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">
            {{ t('constructionSites.form.cancel') }}
          </button>
        </div>
      </form>

      <p v-if="!loading && sites.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('constructionSites.empty') }}</p>

      <div v-else class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3">
        <button
          v-for="site in sites"
          :key="site.id"
          type="button"
          class="rounded-xl border border-steel-200 bg-white p-4 text-left shadow-sm transition hover:shadow-md dark:border-steel-700 dark:bg-steel-800"
          @click="openSite(site)"
        >
          <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
          <p class="mt-3 font-medium text-steel-800 dark:text-steel-50">{{ site.name }}</p>
          <p class="text-sm text-steel-500 dark:text-steel-400">{{ site.startDate }}</p>
        </button>
      </div>

      <EventLog />
    </main>
  </div>
</template>
