<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useConstructionSites, type ConstructionSite, type SiteStatus } from '../composables/useConstructionSites'
import { useCompanies } from '../composables/useCompanies'
import { useSiteHeroCollapse } from '../composables/useSiteHeroCollapse'
import { useSitePermissions, type AccessLevel, type PermissionCapability } from '../composables/useSitePermissions'
import { usePurchaseRequests } from '../composables/usePurchaseRequests'
import SitePhoto from '../components/SitePhoto.vue'
import SiteTeamPanel from '../components/SiteTeamPanel.vue'
import SiteDocumentProjectsPanel from '../components/SiteDocumentProjectsPanel.vue'
import SitePermissionsPanel from '../components/SitePermissionsPanel.vue'
import SitePurchaseRequestApprovalLevelsPanel from '../components/SitePurchaseRequestApprovalLevelsPanel.vue'
import EquipmentPanel from '../components/EquipmentPanel.vue'
import PurchaseRequestPanel from '../components/PurchaseRequestPanel.vue'
import OrcamentoListPanel from '../components/OrcamentoListPanel.vue'
import DailyReportsPanel from '../components/DailyReportsPanel.vue'
import TasksBoardPanel from '../components/TasksBoardPanel.vue'
import SchedulePanel from '../components/SchedulePanel.vue'
import SiteSummaryPanel from '../components/SiteSummaryPanel.vue'
import AppHeader from '../components/AppHeader.vue'
import AppSidebar from '../components/AppSidebar.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import StatusBadge from '../components/StatusBadge.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getSite, updateSitePhoto, updateSiteStatus } = useConstructionSites()
const { listMyCompanies } = useCompanies()
const { collapsed: heroCollapsed, toggle: toggleHero } = useSiteHeroCollapse()
const { getMyPermissions } = useSitePermissions()
const { listPurchaseRequests } = usePurchaseRequests()

const siteId = route.params.siteId as string
const site = ref<ConstructionSite | null>(null)
const loading = ref(false)
const isCompanyAdmin = ref(false)

type Tab = 'summary' | 'team' | 'dailyReport' | 'projects' | 'equipment' | 'purchaseRequests' | 'orcamentos' | 'tasks' | 'schedule' | 'permissions'
const activeTab = ref<Tab>('summary')

// Tabs backed by a PermissionCapability can be hidden per member; 'summary' (aggregates every
// section rather than belonging to one capability) and 'permissions' (gated by company-admin
// status, not a capability) are exempt — both always resolve visible via isTabVisible below.
const TAB_CAPABILITY: Partial<Record<Tab, PermissionCapability>> = {
  team: 'TEAM_MANAGE',
  dailyReport: 'DAILY_REPORT',
  projects: 'DOCUMENT_PROJECTS',
  equipment: 'EQUIPMENT',
  purchaseRequests: 'PURCHASE_REQUEST',
  orcamentos: 'ORCAMENTO_MANAGE',
  tasks: 'TASKS',
  schedule: 'SCHEDULE',
}
// 'summary' leads so it's always the default landing tab (TAB_ORDER.find(isTabVisible) below).
const TAB_ORDER: Tab[] = ['summary', 'team', 'dailyReport', 'projects', 'equipment', 'purchaseRequests', 'orcamentos', 'tasks', 'schedule']

const myPermissions = ref<Record<PermissionCapability, AccessLevel> | null>(null)
const pendingPurchaseRequestCount = ref(0)

const SITE_STATUSES: SiteStatus[] = ['PLANNING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED']
const canManageSiteStatus = computed(() => myPermissions.value?.SITE_STATUS === 'MANAGE')
const savingStatus = ref(false)
const statusError = ref('')

async function onStatusChange(event: Event) {
  if (!site.value) return
  const newStatus = (event.target as HTMLSelectElement).value as SiteStatus
  statusError.value = ''
  savingStatus.value = true
  try {
    site.value = await updateSiteStatus(site.value.id, newStatus)
  } catch {
    statusError.value = t('siteDetail.statusUpdateError')
  } finally {
    savingStatus.value = false
  }
}

async function loadPendingPurchaseRequestCount() {
  try {
    const page = await listPurchaseRequests(siteId, { status: 'ORCADO', size: 100 })
    pendingPurchaseRequestCount.value = page.content.filter((pr) => pr.submittedAt).length
  } catch {
    pendingPurchaseRequestCount.value = 0
  }
}

function isTabVisible(tab: Tab): boolean {
  const capability = TAB_CAPABILITY[tab]
  if (!capability) return true
  // Don't hide anything before permissions have loaded — avoids a flash of a tab disappearing.
  if (!myPermissions.value) return true
  return myPermissions.value[capability] !== 'HIDDEN'
}

async function load() {
  loading.value = true
  try {
    const [siteResult, memberships, permissions] = await Promise.all([
      getSite(siteId),
      listMyCompanies(),
      getMyPermissions(siteId),
    ])
    site.value = siteResult
    isCompanyAdmin.value = memberships.some((m) => m.companyId === site.value?.companyId && m.role === 'ADMIN')
    myPermissions.value = permissions
    const requestedTab = route.query.tab as Tab | undefined
    const firstVisible = TAB_ORDER.find(isTabVisible)
    if (requestedTab && TAB_ORDER.includes(requestedTab) && isTabVisible(requestedTab)) {
      activeTab.value = requestedTab
    } else if (firstVisible) {
      activeTab.value = firstVisible
    }
    if (isTabVisible('purchaseRequests')) loadPendingPurchaseRequestCount()
  } finally {
    loading.value = false
  }
}

async function onPhotoChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  site.value = await updateSitePhoto(siteId, file)
}

onMounted(load)
</script>

<template>
  <div class="flex min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppSidebar />

    <!-- Obra context nav -->
    <aside
      v-if="site"
      class="sticky top-0 hidden h-screen w-60 shrink-0 flex-col overflow-y-auto border-r border-steel-200 bg-white px-4 py-6 dark:border-steel-800 dark:bg-steel-900 md:flex"
    >
      <button type="button" class="mb-5 flex items-center gap-1.5 px-1 text-xs font-semibold text-steel-500 hover:text-steel-700 dark:text-steel-400 dark:hover:text-steel-200" @click="router.push('/')">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="h-3.5 w-3.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
        </svg>
        {{ t('siteDetail.back') }}
      </button>
      <div class="mb-5 flex items-center gap-2.5 px-1">
        <div class="h-9 w-9 shrink-0 overflow-hidden rounded-lg bg-gradient-to-br from-blueprint-600 to-ink-900">
          <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
        </div>
        <div class="min-w-0">
          <p class="truncate text-sm font-bold text-steel-800 dark:text-steel-50">{{ site.name }}</p>
          <StatusBadge v-if="!canManageSiteStatus" kind="constructionSite" :status="site.status" class="mt-0.5" />
          <select
            v-else
            :value="site.status"
            :disabled="savingStatus"
            class="mt-0.5 rounded-md border border-steel-300 bg-white px-1.5 py-0.5 text-xs text-steel-700 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-100"
            @change="onStatusChange"
          >
            <option v-for="status in SITE_STATUSES" :key="status" :value="status">
              {{ t(`constructionSites.status.${status}`) }}
            </option>
          </select>
        </div>
      </div>
      <p v-if="statusError" class="mb-3 px-1 text-xs text-safety-600 dark:text-safety-500">{{ statusError }}</p>
      <nav class="flex flex-col gap-0.5">
        <button
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'summary' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'summary'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><rect x="3" y="3" width="7" height="9" rx="1" /><rect x="14" y="3" width="7" height="5" rx="1" /><rect x="14" y="12" width="7" height="9" rx="1" /><rect x="3" y="16" width="7" height="5" rx="1" /></svg>
          {{ t('siteDetail.tabs.summary') }}
        </button>
        <button
          v-if="isTabVisible('team')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'team' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'team'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" /></svg>
          {{ t('siteDetail.tabs.team') }}
        </button>
        <button
          v-if="isTabVisible('dailyReport')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'dailyReport' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'dailyReport'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" /><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" /></svg>
          {{ t('siteDetail.tabs.dailyReport') }}
        </button>
        <button
          v-if="isTabVisible('projects')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'projects' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'projects'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z" /></svg>
          {{ t('siteDetail.tabs.projects') }}
        </button>
        <button
          v-if="isTabVisible('equipment')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'equipment' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'equipment'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06A1.65 1.65 0 004.6 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06A1.65 1.65 0 008.09 4.6 1.65 1.65 0 009 3.51V3.4a2 2 0 014 0v.09c.14.63.5 1.16 1 1.51.55.24 1.23.16 1.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06c-.49.49-.57 1.17-.33 1.82.35.5.88.86 1.51 1H21a2 2 0 010 4h-.09c-.63.14-1.16.5-1.51 1z" /></svg>
          {{ t('siteDetail.tabs.equipment') }}
        </button>
        <button
          v-if="isTabVisible('purchaseRequests')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'purchaseRequests' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'purchaseRequests'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z" /><path d="M3 6h18M16 10a4 4 0 01-8 0" /></svg>
          {{ t('siteDetail.tabs.purchaseRequests') }}
          <span v-if="pendingPurchaseRequestCount > 0" class="ml-auto rounded-full bg-safety-500 px-[7px] py-0.5 text-[10px] font-bold text-white">
            {{ pendingPurchaseRequestCount }}
          </span>
        </button>
        <button
          v-if="isTabVisible('orcamentos')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'orcamentos' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'orcamentos'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" /></svg>
          {{ t('siteDetail.tabs.orcamentos') }}
        </button>
        <button
          v-if="isTabVisible('tasks')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'tasks' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'tasks'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 3v18M3 9h6" /></svg>
          {{ t('siteDetail.tabs.tasks') }}
        </button>
        <button
          v-if="isTabVisible('schedule')"
          type="button"
          class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
          :class="activeTab === 'schedule' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
          @click="activeTab = 'schedule'"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><rect x="3" y="4" width="18" height="18" rx="2" /><path d="M16 2v4M8 2v4M3 10h18" /></svg>
          {{ t('siteDetail.tabs.schedule') }}
        </button>
        <template v-if="isCompanyAdmin">
          <div class="my-2 h-px bg-steel-100 dark:bg-steel-800"></div>
          <button
            type="button"
            class="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-semibold transition"
            :class="activeTab === 'permissions' ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'text-steel-500 hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800'"
            @click="activeTab = 'permissions'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0"><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0110 0v4" /></svg>
            {{ t('siteDetail.tabs.permissions') }}
          </button>
        </template>
      </nav>
    </aside>

    <div class="min-w-0 flex-1">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2 md:hidden" @click="router.push('/')">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('siteDetail.back') }}
        </button>
        <template v-if="site && heroCollapsed">
          <h1 class="truncate text-sm font-semibold text-steel-800 dark:text-steel-100">{{ site.name }}</h1>
        </template>
      </template>
      <template #right>
        <ThemeToggle />
        <button
          v-if="site && heroCollapsed"
          type="button"
          class="flex h-9 w-9 items-center justify-center rounded-md border border-steel-300 text-steel-600 transition hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700"
          :title="t('siteDetail.expandPhoto')"
          @click="toggleHero"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </template>
    </AppHeader>

    <template v-if="site">
      <div v-if="!heroCollapsed" class="relative h-56 w-full overflow-hidden sm:h-72">
        <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
        <div class="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent"></div>
        <button
          type="button"
          class="absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur-sm transition hover:bg-black/55"
          :title="t('siteDetail.collapsePhoto')"
          @click="toggleHero"
        >
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M5 15l7-7 7 7" />
          </svg>
        </button>
        <div class="app-container absolute inset-x-0 bottom-0 flex items-end justify-between gap-4 pb-6">
          <div class="min-w-0">
            <h1 class="truncate text-2xl font-semibold text-white drop-shadow sm:text-3xl">{{ site.name }}</h1>
            <p class="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-white/85">
              <span class="flex items-center gap-1.5">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 21s-7-5.5-7-11a7 7 0 1 1 14 0c0 5.5-7 11-7 11z" />
                  <circle cx="12" cy="10" r="2.5" />
                </svg>
                {{ site.address }}
              </span>
              <span class="flex items-center gap-1.5">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5">
                  <rect x="3" y="4" width="18" height="18" rx="2" />
                  <path stroke-linecap="round" d="M16 2v4M8 2v4M3 10h18" />
                </svg>
                {{ site.startDate }}
              </span>
            </p>
          </div>
          <label class="btn-secondary shrink-0 cursor-pointer bg-white/90 backdrop-blur hover:bg-white dark:bg-steel-800/90">
            {{ t('siteDetail.changePhoto') }}
            <input type="file" accept="image/*" class="hidden" @change="onPhotoChange" />
          </label>
        </div>
      </div>

      <main class="app-container space-y-6 py-8">
        <!-- Compact fallback nav for narrow viewports, where the obra sidebar is hidden -->
        <nav class="flex flex-wrap gap-1.5 border-b border-steel-200 pb-3 dark:border-steel-800 md:hidden">
          <button
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'summary' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'summary'"
          >
            {{ t('siteDetail.tabs.summary') }}
          </button>
          <button
            v-if="isTabVisible('team')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'team' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'team'"
          >
            {{ t('siteDetail.tabs.team') }}
          </button>
          <button
            v-if="isTabVisible('dailyReport')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'dailyReport' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'dailyReport'"
          >
            {{ t('siteDetail.tabs.dailyReport') }}
          </button>
          <button
            v-if="isTabVisible('projects')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'projects' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'projects'"
          >
            {{ t('siteDetail.tabs.projects') }}
          </button>
          <button
            v-if="isTabVisible('equipment')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'equipment' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'equipment'"
          >
            {{ t('siteDetail.tabs.equipment') }}
          </button>
          <button
            v-if="isTabVisible('purchaseRequests')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'purchaseRequests' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'purchaseRequests'"
          >
            {{ t('siteDetail.tabs.purchaseRequests') }}
          </button>
          <button
            v-if="isTabVisible('orcamentos')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'orcamentos' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'orcamentos'"
          >
            {{ t('siteDetail.tabs.orcamentos') }}
          </button>
          <button
            v-if="isTabVisible('tasks')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'tasks' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'tasks'"
          >
            {{ t('siteDetail.tabs.tasks') }}
          </button>
          <button
            v-if="isTabVisible('schedule')"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'schedule' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'schedule'"
          >
            {{ t('siteDetail.tabs.schedule') }}
          </button>
          <button
            v-if="isCompanyAdmin"
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'permissions' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'permissions'"
          >
            {{ t('siteDetail.tabs.permissions') }}
          </button>
        </nav>

        <SiteSummaryPanel v-if="activeTab === 'summary'" :site-id="siteId" @open-tab="activeTab = $event" />
        <SiteTeamPanel v-if="activeTab === 'team' && isTabVisible('team')" :site-id="siteId" />
        <DailyReportsPanel v-if="activeTab === 'dailyReport' && isTabVisible('dailyReport')" :site-id="siteId" />
        <SiteDocumentProjectsPanel
          v-if="activeTab === 'projects' && isTabVisible('projects')"
          :site-id="siteId"
          :can-manage="myPermissions?.DOCUMENT_PROJECTS === 'MANAGE'"
        />
        <EquipmentPanel v-if="activeTab === 'equipment' && isTabVisible('equipment')" :site-id="siteId" />
        <PurchaseRequestPanel
          v-if="activeTab === 'purchaseRequests' && isTabVisible('purchaseRequests')"
          :site-id="siteId"
          :can-manage="myPermissions?.PURCHASE_REQUEST === 'MANAGE'"
        />
        <OrcamentoListPanel v-if="activeTab === 'orcamentos' && isTabVisible('orcamentos')" :site-id="siteId" />
        <TasksBoardPanel
          v-if="activeTab === 'tasks' && isTabVisible('tasks')"
          :site-id="siteId"
          :can-manage-tasks="myPermissions?.TASKS === 'MANAGE'"
          :can-view-projects="myPermissions?.DOCUMENT_PROJECTS !== 'HIDDEN'"
        />
        <SchedulePanel
          v-if="activeTab === 'schedule' && isTabVisible('schedule')"
          :site-id="siteId"
          :can-manage="myPermissions?.SCHEDULE === 'MANAGE'"
          :can-manage-tasks="myPermissions?.TASKS === 'MANAGE'"
          @open-tab="activeTab = $event"
        />
        <template v-if="activeTab === 'permissions' && isCompanyAdmin">
          <SitePermissionsPanel :site-id="siteId" />
          <SitePurchaseRequestApprovalLevelsPanel :site-id="siteId" />
        </template>
      </main>
    </template>
    </div>
  </div>
</template>
