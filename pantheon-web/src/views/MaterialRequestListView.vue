<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMaterialRequests, type MaterialRequest, type MaterialRequestStatus } from '../composables/useMaterialRequests'
import { useEquipmentMaterials, type MaterialItem } from '../composables/useEquipmentMaterials'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { listRequests, createRequest } = useMaterialRequests()
const { listMaterials } = useEquipmentMaterials()

const siteId = route.params.siteId as string

const requests = ref<MaterialRequest[]>([])
const materialCatalog = ref<MaterialItem[]>([])
const loading = ref(false)
const statusFilter = ref<MaterialRequestStatus | ''>('')

const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const items = ref<{ materialId: string; requestedQuantity: string }[]>([{ materialId: '', requestedQuantity: '' }])

const statuses: MaterialRequestStatus[] = ['PENDING', 'APPROVED', 'REJECTED', 'PARTIALLY_RECEIVED', 'RECEIVED']

async function load() {
  loading.value = true
  try {
    requests.value = await listRequests(siteId, statusFilter.value)
  } finally {
    loading.value = false
  }
}

function addItemRow() {
  items.value.push({ materialId: '', requestedQuantity: '' })
}

function removeItemRow(index: number) {
  items.value.splice(index, 1)
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createRequest(siteId, items.value)
    items.value = [{ materialId: '', requestedQuantity: '' }]
    showForm.value = false
    await load()
  } catch {
    errorMessage.value = t('materialRequests.form.error')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  materialCatalog.value = await listMaterials(siteId)
  await load()
})

const statusBadgeClass: Record<MaterialRequestStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
  APPROVED: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
  REJECTED: 'bg-safety-500/10 text-safety-600 dark:text-safety-500',
  PARTIALLY_RECEIVED: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
  RECEIVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300',
}
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.push(`/sites/${siteId}`)">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('materialRequests.list.back') }}
        </button>
      </template>
    </AppHeader>
    <main class="app-container max-w-5xl! space-y-6 py-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-3xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('materialRequests.list.title') }}</h1>
          <p class="mt-1.5 text-steel-500 dark:text-steel-400">{{ t('materialRequests.list.subtitle') }}</p>
        </div>
        <button type="button" class="btn-primary" @click="showForm = !showForm">
          {{ t('materialRequests.list.newButton') }}
        </button>
      </div>

      <form v-if="showForm" class="card card-pad space-y-3" @submit.prevent="onSubmit">
        <div v-for="(item, index) in items" :key="index" class="flex flex-wrap items-end gap-2">
          <select v-model="item.materialId" required class="field-input flex-1">
            <option value="" disabled>{{ t('materialRequests.form.material') }}</option>
            <option v-for="m in materialCatalog" :key="m.id" :value="m.id">{{ m.name }} ({{ m.unit }})</option>
          </select>
          <input
            v-model="item.requestedQuantity"
            type="number"
            step="0.001"
            min="0"
            required
            :placeholder="t('materialRequests.form.quantity')"
            class="field-input w-32"
          />
          <button
            v-if="items.length > 1"
            type="button"
            class="text-xs font-medium text-safety-600 hover:underline dark:text-safety-500"
            @click="removeItemRow(index)"
          >
            {{ t('materialRequests.form.removeItemButton') }}
          </button>
        </div>
        <button type="button" class="text-sm font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="addItemRow">
          + {{ t('materialRequests.form.addItemButton') }}
        </button>
        <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="btn-primary">
            {{ t('materialRequests.form.submit') }}
          </button>
          <button type="button" class="btn-secondary" @click="showForm = false">
            {{ t('materialRequests.form.cancel') }}
          </button>
        </div>
      </form>

      <div class="flex items-center gap-2">
        <label class="text-sm text-steel-600 dark:text-steel-300">{{ t('materialRequests.list.filterLabel') }}</label>
        <select v-model="statusFilter" class="field-input w-auto py-1.5" @change="load">
          <option value="">{{ t('materialRequests.list.filterAll') }}</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ t(`materialRequests.status.${s}`) }}</option>
        </select>
      </div>

      <p v-if="!loading && requests.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
        {{ t('materialRequests.list.empty') }}
      </p>
      <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <li v-for="request in requests" :key="request.id">
          <router-link :to="{ path: `/material-requests/${request.id}` }" class="card flex items-center justify-between px-4 py-3.5 transition hover:shadow-md">
            <span class="font-medium text-steel-800 dark:text-steel-50">{{ t('materialRequests.list.requestLabel') }} #{{ request.id.slice(0, 8) }}</span>
            <span class="badge" :class="statusBadgeClass[request.status]">{{ t(`materialRequests.status.${request.status}`) }}</span>
          </router-link>
        </li>
      </ul>
    </main>
  </div>
</template>
