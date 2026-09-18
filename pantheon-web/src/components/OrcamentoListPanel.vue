<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useOrcamentos, type Orcamento } from '../composables/useOrcamentos'
import { usePurchaseRequests, type PurchaseRequest } from '../composables/usePurchaseRequests'
import type { FornecedorInput } from '../composables/useFornecedores'
import FornecedorPicker from './FornecedorPicker.vue'
import StatusBadge from './StatusBadge.vue'
import { vDatePicker } from '../lib/datePicker'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const router = useRouter()
const { listOrcamentos, createOrcamento } = useOrcamentos()
const { listPurchaseRequests } = usePurchaseRequests()

const PAGE_SIZE = 12

const orcamentos = ref<Orcamento[]>([])
const purchaseRequests = ref<PurchaseRequest[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const page = ref(0)
const loading = ref(false)
const creating = ref(false)
const errorMessage = ref('')
const showFornecedorPicker = ref(false)

const showFilterPanel = ref(false)
const dateFilter = ref('')
const purchaseRequestFilter = ref('')
const supplierFilter = ref('')
const hasActiveFilter = ref(false)

async function load() {
  loading.value = true
  try {
    const result = await listOrcamentos(props.siteId, {
      date: dateFilter.value || undefined,
      purchaseRequestId: purchaseRequestFilter.value || undefined,
      supplier: supplierFilter.value || undefined,
      page: page.value,
      size: PAGE_SIZE,
    })
    orcamentos.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  hasActiveFilter.value = !!(dateFilter.value || purchaseRequestFilter.value || supplierFilter.value)
  page.value = 0
  showFilterPanel.value = false
  load()
}

function clearFilters() {
  dateFilter.value = ''
  purchaseRequestFilter.value = ''
  supplierFilter.value = ''
  hasActiveFilter.value = false
  page.value = 0
  load()
}

function goToPage(target: number) {
  if (target < 0 || target >= totalPages.value) return
  page.value = target
  load()
}

async function onCreateConfirmed(fornecedor: FornecedorInput) {
  errorMessage.value = ''
  creating.value = true
  try {
    const orcamento = await createOrcamento(props.siteId, [], fornecedor)
    router.push(`/orcamentos/${orcamento.id}`)
  } catch {
    errorMessage.value = t('orcamento.error')
  } finally {
    creating.value = false
    showFornecedorPicker.value = false
  }
}

watch(() => props.siteId, () => {
  page.value = 0
  load()
})

onMounted(async () => {
  const prPage = await listPurchaseRequests(props.siteId, { size: 100 })
  purchaseRequests.value = prPage.content
  await load()
})
</script>

<template>
  <section class="card card-pad">
    <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-2">
        <div class="relative">
          <button type="button" class="btn-secondary" :class="{ 'ring-2 ring-blueprint-500/30': hasActiveFilter }" @click="showFilterPanel = !showFilterPanel">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 4h18M6 8h12M10 12h4" />
            </svg>
            {{ t('orcamento.filter.button') }}
          </button>
          <div v-if="showFilterPanel" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-4 shadow-lg" @click.stop>
            <div class="mb-3">
              <label class="field-label">{{ t('orcamento.dateFilterLabel') }}</label>
              <input v-model="dateFilter" v-date-picker type="date" class="field-input" />
            </div>
            <div class="mb-3">
              <label class="field-label">{{ t('orcamento.supplierFilterLabel') }}</label>
              <input v-model="supplierFilter" type="text" :placeholder="t('orcamento.supplierFilterPlaceholder')" class="field-input" />
            </div>
            <div class="mb-4">
              <label class="field-label">{{ t('orcamento.purchaseRequestFilterLabel') }}</label>
              <select v-model="purchaseRequestFilter" class="field-input">
                <option value="">—</option>
                <option v-for="pr in purchaseRequests" :key="pr.id" :value="pr.id">{{ pr.name }}</option>
              </select>
            </div>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-ghost px-3 py-1.5 text-xs" @click="clearFilters">{{ t('orcamento.clearFilterButton') }}</button>
              <button type="button" class="btn-primary px-3 py-1.5 text-xs" @click="applyFilters">{{ t('orcamento.filter.apply') }}</button>
            </div>
          </div>
        </div>
        <button v-if="!showFornecedorPicker" type="button" :disabled="creating" class="btn-primary" @click="showFornecedorPicker = true">
          {{ t('orcamento.newButton') }}
        </button>
      </div>
    </div>

    <div v-if="showFornecedorPicker" class="mb-5">
      <FornecedorPicker :site-id="siteId" @confirm="onCreateConfirmed" @cancel="showFornecedorPicker = false" />
    </div>

    <p v-if="errorMessage" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
    <p v-if="!loading && orcamentos.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.empty') }}</p>
    <template v-else>
      <ul class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <li v-for="orcamento in orcamentos" :key="orcamento.id">
          <router-link :to="`/orcamentos/${orcamento.id}`" class="flex flex-col gap-1.5 rounded-lg border border-steel-200 px-4 py-3 text-sm transition hover:bg-steel-50 dark:border-steel-700 dark:hover:bg-steel-700">
            <div class="flex items-center justify-between">
              <span class="font-medium text-steel-800 dark:text-steel-50">{{ orcamento.fornecedorNome }}</span>
              <StatusBadge kind="orcamento" :status="orcamento.status" />
            </div>
            <span class="text-xs text-steel-500 dark:text-steel-400">
              {{ t('orcamento.label') }} #{{ orcamento.id.slice(0, 8) }}
              <span v-if="orcamento.sourcePurchaseRequestName"> · {{ orcamento.sourcePurchaseRequestName }}</span>
            </span>
          </router-link>
        </li>
      </ul>

      <div v-if="totalPages > 1" class="mt-5 flex items-center justify-between gap-3 border-t border-steel-200 pt-4 dark:border-steel-700">
        <p class="text-xs text-steel-500 dark:text-steel-400">
          {{ t('orcamento.pagination.summary', { page: page + 1, totalPages, totalElements }) }}
        </p>
        <div class="flex gap-2">
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page === 0" @click="goToPage(page - 1)">
            {{ t('orcamento.pagination.previous') }}
          </button>
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
            {{ t('orcamento.pagination.next') }}
          </button>
        </div>
      </div>
    </template>
  </section>
</template>
