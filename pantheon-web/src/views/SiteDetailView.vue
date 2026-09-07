<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useConstructionSites, type ConstructionSite } from '../composables/useConstructionSites'
import SitePhoto from '../components/SitePhoto.vue'
import SiteTeamPanel from '../components/SiteTeamPanel.vue'
import SiteDocumentProjectsPanel from '../components/SiteDocumentProjectsPanel.vue'
import SitePermissionsPanel from '../components/SitePermissionsPanel.vue'
import EquipmentPanel from '../components/EquipmentPanel.vue'
import MaterialsPanel from '../components/MaterialsPanel.vue'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getSite, updateSitePhoto } = useConstructionSites()

const siteId = route.params.siteId as string
const site = ref<ConstructionSite | null>(null)
const loading = ref(false)

type Tab = 'team' | 'projects' | 'materials' | 'schedule' | 'permissions'
const activeTab = ref<Tab>('team')

async function load() {
  loading.value = true
  try {
    site.value = await getSite(siteId)
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
      </template>
    </AppHeader>

    <template v-if="site">
      <div class="relative h-56 w-full overflow-hidden sm:h-72">
        <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
        <div class="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent"></div>
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
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'team' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'team'"
          >
            {{ t('siteDetail.tabs.team') }}
          </button>
          <router-link
            :to="`/construction-sites/${siteId}/daily-reports`"
            class="rounded-lg px-3.5 py-2 text-sm font-medium text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800"
          >
            {{ t('siteDetail.tabs.dailyReport') }}
          </router-link>
          <button
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'projects' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'projects'"
          >
            {{ t('siteDetail.tabs.projects') }}
          </button>
          <button
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'materials' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'materials'"
          >
            {{ t('siteDetail.tabs.materials') }}
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
            type="button"
            class="rounded-lg px-3.5 py-2 text-sm font-medium transition"
            :class="activeTab === 'permissions' ? 'bg-blueprint-600 text-white shadow-sm' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="activeTab = 'permissions'"
          >
            {{ t('siteDetail.tabs.permissions') }}
          </button>
        </nav>

        <SiteTeamPanel v-if="activeTab === 'team'" :site-id="siteId" />
        <SiteDocumentProjectsPanel v-if="activeTab === 'projects'" :site-id="siteId" />
        <template v-if="activeTab === 'materials'">
          <div>
            <router-link :to="`/construction-sites/${siteId}/material-requests`" class="btn-primary inline-flex">
              {{ t('materialRequests.toggleLabel') }}
            </router-link>
          </div>
          <div class="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <EquipmentPanel :site-id="siteId" />
            <MaterialsPanel :site-id="siteId" />
          </div>
        </template>
        <SitePermissionsPanel v-if="activeTab === 'permissions'" :site-id="siteId" />
      </main>
    </template>
  </div>
</template>
