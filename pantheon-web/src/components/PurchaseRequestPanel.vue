<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  usePurchaseRequests,
  type PurchaseRequest,
  type PurchaseRequestItemCreationData,
  type PurchaseRequestStatus,
} from '../composables/usePurchaseRequests'
import { vDatePicker } from '../lib/datePicker'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listPurchaseRequests, createPurchaseRequest } = usePurchaseRequests()

const PAGE_SIZE = 12
const STATUSES: PurchaseRequestStatus[] = ['INICIADO', 'ORCADO', 'CONFERIDO', 'CONCLUIDO']

const purchaseRequests = ref<PurchaseRequest[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const page = ref(0)
const loading = ref(false)

const showFilterPanel = ref(false)
const dateFilter = ref('')
const statusFilter = ref<PurchaseRequestStatus | ''>('')
const hasActiveFilter = ref(false)

const showForm = ref(false)
const submitting = ref(false)
const formError = ref('')
const rows = ref<PurchaseRequestItemCreationData[]>([{ name: '', type: null, quantity: '', unit: null }])

async function load() {
  loading.value = true
  try {
    const result = await listPurchaseRequests(props.siteId, {
      date: dateFilter.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    purchaseRequests.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  hasActiveFilter.value = !!(dateFilter.value || statusFilter.value)
  page.value = 0
  showFilterPanel.value = false
  load()
}

function clearFilters() {
  dateFilter.value = ''
  statusFilter.value = ''
  hasActiveFilter.value = false
  page.value = 0
  load()
}

function goToPage(target: number) {
  if (target < 0 || target >= totalPages.value) return
  page.value = target
  load()
}

function addRow() {
  rows.value.push({ name: '', type: null, quantity: '', unit: null })
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
}

async function onSubmitForm() {
  formError.value = ''
  submitting.value = true
  try {
    const items = rows.value
      .filter((r) => r.name.trim() && r.quantity)
      .map((r) => ({ name: r.name.trim(), type: r.type || null, quantity: r.quantity, unit: r.unit || null }))
    if (items.length === 0) {
      formError.value = t('purchaseRequests.form.error')
      return
    }
    await createPurchaseRequest(props.siteId, items)
    rows.value = [{ name: '', type: null, quantity: '', unit: null }]
    showForm.value = false
    page.value = 0
    await load()
  } catch {
    formError.value = t('purchaseRequests.form.error')
  } finally {
    submitting.value = false
  }
}

watch(() => props.siteId, () => {
  page.value = 0
  load()
})

onMounted(load)
</script>

<template>
  <section class="card card-pad">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <div class="relative">
          <button type="button" class="btn-secondary" :class="{ 'ring-2 ring-blueprint-500/30': hasActiveFilter }" @click="showFilterPanel = !showFilterPanel">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 4h18M6 8h12M10 12h4" />
            </svg>
            {{ t('purchaseRequests.filter.button') }}
          </button>
          <div v-if="showFilterPanel" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-4 shadow-lg" @click.stop>
            <div class="mb-3">
              <label class="field-label">{{ t('purchaseRequests.filter.statusLabel') }}</label>
              <select v-model="statusFilter" class="field-input">
                <option value="">{{ t('purchaseRequests.filter.allStatuses') }}</option>
                <option v-for="status in STATUSES" :key="status" :value="status">{{ t(`purchaseRequests.status.${status}`) }}</option>
              </select>
            </div>
            <div class="mb-4">
              <label class="field-label">{{ t('purchaseRequests.filter.dateLabel') }}</label>
              <input v-model="dateFilter" v-date-picker type="date" class="field-input" />
            </div>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-ghost px-3 py-1.5 text-xs" @click="clearFilters">{{ t('purchaseRequests.filter.clear') }}</button>
              <button type="button" class="btn-primary px-3 py-1.5 text-xs" @click="applyFilters">{{ t('purchaseRequests.filter.apply') }}</button>
            </div>
          </div>
        </div>
        <button type="button" class="btn-primary" @click="showForm = !showForm">
          {{ t('purchaseRequests.newButton') }}
        </button>
      </div>
    </div>

    <form v-if="showForm" class="mb-5 space-y-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmitForm">
      <div v-for="(row, index) in rows" :key="index" class="grid grid-cols-1 gap-3 sm:grid-cols-5 sm:items-end">
        <div class="sm:col-span-2">
          <label class="field-label">{{ t('purchaseRequests.form.name') }}</label>
          <input v-model="row.name" type="text" required class="field-input" />
        </div>
        <div>
          <label class="field-label">{{ t('purchaseRequests.form.type') }}</label>
          <input v-model="row.type" type="text" class="field-input" />
        </div>
        <div>
          <label class="field-label">{{ t('purchaseRequests.form.quantity') }}</label>
          <input v-model="row.quantity" type="number" step="0.001" min="0" required class="field-input" />
        </div>
        <div class="flex items-end gap-2">
          <div class="flex-1">
            <label class="field-label">{{ t('purchaseRequests.form.unit') }}</label>
            <input v-model="row.unit" type="text" class="field-input" />
          </div>
          <button v-if="rows.length > 1" type="button" class="btn-ghost px-2 py-1.5 text-xs" @click="removeRow(index)">
            {{ t('purchaseRequests.removeRowButton') }}
          </button>
        </div>
      </div>
      <button type="button" class="text-sm font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="addRow">
        {{ t('purchaseRequests.addRowButton') }}
      </button>
      <p v-if="formError" class="text-sm text-safety-600 dark:text-safety-500">{{ formError }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="btn-primary">{{ t('purchaseRequests.form.submit') }}</button>
        <button type="button" class="btn-secondary" @click="showForm = false">{{ t('purchaseRequests.form.cancel') }}</button>
      </div>
    </form>

    <p v-if="!loading && purchaseRequests.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('purchaseRequests.empty') }}
    </p>
    <template v-else>
      <ul class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <li v-for="pr in purchaseRequests" :key="pr.id">
          <router-link
            :to="`/purchase-requests/${pr.id}`"
            class="flex flex-col gap-2 rounded-lg border border-steel-200 px-4 py-3 text-sm transition hover:bg-steel-50 dark:border-steel-700 dark:hover:bg-steel-700"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="min-w-0 truncate font-medium text-steel-800 dark:text-steel-50">{{ pr.name }}</span>
              <StatusBadge kind="purchaseRequest" :status="pr.status" />
            </div>
            <span class="text-xs text-steel-500 dark:text-steel-400">
              {{ t('purchaseRequests.card.linkedOrcamentos', { count: pr.linkedOrcamentos.length }) }}
            </span>
          </router-link>
        </li>
      </ul>

      <div v-if="totalPages > 1" class="mt-5 flex items-center justify-between gap-3 border-t border-steel-200 pt-4 dark:border-steel-700">
        <p class="text-xs text-steel-500 dark:text-steel-400">
          {{ t('purchaseRequests.pagination.summary', { page: page + 1, totalPages, totalElements }) }}
        </p>
        <div class="flex gap-2">
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page === 0" @click="goToPage(page - 1)">
            {{ t('purchaseRequests.pagination.previous') }}
          </button>
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
            {{ t('purchaseRequests.pagination.next') }}
          </button>
        </div>
      </div>
    </template>
  </section>
</template>
