<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanies, type Company } from '../composables/useCompanies'
import { useConstructionSites, type ConstructionSite, type SiteStatus } from '../composables/useConstructionSites'
import AppSidebar from '../components/AppSidebar.vue'
import SitePhoto from '../components/SitePhoto.vue'
import StatusBadge from '../components/StatusBadge.vue'

const router = useRouter()
const { t } = useI18n()
const { listAllCompanies } = useCompanies()
const { listSites } = useConstructionSites()

const COMPANY_OPTIONS_LIMIT = 10
const COMPANY_SEARCH_MIN_CHARS = 3

const companyOptions = ref<Company[]>([])
const companySearchQuery = ref('')
const showCompanyFilter = ref(false)
const loadingOptions = ref(false)

const selectedCompany = ref<Company | null>(null)
const sites = ref<ConstructionSite[]>([])
const sitesLoading = ref(false)
const sitesError = ref('')

let searchDebounce: ReturnType<typeof setTimeout> | null = null

const PLACEHOLDER_PROGRESS: Record<SiteStatus, number> = {
  PLANNING: 10,
  IN_PROGRESS: 55,
  PAUSED: 40,
  COMPLETED: 100,
}

function progressFor(site: ConstructionSite): number {
  return site.schedulePercentComplete ?? PLACEHOLDER_PROGRESS[site.status]
}

async function loadCompanyOptions() {
  loadingOptions.value = true
  try {
    const query = companySearchQuery.value.trim()
    const result = await listAllCompanies({
      search: query.length >= COMPANY_SEARCH_MIN_CHARS ? query : undefined,
      page: 0,
      size: COMPANY_OPTIONS_LIMIT,
    })
    companyOptions.value = result.content
  } finally {
    loadingOptions.value = false
  }
}

watch(companySearchQuery, () => {
  if (searchDebounce) clearTimeout(searchDebounce)
  searchDebounce = setTimeout(loadCompanyOptions, 300)
})

async function selectCompany(company: Company) {
  selectedCompany.value = company
  companySearchQuery.value = ''
  showCompanyFilter.value = false
  sites.value = []
  sitesError.value = ''
  sitesLoading.value = true
  try {
    sites.value = await listSites(company.id)
  } catch {
    sitesError.value = t('admin.companies.sitesError')
  } finally {
    sitesLoading.value = false
  }
}

function openSite(site: ConstructionSite) {
  router.push(`/sites/${site.id}`)
}

onMounted(loadCompanyOptions)
</script>

<template>
  <div class="flex min-h-screen bg-steel-50 dark:bg-steel-950">
    <AppSidebar />

    <div class="min-w-0 flex-1 px-8 py-9 sm:px-12 lg:px-14">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 class="text-xl font-extrabold tracking-tight text-steel-900 dark:text-steel-50">{{ t('admin.companies.title') }}</h1>
          <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('admin.companies.subtitle') }}</p>
        </div>

        <div class="flex items-center gap-2">
          <div class="relative">
            <button
              type="button"
              class="btn-secondary"
              :class="{ 'ring-2 ring-blueprint-500/30': selectedCompany }"
              @click="showCompanyFilter = !showCompanyFilter"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 4h18M6 8h12M10 12h4" />
              </svg>
              {{ selectedCompany ? selectedCompany.name : t('admin.companies.selectCompany') }}
            </button>
            <div v-if="showCompanyFilter" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-3 shadow-lg" @click.stop>
              <input
                v-model="companySearchQuery"
                type="text"
                class="field-input mb-2"
                :placeholder="t('admin.companies.searchPlaceholder')"
              />
              <ul class="max-h-64 space-y-0.5 overflow-y-auto">
                <li v-if="!loadingOptions && companyOptions.length === 0" class="px-2 py-1.5 text-xs text-steel-500 dark:text-steel-400">
                  {{ t('admin.companies.empty') }}
                </li>
                <li v-for="company in companyOptions" :key="company.id">
                  <button
                    type="button"
                    class="block w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-steel-100 dark:hover:bg-steel-700"
                    :class="selectedCompany?.id === company.id ? 'font-semibold text-blueprint-600 dark:text-blueprint-400' : 'text-steel-700 dark:text-steel-200'"
                    @click="selectCompany(company)"
                  >
                    {{ company.name }}
                    <span v-if="company.tradeName" class="block truncate text-xs font-normal text-steel-500 dark:text-steel-400">{{ company.tradeName }}</span>
                  </button>
                </li>
              </ul>
            </div>
          </div>
          <RouterLink
            v-if="selectedCompany"
            :to="`/companies/${selectedCompany.id}/settings`"
            class="btn-ghost px-2 py-1.5 text-xs"
          >
            {{ t('admin.companies.settingsButton') }}
          </RouterLink>
        </div>
      </div>

      <template v-if="selectedCompany">
        <p v-if="sitesError" class="mt-6 text-sm text-safety-600 dark:text-safety-500">{{ sitesError }}</p>
        <p v-else-if="!sitesLoading && sites.length === 0" class="mt-6 text-sm text-steel-500 dark:text-steel-400">
          {{ t('admin.companies.noSites') }}
        </p>
        <div v-else class="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          <button
            v-for="site in sites"
            :key="site.id"
            type="button"
            class="flex flex-col rounded-2xl border border-steel-200 bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-steel-800 dark:bg-steel-900"
            @click="openSite(site)"
          >
            <div class="relative h-28 w-full shrink-0 overflow-hidden rounded-t-2xl bg-gradient-to-br from-blueprint-600 to-ink-900">
              <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
              <div
                v-if="site.photoObjectKey"
                class="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/45 via-transparent to-transparent"
              ></div>
            </div>
            <div class="p-4">
              <p class="truncate text-[15px] font-extrabold text-steel-900 dark:text-steel-50">{{ site.name }}</p>
              <p class="mt-1 flex items-center gap-1.5 text-xs text-steel-500 dark:text-steel-400">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5 shrink-0">
                  <rect x="3" y="4" width="18" height="18" rx="2" />
                  <path stroke-linecap="round" d="M16 2v4M8 2v4M3 10h18" />
                </svg>
                {{ site.startDate }}
              </p>

              <div class="mt-3.5 flex items-center justify-between">
                <span class="text-[11px] font-bold text-steel-500 dark:text-steel-400">{{ t('dashboard.card.progress') }}</span>
                <span class="text-xs font-extrabold text-emerald-600">{{ progressFor(site) }}%</span>
              </div>
              <div class="mt-1.5 h-1.5 overflow-hidden rounded-full bg-steel-100 dark:bg-steel-800">
                <div class="h-full rounded-full bg-emerald-500" :style="{ width: progressFor(site) + '%' }"></div>
              </div>

              <div class="mt-3.5 flex items-center justify-end">
                <StatusBadge kind="constructionSite" :status="site.status" />
              </div>
            </div>
          </button>
        </div>
      </template>
      <p v-else class="mt-6 text-sm text-steel-500 dark:text-steel-400">{{ t('admin.companies.selectHint') }}</p>
    </div>
  </div>
</template>
