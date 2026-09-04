<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useProjects, type ConstructionSite, type SiteStatus } from '../composables/useProjects'
import EquipmentPanel from './EquipmentPanel.vue'
import MaterialsPanel from './MaterialsPanel.vue'

const props = defineProps<{ projectId: string }>()
const emit = defineEmits<{ (e: 'updated', sites: ConstructionSite[]): void }>()

const { t } = useI18n()
const { listConstructionSites, createConstructionSite, updateConstructionSiteStatus } = useProjects()

const sites = ref<ConstructionSite[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const expandedSiteId = ref<string | null>(null)

function toggleExpanded(siteId: string) {
  expandedSiteId.value = expandedSiteId.value === siteId ? null : siteId
}

const name = ref('')
const address = ref('')
const startDate = ref('')
const expectedEndDate = ref('')

const statuses: SiteStatus[] = ['PLANNING', 'IN_PROGRESS', 'PAUSED', 'COMPLETED']

async function loadSites() {
  loading.value = true
  try {
    sites.value = await listConstructionSites(props.projectId)
    emit('updated', sites.value)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createConstructionSite(props.projectId, {
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
    errorMessage.value = t('constructionSites.form.error')
  } finally {
    submitting.value = false
  }
}

async function onStatusChange(site: ConstructionSite, status: SiteStatus) {
  try {
    await updateConstructionSiteStatus(site.id, status)
    await loadSites()
  } catch {
    errorMessage.value = t('constructionSites.status.updateError')
  }
}

onMounted(loadSites)
</script>

<template>
  <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('constructionSites.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('constructionSites.subtitle') }}</p>
      </div>
      <button
        type="button"
        class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        @click="showForm = !showForm"
      >
        {{ t('constructionSites.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-4 space-y-3 rounded-md border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.name') }}</label>
        <input v-model="name" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      </div>
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.address') }}</label>
        <input v-model="address" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.startDate') }}</label>
          <input v-model="startDate" type="date" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
        <div>
          <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('constructionSites.form.expectedEndDate') }}</label>
          <input v-model="expectedEndDate" type="date" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        </div>
      </div>
      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600">
          {{ t('constructionSites.form.submit') }}
        </button>
        <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700" @click="showForm = false">
          {{ t('constructionSites.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="!loading && sites.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('constructionSites.empty') }}
    </p>

    <ul v-else class="space-y-2">
      <li v-for="site in sites" :key="site.id" class="rounded-md border border-steel-200 dark:border-steel-700">
        <div class="flex items-center justify-between px-4 py-3">
          <button type="button" class="text-left" @click="toggleExpanded(site.id)">
            <p class="font-medium text-steel-800 hover:underline dark:text-steel-50">{{ site.name }}</p>
            <p class="text-sm text-steel-500 dark:text-steel-400">{{ site.address }}</p>
          </button>
          <div class="flex items-center gap-2">
            <label class="sr-only" :for="`status-${site.id}`">{{ t('constructionSites.status.updateLabel') }}</label>
            <select
              :id="`status-${site.id}`"
              :value="site.status"
              class="rounded-md border border-steel-300 bg-white px-2 py-1 text-sm text-steel-700 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-100"
              @change="onStatusChange(site, ($event.target as HTMLSelectElement).value as SiteStatus)"
            >
              <option v-for="status in statuses" :key="status" :value="status">
                {{ t(`constructionSites.status.${status}`) }}
              </option>
            </select>
            <button
              type="button"
              class="rounded-md border border-steel-300 px-2 py-1 text-xs font-medium text-steel-600 hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700"
              @click="toggleExpanded(site.id)"
            >
              {{ expandedSiteId === site.id ? '▲' : '▼' }} {{ t('equipment.toggleLabel') }} / {{ t('materials.toggleLabel') }}
            </button>
          </div>
        </div>
        <div v-if="expandedSiteId === site.id" class="space-y-3 border-t border-steel-200 p-4 dark:border-steel-700">
          <div class="flex gap-2">
            <router-link
              :to="{ path: `/construction-sites/${site.id}/daily-reports`, query: { projectId: props.projectId } }"
              class="inline-block rounded-md bg-blueprint-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
            >
              {{ t('dailyReports.toggleLabel') }}
            </router-link>
            <router-link
              :to="{ path: `/construction-sites/${site.id}/material-requests`, query: { projectId: props.projectId } }"
              class="inline-block rounded-md border border-steel-300 px-3 py-1.5 text-xs font-medium text-steel-600 transition hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700"
            >
              {{ t('materialRequests.toggleLabel') }}
            </router-link>
          </div>
          <EquipmentPanel :site-id="site.id" />
          <MaterialsPanel :site-id="site.id" />
        </div>
      </li>
    </ul>
  </section>
</template>
