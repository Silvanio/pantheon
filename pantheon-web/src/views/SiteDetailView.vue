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
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
        <button type="button" class="text-sm text-blueprint-600 dark:text-blueprint-400" @click="router.push('/')">
          ← {{ t('siteDetail.back') }}
        </button>
      </div>
    </header>

    <main v-if="site" class="mx-auto max-w-5xl space-y-6 px-6 py-8">
      <div class="flex items-start gap-4">
        <div class="w-40 shrink-0">
          <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
          <label class="mt-1 block text-center text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">
            {{ t('siteDetail.changePhoto') }}
            <input type="file" accept="image/*" class="hidden" @change="onPhotoChange" />
          </label>
        </div>
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ site.name }}</h1>
          <p class="text-steel-500 dark:text-steel-400">{{ site.address }}</p>
          <p class="text-sm text-steel-500 dark:text-steel-400">{{ site.startDate }}</p>
        </div>
      </div>

      <nav class="flex flex-wrap gap-2 border-b border-steel-200 pb-2 dark:border-steel-700">
        <button
          type="button"
          class="rounded-md px-3 py-1.5 text-sm font-medium"
          :class="activeTab === 'team' ? 'bg-blueprint-600 text-white' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-700'"
          @click="activeTab = 'team'"
        >
          {{ t('siteDetail.tabs.team') }}
        </button>
        <router-link
          :to="`/construction-sites/${siteId}/daily-reports`"
          class="rounded-md px-3 py-1.5 text-sm font-medium text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-700"
        >
          {{ t('siteDetail.tabs.dailyReport') }}
        </router-link>
        <button
          type="button"
          class="rounded-md px-3 py-1.5 text-sm font-medium"
          :class="activeTab === 'projects' ? 'bg-blueprint-600 text-white' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-700'"
          @click="activeTab = 'projects'"
        >
          {{ t('siteDetail.tabs.projects') }}
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1.5 text-sm font-medium"
          :class="activeTab === 'materials' ? 'bg-blueprint-600 text-white' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-700'"
          @click="activeTab = 'materials'"
        >
          {{ t('siteDetail.tabs.materials') }}
        </button>
        <button
          type="button"
          disabled
          class="cursor-not-allowed rounded-md px-3 py-1.5 text-sm font-medium text-steel-400 dark:text-steel-600"
          :title="t('siteDetail.tabs.scheduleDisabled')"
        >
          {{ t('siteDetail.tabs.schedule') }}
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1.5 text-sm font-medium"
          :class="activeTab === 'permissions' ? 'bg-blueprint-600 text-white' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-700'"
          @click="activeTab = 'permissions'"
        >
          {{ t('siteDetail.tabs.permissions') }}
        </button>
      </nav>

      <SiteTeamPanel v-if="activeTab === 'team'" :site-id="siteId" />
      <SiteDocumentProjectsPanel v-if="activeTab === 'projects'" :site-id="siteId" />
      <template v-if="activeTab === 'materials'">
        <div>
          <router-link
            :to="`/construction-sites/${siteId}/material-requests`"
            class="inline-block rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500"
          >
            {{ t('materialRequests.toggleLabel') }}
          </router-link>
        </div>
        <EquipmentPanel :site-id="siteId" />
        <MaterialsPanel :site-id="siteId" />
      </template>
      <SitePermissionsPanel v-if="activeTab === 'permissions'" :site-id="siteId" />
    </main>
  </div>
</template>
