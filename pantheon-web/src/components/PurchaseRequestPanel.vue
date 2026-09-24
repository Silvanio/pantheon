<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  usePurchaseRequests,
  type PurchaseRequest,
  type PurchaseRequestStatus,
} from '../composables/usePurchaseRequests'
import { useSiteMembers } from '../composables/useSiteMembers'
import { vDatePicker } from '../lib/datePicker'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ siteId: string; canManage: boolean }>()

const { t } = useI18n()
const router = useRouter()
const { listPurchaseRequests, createPurchaseRequest } = usePurchaseRequests()
const { listMembers } = useSiteMembers()

const PAGE_SIZE_OPTIONS = [1, 5, 10] as const
const STATUSES: PurchaseRequestStatus[] = ['INICIADO', 'ORCADO', 'CONFERIDO', 'CONCLUIDO']

const purchaseRequests = ref<PurchaseRequest[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const page = ref(0)
const pageSize = ref<number>(10)
const loading = ref(false)

const showFilterPanel = ref(false)
const dateFilter = ref('')
const statusFilter = ref<PurchaseRequestStatus | ''>('')
const hasActiveFilter = ref(false)

const creating = ref(false)
const createError = ref('')

const memberNames = ref<Record<string, string>>({})

const stats = ref<{ awaitingApproval: number; budgeting: number; approved: number; completed: number } | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await listPurchaseRequests(props.siteId, {
      date: dateFilter.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      size: pageSize.value,
    })
    purchaseRequests.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } finally {
    loading.value = false
  }
}

async function loadMemberNames() {
  try {
    const members = await listMembers(props.siteId)
    memberNames.value = Object.fromEntries(
      members.filter((m) => m.userId).map((m) => [m.userId as string, m.displayName || m.email || '—']),
    )
  } catch {
    memberNames.value = {}
  }
}

// Real, cheaply-computable buckets derived from status + submittedAt — no per-item approval-step
// fetch (that would need one detail call per row). "Aguardando aprovação" = ORCADO and already
// submitted; "Em orçamento" = ORCADO but not yet submitted.
async function loadStats() {
  stats.value = null
  const [orcadoPage, conferidoPage, concluidoPage] = await Promise.all([
    listPurchaseRequests(props.siteId, { status: 'ORCADO', size: 100 }),
    listPurchaseRequests(props.siteId, { status: 'CONFERIDO', size: 1 }),
    listPurchaseRequests(props.siteId, { status: 'CONCLUIDO', size: 1 }),
  ])
  const awaitingApproval = orcadoPage.content.filter((pr) => pr.submittedAt).length
  const budgeting = orcadoPage.content.filter((pr) => !pr.submittedAt).length
  stats.value = {
    awaitingApproval,
    budgeting,
    approved: conferidoPage.totalElements,
    completed: concluidoPage.totalElements,
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

function onPageSizeChange() {
  page.value = 0
  load()
}

async function onCreate() {
  createError.value = ''
  creating.value = true
  try {
    const created = await createPurchaseRequest(props.siteId, [])
    router.push(`/purchase-requests/${created.id}`)
  } catch {
    createError.value = t('purchaseRequests.form.error')
  } finally {
    creating.value = false
  }
}

function stageLabel(pr: PurchaseRequest): string {
  if (pr.status === 'INICIADO') return t('purchaseRequests.stage.notBudgeted')
  if (pr.status === 'ORCADO') return pr.submittedAt ? t('purchaseRequests.stage.awaitingApproval') : t('purchaseRequests.stage.budgeting')
  if (pr.status === 'CONFERIDO') return t('purchaseRequests.stage.approved')
  return t('purchaseRequests.stage.completed')
}

function supplierSummary(pr: PurchaseRequest): string {
  if (pr.linkedOrcamentos.length === 0) return '—'
  return pr.linkedOrcamentos[0].fornecedorNome
}

function allSuppliers(pr: PurchaseRequest): string {
  return pr.linkedOrcamentos.map((o) => o.fornecedorNome).join(', ')
}

const dateFormatter = new Intl.DateTimeFormat('pt-BR')
function formatDate(value: string): string {
  return dateFormatter.format(new Date(value))
}

watch(
  () => props.siteId,
  () => {
    page.value = 0
    load()
    loadMemberNames()
    loadStats()
  },
)

onMounted(() => {
  load()
  loadMemberNames()
  loadStats()
})
</script>

<template>
  <section class="space-y-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
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
        <button v-if="canManage" type="button" :disabled="creating" class="btn-primary" @click="onCreate">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="h-3.5 w-3.5">
            <path stroke-linecap="round" d="M12 5v14M5 12h14" />
          </svg>
          {{ t('purchaseRequests.newButton') }}
        </button>
      </div>
    </div>

    <p v-if="createError" class="text-sm text-safety-600 dark:text-safety-500">{{ createError }}</p>

    <div class="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
      <div class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
        <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.stats.awaitingApproval') }}</p>
        <p class="text-2xl font-extrabold text-amber-600">{{ stats?.awaitingApproval ?? '—' }}</p>
      </div>
      <div class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
        <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.stats.budgeting') }}</p>
        <p class="text-2xl font-extrabold text-blueprint-600">{{ stats?.budgeting ?? '—' }}</p>
      </div>
      <div class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
        <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.stats.approved') }}</p>
        <p class="text-2xl font-extrabold text-emerald-600">{{ stats?.approved ?? '—' }}</p>
      </div>
      <div class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
        <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.stats.completed') }}</p>
        <p class="text-2xl font-extrabold text-steel-700 dark:text-steel-300">{{ stats?.completed ?? '—' }}</p>
      </div>
    </div>

    <p v-if="!loading && purchaseRequests.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('purchaseRequests.empty') }}
    </p>
    <template v-else>
      <div class="overflow-hidden rounded-2xl border border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-900">
        <table class="w-full border-collapse">
          <thead>
            <tr>
              <th class="pb-3 pl-5 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.table.name') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.table.status') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.table.stage') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.table.suppliers') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.table.createdBy') }}</th>
              <th class="pb-3 pr-5 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.table.date') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="pr in purchaseRequests"
              :key="pr.id"
              class="group cursor-pointer border-t border-steel-100 transition hover:bg-steel-50 dark:border-steel-800 dark:hover:bg-steel-800/60"
              @click="router.push(`/purchase-requests/${pr.id}`)"
            >
              <td class="py-3.5 pl-5 text-[13.5px] font-bold text-steel-800 dark:text-steel-50">{{ pr.name }}</td>
              <td class="py-3.5"><StatusBadge kind="purchaseRequest" :status="pr.status" /></td>
              <td class="py-3.5 text-[13px] text-steel-600 dark:text-steel-300">{{ stageLabel(pr) }}</td>
              <td class="py-3.5 text-[13px] text-steel-600 dark:text-steel-300">
                <span :title="pr.linkedOrcamentos.length > 1 ? allSuppliers(pr) : undefined">
                  {{ supplierSummary(pr) }}
                  <span v-if="pr.linkedOrcamentos.length > 1" class="text-steel-400">+{{ pr.linkedOrcamentos.length - 1 }}</span>
                </span>
              </td>
              <td class="py-3.5 text-[13px] text-steel-500 dark:text-steel-400">{{ memberNames[pr.createdBy] ?? '—' }}</td>
              <td class="py-3.5 pr-5 text-[13px] text-steel-500 dark:text-steel-400">{{ formatDate(pr.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-between gap-3 border-t border-steel-200 pt-4 dark:border-steel-700">
        <div class="flex items-center gap-2">
          <label class="text-xs text-steel-500 dark:text-steel-400">{{ t('common.pagination.pageSizeLabel') }}</label>
          <select v-model.number="pageSize" class="field-input w-auto py-1 text-xs" @change="onPageSizeChange">
            <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">{{ size }}</option>
          </select>
        </div>
        <div v-if="totalPages > 1" class="flex items-center gap-3">
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
      </div>
    </template>
  </section>
</template>
