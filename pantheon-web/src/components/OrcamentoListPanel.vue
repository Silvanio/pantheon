<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useOrcamentos, type Orcamento } from '../composables/useOrcamentos'
import { usePurchaseRequests, type PurchaseRequest } from '../composables/usePurchaseRequests'
import type { FornecedorInput } from '../composables/useFornecedores'
import FornecedorPicker from './FornecedorPicker.vue'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const router = useRouter()
const { listOrcamentos, createOrcamento } = useOrcamentos()
const { listPurchaseRequests } = usePurchaseRequests()

const orcamentos = ref<Orcamento[]>([])
const purchaseRequests = ref<PurchaseRequest[]>([])
const loading = ref(false)
const creating = ref(false)
const errorMessage = ref('')
const showFornecedorPicker = ref(false)

const dateFilter = ref('')
const purchaseRequestFilter = ref('')

const statusBadgeClass: Record<Orcamento['status'], string> = {
  DRAFT: 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200',
  IN_APPROVAL: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
  APPROVED: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
  COMPLETED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300',
}

async function load() {
  loading.value = true
  try {
    orcamentos.value = await listOrcamentos(props.siteId, {
      date: dateFilter.value || undefined,
      purchaseRequestId: purchaseRequestFilter.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  dateFilter.value = ''
  purchaseRequestFilter.value = ''
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

watch([dateFilter, purchaseRequestFilter], load)

onMounted(async () => {
  purchaseRequests.value = await listPurchaseRequests(props.siteId)
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
      <button v-if="!showFornecedorPicker" type="button" :disabled="creating" class="btn-primary" @click="showFornecedorPicker = true">
        {{ t('orcamento.newButton') }}
      </button>
    </div>

    <div v-if="showFornecedorPicker" class="mb-5">
      <FornecedorPicker :site-id="siteId" @confirm="onCreateConfirmed" @cancel="showFornecedorPicker = false" />
    </div>

    <div class="mb-5 flex flex-wrap items-end gap-3">
      <div>
        <label class="field-label">{{ t('orcamento.dateFilterLabel') }}</label>
        <input v-model="dateFilter" type="date" class="field-input" />
      </div>
      <div>
        <label class="field-label">{{ t('orcamento.purchaseRequestFilterLabel') }}</label>
        <select v-model="purchaseRequestFilter" class="field-input">
          <option value="">—</option>
          <option v-for="pr in purchaseRequests" :key="pr.id" :value="pr.id">{{ pr.name }}</option>
        </select>
      </div>
      <button
        v-if="dateFilter || purchaseRequestFilter"
        type="button"
        class="btn-secondary px-3 py-1.5 text-xs"
        @click="clearFilters"
      >
        {{ t('orcamento.clearFilterButton') }}
      </button>
    </div>

    <p v-if="errorMessage" class="mb-4 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
    <p v-if="!loading && orcamentos.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.empty') }}</p>
    <ul v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <li v-for="orcamento in orcamentos" :key="orcamento.id">
        <router-link :to="`/orcamentos/${orcamento.id}`" class="flex flex-col gap-1.5 rounded-lg border border-steel-200 px-4 py-3 text-sm transition hover:bg-steel-50 dark:border-steel-700 dark:hover:bg-steel-700">
          <div class="flex items-center justify-between">
            <span class="font-medium text-steel-800 dark:text-steel-50">{{ orcamento.fornecedorNome }}</span>
            <span class="badge" :class="statusBadgeClass[orcamento.status]">{{ t(`orcamento.status.${orcamento.status}`) }}</span>
          </div>
          <span class="text-xs text-steel-500 dark:text-steel-400">
            {{ t('orcamento.label') }} #{{ orcamento.id.slice(0, 8) }}
            <span v-if="orcamento.sourcePurchaseRequestName"> · {{ orcamento.sourcePurchaseRequestName }}</span>
          </span>
        </router-link>
      </li>
    </ul>
  </section>
</template>
