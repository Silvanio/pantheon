<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  usePurchaseRequests,
  type PurchaseRequest,
  type PurchaseRequestItemCreationData,
} from '../composables/usePurchaseRequests'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listPurchaseRequests, createPurchaseRequest } = usePurchaseRequests()

const purchaseRequests = ref<PurchaseRequest[]>([])
const loading = ref(false)
const dateFilter = ref('')

const showForm = ref(false)
const submitting = ref(false)
const formError = ref('')
const rows = ref<PurchaseRequestItemCreationData[]>([{ name: '', type: null, quantity: '', unit: null }])

async function load() {
  loading.value = true
  try {
    purchaseRequests.value = await listPurchaseRequests(props.siteId, dateFilter.value || undefined)
  } finally {
    loading.value = false
  }
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
    await load()
  } catch {
    formError.value = t('purchaseRequests.form.error')
  } finally {
    submitting.value = false
  }
}

watch(dateFilter, load)

onMounted(load)
</script>

<template>
  <section class="card card-pad">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary" @click="showForm = !showForm">
        {{ t('purchaseRequests.newButton') }}
      </button>
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

    <div class="mb-4 flex flex-wrap items-end gap-3">
      <div>
        <label class="field-label">{{ t('purchaseRequests.dateFilterLabel') }}</label>
        <input v-model="dateFilter" type="date" class="field-input" />
      </div>
      <button v-if="dateFilter" type="button" class="btn-secondary px-3 py-1.5 text-xs" @click="dateFilter = ''">
        {{ t('purchaseRequests.clearFilterButton') }}
      </button>
    </div>

    <p v-if="!loading && purchaseRequests.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('purchaseRequests.empty') }}
    </p>
    <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <li v-for="pr in purchaseRequests" :key="pr.id">
        <router-link
          :to="`/purchase-requests/${pr.id}`"
          class="block rounded-lg border border-steel-200 px-4 py-3 text-sm transition hover:bg-steel-50 dark:border-steel-700 dark:hover:bg-steel-700"
        >
          <span class="font-medium text-steel-800 dark:text-steel-50">{{ pr.name }}</span>
        </router-link>
      </li>
    </ul>
  </section>
</template>
