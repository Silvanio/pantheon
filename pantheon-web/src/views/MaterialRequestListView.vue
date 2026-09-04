<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useMaterialRequests, type MaterialRequest, type MaterialRequestStatus } from '../composables/useMaterialRequests'
import { useEquipmentMaterials, type MaterialItem } from '../composables/useEquipmentMaterials'

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
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
        <button type="button" class="text-sm text-blueprint-600 dark:text-blueprint-400" @click="router.push(`/sites/${siteId}`)">
          ← {{ t('materialRequests.list.back') }}
        </button>
      </div>
    </header>
    <main class="mx-auto max-w-4xl space-y-6 px-6 py-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ t('materialRequests.list.title') }}</h1>
          <p class="mt-1 text-steel-500 dark:text-steel-400">{{ t('materialRequests.list.subtitle') }}</p>
        </div>
        <button
          type="button"
          class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
          @click="showForm = !showForm"
        >
          {{ t('materialRequests.list.newButton') }}
        </button>
      </div>

      <form
        v-if="showForm"
        class="space-y-3 rounded-xl border border-steel-200 bg-white p-6 dark:border-steel-700 dark:bg-steel-800"
        @submit.prevent="onSubmit"
      >
        <div v-for="(item, index) in items" :key="index" class="flex flex-wrap items-end gap-2">
          <select v-model="item.materialId" required class="rounded-md border border-steel-300 bg-white px-3 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50">
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
            class="w-32 rounded-md border border-steel-300 bg-white px-3 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
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
          <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
            {{ t('materialRequests.form.submit') }}
          </button>
          <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">
            {{ t('materialRequests.form.cancel') }}
          </button>
        </div>
      </form>

      <div class="flex items-center gap-2">
        <label class="text-sm text-steel-600 dark:text-steel-300">{{ t('materialRequests.list.filterLabel') }}</label>
        <select v-model="statusFilter" class="rounded-md border border-steel-300 bg-white px-2 py-1 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" @change="load">
          <option value="">{{ t('materialRequests.list.filterAll') }}</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ t(`materialRequests.status.${s}`) }}</option>
        </select>
      </div>

      <p v-if="!loading && requests.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
        {{ t('materialRequests.list.empty') }}
      </p>
      <ul v-else class="space-y-2">
        <li v-for="request in requests" :key="request.id">
          <router-link
            :to="{ path: `/material-requests/${request.id}` }"
            class="flex items-center justify-between rounded-md border border-steel-200 bg-white px-4 py-3 transition hover:bg-steel-50 dark:border-steel-700 dark:bg-steel-800 dark:hover:bg-steel-700"
          >
            <span class="font-medium text-steel-800 dark:text-steel-50">{{ t('materialRequests.list.requestLabel') }} #{{ request.id.slice(0, 8) }}</span>
            <span class="text-sm text-steel-500 dark:text-steel-400">{{ t(`materialRequests.status.${request.status}`) }}</span>
          </router-link>
        </li>
      </ul>
    </main>
  </div>
</template>
