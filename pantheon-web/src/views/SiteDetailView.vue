<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useConstructionSites, type ConstructionSite } from '../composables/useConstructionSites'
import { useCompanies } from '../composables/useCompanies'
import { useSiteHeroCollapse } from '../composables/useSiteHeroCollapse'
import { useSitePermissions, type AccessLevel, type PermissionCapability } from '../composables/useSitePermissions'
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
import AppHeader from '../components/AppHeader.vue'
import ThemeToggle from '../components/ThemeToggle.vue'
import ProfileMenu from '../components/ProfileMenu.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getSite, updateSitePhoto } = useConstructionSites()
const { listMyCompanies } = useCompanies()
const { collapsed: heroCollapsed, toggle: toggleHero } = useSiteHeroCollapse()
const { getMyPermissions } = useSitePermissions()

const siteId = route.params.siteId as string
const site = ref<ConstructionSite | null>(null)
const loading = ref(false)
const isCompanyAdmin = ref(false)

type Tab = 'team' | 'dailyReport' | 'projects' | 'equipment' | 'purchaseRequests' | 'orcamentos' | 'tasks' | 'schedule' | 'permissions'
const activeTab = ref<Tab>('team')

// Tabs backed by a PermissionCapability can be hidden per member; 'schedule' (an unimplemented
// placeholder) and 'permissions' (gated by company-admin status, not a capability) are exempt.
const TAB_CAPABILITY: Partial<Record<Tab, PermissionCapability>> = {
  team: 'TEAM_MANAGE',
  dailyReport: 'DAILY_REPORT',
  projects: 'DOCUMENT_PROJECTS',
  equipment: 'EQUIPMENT',
  purchaseRequests: 'PURCHASE_REQUEST',
  orcamentos: 'ORCAMENTO_MANAGE',
  tasks: 'TASKS',
}
const TAB_ORDER: Tab[] = ['team', 'dailyReport', 'projects', 'equipment', 'purchaseRequests', 'orcamentos', 'tasks']

const myPermissions = ref<Record<PermissionCapability, AccessLevel> | null>(null)

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
    const firstVisible = TAB_ORDER.find(isTabVisible)
    if (firstVisible) activeTab.value = firstVisible
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
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.push('/')">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('siteDetail.back') }}
        </button>
        <template v-if="site && heroCollapsed">
          <span class="mx-1 h-5 w-px shrink-0 bg-steel-200 dark:bg-steel-700"></span>
          <h1 class="truncate text-sm font-semibold text-steel-800 dark:text-steel-100">{{ site.name }}</h1>
        </template>
      </template>
      <template #right>
        <ProfileMenu />
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
        <nav class="flex flex-wrap gap-1.5 border-b border-steel-200 pb-3 dark:border-steel-800">
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
            type="button"
            disabled
            class="cursor-not-allowed rounded-lg px-3.5 py-2 text-sm font-medium text-steel-400 dark:text-steel-600"
            :title="t('siteDetail.tabs.scheduleDisabled')"
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
        <template v-if="activeTab === 'permissions' && isCompanyAdmin">
          <SitePermissionsPanel :site-id="siteId" />
          <SitePurchaseRequestApprovalLevelsPanel :site-id="siteId" />
        </template>
      </main>
    </template>
  </div>
</template>
