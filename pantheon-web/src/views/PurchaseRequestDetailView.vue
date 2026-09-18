<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  usePurchaseRequests,
  type PurchaseRequestDetail,
  type PurchaseRequestComparison,
  type PurchaseRequestInvoice,
  type PurchaseRequestStatus,
} from '../composables/usePurchaseRequests'
import { useMaterialDeliveries, type Material } from '../composables/useMaterialDeliveries'
import type { FornecedorInput } from '../composables/useFornecedores'
import FornecedorPicker from '../components/FornecedorPicker.vue'
import AppHeader from '../components/AppHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import PurchaseRequestComparisonTable from '../components/PurchaseRequestComparisonTable.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const {
  getPurchaseRequest,
  deletePurchaseRequest,
  convertToOrcamento,
  setItemSelection,
  getComparison,
  getSupplierPdfBlob,
  getSummaryPdfBlob,
  submitForApproval,
  approveStep,
  rejectStep,
  conclude,
  uploadInvoice,
  listInvoices,
  deleteInvoice,
  getInvoiceContentBlob,
} = usePurchaseRequests()
const { listMaterials, markDelivered, markChecked } = useMaterialDeliveries()

const purchaseRequestId = route.params.id as string
const detail = ref<PurchaseRequestDetail | null>(null)
const comparison = ref<PurchaseRequestComparison | null>(null)
const materials = ref<Material[]>([])
const invoices = ref<PurchaseRequestInvoice[]>([])
const loading = ref(false)
const loadError = ref('')

const STEPS: PurchaseRequestStatus[] = ['INICIADO', 'ORCADO', 'CONFERIDO', 'CONCLUIDO']

const selectedIds = ref<Set<string>>(new Set())
const showFornecedorPicker = ref(false)
const converting = ref(false)
const convertError = ref('')

const selectionError = ref('')
const printingOrcamentoId = ref<string | null>(null)
const printError = ref('')
const printingSummary = ref(false)
const summaryError = ref('')

const submitting = ref(false)
const submitError = ref('')
const approvalActionError = ref('')
const rejectReason = ref('')
const showRejectForm = ref(false)
const concluding = ref(false)
const concludeError = ref('')
const confirmingDelete = ref(false)
const deleting = ref(false)
const deleteError = ref('')

const materialActionError = ref('')
const photosByMaterial = ref<Record<string, File[]>>({})

const uploadingInvoice = ref(false)
const invoiceError = ref('')
const confirmingDeleteInvoiceId = ref<string | null>(null)

const items = computed(() => detail.value?.items ?? [])
const allSelected = computed(() => items.value.length > 0 && items.value.every((i) => selectedIds.value.has(i.id)))

const currentStepIndex = computed(() => (detail.value ? STEPS.indexOf(detail.value.purchaseRequest.status) : 0))

const currentCycle = computed(() => {
  if (!detail.value || detail.value.approvals.length === 0) return 0
  return Math.max(...detail.value.approvals.map((a) => a.cycleNumber))
})

const currentPendingApproval = computed(() => {
  if (!detail.value) return null
  return (
    detail.value.approvals
      .filter((a) => a.cycleNumber === currentCycle.value && a.status === 'PENDING')
      .sort((a, b) => a.stepOrder - b.stepOrder)[0] ?? null
  )
})

const approvalsByCycle = computed(() => {
  if (!detail.value) return []
  const cycles = new Map<number, typeof detail.value.approvals>()
  for (const approval of detail.value.approvals) {
    if (!cycles.has(approval.cycleNumber)) cycles.set(approval.cycleNumber, [])
    cycles.get(approval.cycleNumber)!.push(approval)
  }
  return Array.from(cycles.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([cycleNumber, approvals]) => ({
      cycleNumber,
      approvals: approvals.slice().sort((a, b) => a.stepOrder - b.stepOrder),
    }))
})

const canSubmit = computed(() => {
  if (!detail.value) return false
  if (detail.value.purchaseRequest.status !== 'ORCADO') return false
  if (detail.value.items.length === 0) return false
  return detail.value.items.every((item) => !!item.selectedOrcamentoLineItemId)
})

const canDelete = computed(() => detail.value?.purchaseRequest.status === 'INICIADO')
const canConclude = computed(() => detail.value?.purchaseRequest.status === 'CONFERIDO')
const isConcluded = computed(() => detail.value?.purchaseRequest.status === 'CONCLUIDO')
const selectionEditable = computed(() => detail.value?.purchaseRequest.status === 'ORCADO')

const selectionByItemId = computed(() => {
  const map = new Map<string, { supplierName: string; unitPrice: string | null }>()
  if (!comparison.value) return map
  for (const row of comparison.value.rows) {
    for (const cell of row.cells) {
      if (!cell.selected) continue
      const column = comparison.value.columns.find((c) => c.orcamentoId === cell.orcamentoId)
      if (column) map.set(row.itemId, { supplierName: column.supplierName, unitPrice: cell.unitPrice })
    }
  }
  return map
})

async function loadDetail() {
  detail.value = await getPurchaseRequest(purchaseRequestId)
}

async function loadComparison() {
  if (!detail.value || detail.value.purchaseRequest.linkedOrcamentos.length === 0) {
    comparison.value = null
    return
  }
  comparison.value = await getComparison(purchaseRequestId)
}

async function loadMaterials() {
  if (!detail.value || detail.value.purchaseRequest.status !== 'CONCLUIDO') {
    materials.value = []
    return
  }
  const lists = await Promise.all(
    detail.value.purchaseRequest.linkedOrcamentos.map((o) => listMaterials(detail.value!.purchaseRequest.constructionSiteId, o.id)),
  )
  materials.value = lists.flat()
}

async function loadInvoices() {
  invoices.value = await listInvoices(purchaseRequestId)
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    await loadDetail()
    await Promise.all([loadComparison(), loadMaterials(), loadInvoices()])
  } catch {
    loadError.value = t('purchaseRequests.loadError')
  } finally {
    loading.value = false
  }
}

function toggleSelection(itemId: string) {
  if (selectedIds.value.has(itemId)) {
    selectedIds.value.delete(itemId)
  } else {
    selectedIds.value.add(itemId)
  }
  selectedIds.value = new Set(selectedIds.value)
}

function toggleSelectAll() {
  selectedIds.value = allSelected.value ? new Set() : new Set(items.value.map((i) => i.id))
}

async function onConvertConfirmed(fornecedor: FornecedorInput) {
  convertError.value = ''
  converting.value = true
  try {
    const orcamento = await convertToOrcamento(purchaseRequestId, Array.from(selectedIds.value), fornecedor)
    router.push(`/orcamentos/${orcamento.id}`)
  } catch {
    convertError.value = t('purchaseRequests.convertError')
    showFornecedorPicker.value = false
  } finally {
    converting.value = false
  }
}

async function onSelectCell(itemId: string, orcamentoLineItemId: string | null) {
  selectionError.value = ''
  try {
    await setItemSelection(purchaseRequestId, itemId, orcamentoLineItemId)
    await loadDetail()
    await loadComparison()
  } catch {
    selectionError.value = t('purchaseRequests.comparison.selectError')
  }
}

async function onPrintPdf(orcamentoId: string) {
  printError.value = ''
  printingOrcamentoId.value = orcamentoId
  try {
    const { blob, filename } = await getSupplierPdfBlob(purchaseRequestId, orcamentoId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename ?? `pedido-${purchaseRequestId}-fornecedor-${orcamentoId}.pdf`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    printError.value = t('purchaseRequests.comparison.printError')
  } finally {
    printingOrcamentoId.value = null
  }
}

async function onPrintSummary() {
  summaryError.value = ''
  printingSummary.value = true
  try {
    const { blob, filename } = await getSummaryPdfBlob(purchaseRequestId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename ?? `resumo-pedido-${purchaseRequestId}.pdf`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    summaryError.value = t('purchaseRequests.comparison.summaryError')
  } finally {
    printingSummary.value = false
  }
}

async function onSubmitForApproval() {
  submitError.value = ''
  submitting.value = true
  try {
    await submitForApproval(purchaseRequestId)
    await load()
  } catch {
    submitError.value = t('purchaseRequests.submitError')
  } finally {
    submitting.value = false
  }
}

async function onApproveStep() {
  approvalActionError.value = ''
  try {
    await approveStep(purchaseRequestId)
    await load()
  } catch {
    approvalActionError.value = t('purchaseRequests.approvalError')
  }
}

async function onRejectStep() {
  approvalActionError.value = ''
  try {
    await rejectStep(purchaseRequestId, rejectReason.value)
    showRejectForm.value = false
    rejectReason.value = ''
    await load()
  } catch {
    approvalActionError.value = t('purchaseRequests.approvalError')
  }
}

async function onDelete() {
  deleteError.value = ''
  deleting.value = true
  try {
    await deletePurchaseRequest(purchaseRequestId)
    router.back()
  } catch {
    deleteError.value = t('purchaseRequests.deleteError')
    deleting.value = false
    confirmingDelete.value = false
  }
}

async function onUploadInvoice(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  invoiceError.value = ''
  uploadingInvoice.value = true
  try {
    await uploadInvoice(purchaseRequestId, file)
    await loadInvoices()
  } catch {
    invoiceError.value = t('purchaseRequests.invoices.uploadError')
  } finally {
    uploadingInvoice.value = false
  }
}

async function onDownloadInvoice(invoice: PurchaseRequestInvoice) {
  invoiceError.value = ''
  try {
    const { blob, filename } = await getInvoiceContentBlob(invoice.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename ?? invoice.originalName
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    invoiceError.value = t('purchaseRequests.invoices.downloadError')
  }
}

async function onDeleteInvoice(invoiceId: string) {
  invoiceError.value = ''
  try {
    await deleteInvoice(invoiceId)
    confirmingDeleteInvoiceId.value = null
    await loadInvoices()
  } catch {
    invoiceError.value = t('purchaseRequests.invoices.removeError')
  }
}

async function onConclude() {
  concludeError.value = ''
  concluding.value = true
  try {
    await conclude(purchaseRequestId)
    await load()
  } catch {
    concludeError.value = t('purchaseRequests.approvalError')
  } finally {
    concluding.value = false
  }
}

function onPhotosSelected(materialId: string, event: Event) {
  const files = (event.target as HTMLInputElement).files
  photosByMaterial.value[materialId] = files ? Array.from(files) : []
}

async function onMarkDelivered(materialId: string) {
  materialActionError.value = ''
  try {
    await markDelivered(materialId)
    await loadMaterials()
  } catch {
    materialActionError.value = t('materialDelivery.error')
  }
}

async function onMarkChecked(materialId: string) {
  materialActionError.value = ''
  try {
    await markChecked(materialId, photosByMaterial.value[materialId] ?? [])
    await loadMaterials()
  } catch {
    materialActionError.value = t('materialDelivery.error')
  }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.back()">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('purchaseRequests.backToList') }}
        </button>
      </template>
    </AppHeader>

    <main v-if="detail" class="app-container max-w-5xl! space-y-6 py-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ detail.purchaseRequest.name }}</h1>
          <div class="mt-1.5 flex flex-wrap items-center gap-2">
            <StatusBadge kind="purchaseRequest" :status="detail.purchaseRequest.status" />
          </div>
        </div>
        <div class="flex gap-2">
          <button v-if="canSubmit" type="button" :disabled="submitting" class="btn-primary" @click="onSubmitForApproval">
            {{ t('purchaseRequests.submitButton') }}
          </button>
          <button v-if="canConclude" type="button" :disabled="concluding" class="btn-primary" @click="onConclude">
            {{ t('purchaseRequests.concludeButton') }}
          </button>
          <div v-if="canDelete" class="relative">
            <button type="button" :disabled="deleting" class="btn-danger" @click="confirmingDelete = !confirmingDelete">
              {{ t('purchaseRequests.deleteButton') }}
            </button>
            <div v-if="confirmingDelete" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-3 shadow-lg" @click.stop>
              <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('purchaseRequests.deleteConfirm') }}</p>
              <div class="flex justify-end gap-2">
                <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDelete = false">{{ t('purchaseRequests.form.cancel') }}</button>
                <button type="button" :disabled="deleting" class="btn-danger py-1 text-xs" @click="onDelete">{{ t('purchaseRequests.deleteButton') }}</button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <p v-if="deleteError" class="text-sm text-safety-600 dark:text-safety-500">{{ deleteError }}</p>

      <!-- Status stepper -->
      <section class="card card-pad">
        <ol class="flex flex-wrap items-center gap-2 text-sm">
          <template v-for="(step, index) in STEPS" :key="step">
            <li class="flex items-center gap-2">
              <span
                class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold"
                :class="
                  index < currentStepIndex
                    ? 'bg-emerald-500 text-white'
                    : index === currentStepIndex
                      ? 'bg-blueprint-600 text-white'
                      : 'bg-steel-200 text-steel-500 dark:bg-steel-700 dark:text-steel-400'
                "
              >
                {{ index + 1 }}
              </span>
              <span
                class="font-medium"
                :class="index <= currentStepIndex ? 'text-steel-800 dark:text-steel-50' : 'text-steel-400 dark:text-steel-500'"
              >
                {{ t(`purchaseRequests.status.${step}`) }}
              </span>
            </li>
            <li v-if="index < STEPS.length - 1" class="h-px w-8 shrink-0 bg-steel-200 dark:bg-steel-700"></li>
          </template>
        </ol>
        <p v-if="detail.purchaseRequest.lastRejectionReason && detail.purchaseRequest.status === 'ORCADO'" class="mt-3 text-sm text-safety-600 dark:text-safety-500">
          {{ t('purchaseRequests.lastRejectionReason') }}: {{ detail.purchaseRequest.lastRejectionReason }}
        </p>
        <p v-if="submitError" class="mt-3 text-sm text-safety-600 dark:text-safety-500">{{ submitError }}</p>
        <p v-if="concludeError" class="mt-3 text-sm text-safety-600 dark:text-safety-500">{{ concludeError }}</p>
      </section>

      <!-- Linked Orçamentos -->
      <section v-if="detail.purchaseRequest.linkedOrcamentos.length > 0" class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.linkedOrcamentosTitle') }}</h2>
        <ul class="flex flex-wrap gap-2">
          <li v-for="linked in detail.purchaseRequest.linkedOrcamentos" :key="linked.id">
            <router-link
              :to="`/orcamentos/${linked.id}`"
              class="inline-flex items-center gap-1.5 rounded-full border border-blueprint-200 bg-blueprint-50 px-3 py-1 text-sm font-medium text-blueprint-700 transition hover:bg-blueprint-100 dark:border-blueprint-800 dark:bg-blueprint-900/30 dark:text-blueprint-300"
            >
              {{ linked.fornecedorNome }}
            </router-link>
          </li>
        </ul>
      </section>

      <!-- Invoices -->
      <section class="card card-pad">
        <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.invoices.title') }}</h2>
          <label class="btn-secondary cursor-pointer px-3 py-1.5 text-xs" :class="{ 'pointer-events-none opacity-60': uploadingInvoice }">
            {{ uploadingInvoice ? t('purchaseRequests.invoices.uploading') : t('purchaseRequests.invoices.uploadButton') }}
            <input type="file" accept=".pdf,.xml,.jpg,.jpeg,.png" class="hidden" :disabled="uploadingInvoice" @change="onUploadInvoice" />
          </label>
        </div>
        <p v-if="invoiceError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ invoiceError }}</p>
        <p v-if="invoices.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.invoices.empty') }}</p>
        <ul v-else class="divide-y divide-steel-100 dark:divide-steel-800">
          <li v-for="invoice in invoices" :key="invoice.id" class="flex items-center justify-between gap-3 py-2 text-sm">
            <button type="button" class="truncate text-left font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="onDownloadInvoice(invoice)">
              {{ invoice.originalName }}
            </button>
            <div class="relative shrink-0">
              <button type="button" class="text-xs font-medium text-safety-600 hover:underline dark:text-safety-500" @click="confirmingDeleteInvoiceId = invoice.id">
                {{ t('purchaseRequests.invoices.removeButton') }}
              </button>
              <div v-if="confirmingDeleteInvoiceId === invoice.id" class="modal-panel absolute right-0 top-full z-10 mt-2 w-64 p-3 shadow-lg" @click.stop>
                <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('purchaseRequests.invoices.removeConfirm') }}</p>
                <div class="flex justify-end gap-2">
                  <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDeleteInvoiceId = null">{{ t('purchaseRequests.form.cancel') }}</button>
                  <button type="button" class="btn-danger py-1 text-xs" @click="onDeleteInvoice(invoice.id)">{{ t('purchaseRequests.invoices.removeButton') }}</button>
                </div>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <!-- Items -->
      <section class="card card-pad">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-sm font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
            {{ t('purchaseRequests.itemsTitle') }}
          </h2>
          <button
            type="button"
            :disabled="selectedIds.size === 0 || converting"
            class="btn-primary px-3 py-1.5 text-xs"
            @click="showFornecedorPicker = true"
          >
            {{ t('purchaseRequests.convertButton') }} ({{ selectedIds.size }})
          </button>
        </div>

        <div v-if="showFornecedorPicker" class="mb-5">
          <FornecedorPicker :site-id="detail.purchaseRequest.constructionSiteId" @confirm="onConvertConfirmed" @cancel="showFornecedorPicker = false" />
        </div>
        <p v-if="convertError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ convertError }}</p>

        <p v-if="!loading && items.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
          {{ t('purchaseRequests.noItems') }}
        </p>
        <div v-else class="max-h-[28rem] overflow-y-auto overflow-x-auto rounded-lg border border-steel-200 dark:border-steel-700">
          <table class="w-full text-sm">
            <thead class="sticky top-0 bg-white dark:bg-steel-800">
              <tr class="border-b border-steel-200 text-left text-xs uppercase tracking-wide text-steel-500 dark:border-steel-700 dark:text-steel-400">
                <th class="w-8 py-2 pl-3">
                  <input type="checkbox" :checked="allSelected" class="h-4 w-4" @change="toggleSelectAll" />
                </th>
                <th class="py-2 pr-3 font-medium">{{ t('purchaseRequests.table.product') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('purchaseRequests.table.quantity') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('purchaseRequests.table.status') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('purchaseRequests.table.selectedSupplier') }}</th>
                <th class="py-2 pr-3 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in items"
                :key="item.id"
                class="border-b border-steel-100 last:border-0 dark:border-steel-800"
                :class="selectedIds.has(item.id) ? 'bg-blueprint-50 dark:bg-blueprint-900/20' : ''"
              >
                <td class="py-1.5 pl-3">
                  <input type="checkbox" :checked="selectedIds.has(item.id)" class="h-4 w-4" @change="toggleSelection(item.id)" />
                </td>
                <td class="max-w-xs truncate py-1.5 pr-3">
                  <span class="font-medium text-steel-800 dark:text-steel-50">{{ item.name }}</span>
                  <span v-if="item.type" class="text-steel-500 dark:text-steel-400"> · {{ item.type }}</span>
                </td>
                <td class="whitespace-nowrap py-1.5 pr-3 text-steel-700 dark:text-steel-200">
                  {{ item.quantity }}{{ item.unit ? ` ${item.unit}` : '' }}
                </td>
                <td class="py-1.5 pr-3"><StatusBadge kind="purchaseRequestItem" :status="item.status" /></td>
                <td class="py-1.5 pr-3 text-xs">
                  <span v-if="selectionByItemId.get(item.id)" class="text-emerald-700 dark:text-emerald-400">
                    {{ selectionByItemId.get(item.id)!.supplierName }}
                    <span v-if="selectionByItemId.get(item.id)!.unitPrice"> · {{ selectionByItemId.get(item.id)!.unitPrice }}</span>
                  </span>
                  <span v-else class="text-steel-400 dark:text-steel-500">—</span>
                </td>
                <td class="py-1.5 pr-3">
                  <router-link
                    v-if="item.convertedToOrcamentoId"
                    :to="`/orcamentos/${item.convertedToOrcamentoId}`"
                    class="whitespace-nowrap text-xs text-blueprint-600 hover:underline dark:text-blueprint-400"
                  >
                    {{ t('orcamento.label') }}
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Comparison table -->
      <section v-if="comparison && comparison.columns.length > 0" class="card card-pad">
        <div class="mb-1 flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.comparison.title') }}</h2>
          <button type="button" :disabled="printingSummary" class="btn-secondary px-3 py-1.5 text-xs" @click="onPrintSummary">
            {{ printingSummary ? t('purchaseRequests.comparison.summaryPrinting') : t('purchaseRequests.comparison.summaryButton') }}
          </button>
        </div>
        <p class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.comparison.subtitle') }}</p>
        <p v-if="selectionError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ selectionError }}</p>
        <p v-if="printError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ printError }}</p>
        <p v-if="summaryError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ summaryError }}</p>
        <PurchaseRequestComparisonTable
          :comparison="comparison"
          :readonly="!selectionEditable"
          :printing-orcamento-id="printingOrcamentoId"
          @select="onSelectCell"
          @print="onPrintPdf"
        />
      </section>

      <!-- Approval -->
      <section v-if="detail.approvals.length > 0" class="card card-pad">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.approvalHistoryTitle') }}</h2>

        <div v-if="currentPendingApproval" class="mb-4 rounded-lg border border-blueprint-200 bg-blueprint-50 p-4 dark:border-blueprint-800 dark:bg-blueprint-900/30">
          <p class="mb-2 text-sm font-medium text-steel-800 dark:text-steel-50">
            {{ t('purchaseRequests.stepLabel') }} {{ currentPendingApproval.stepOrder }} — {{ t(`purchaseRequests.approverFunction.${currentPendingApproval.approverFunction}`) }}
          </p>
          <div class="flex flex-wrap items-center gap-2">
            <button type="button" class="btn-primary px-3 py-1.5" @click="onApproveStep">{{ t('purchaseRequests.approveStepButton') }}</button>
            <button type="button" class="btn-danger px-3 py-1.5" @click="showRejectForm = !showRejectForm">{{ t('purchaseRequests.rejectStepButton') }}</button>
          </div>
          <form v-if="showRejectForm" class="mt-3 flex flex-wrap items-center gap-2" @submit.prevent="onRejectStep">
            <input v-model="rejectReason" type="text" required :placeholder="t('purchaseRequests.rejectReasonPlaceholder')" class="field-input flex-1" />
            <button type="submit" class="btn-danger px-3 py-1.5">{{ t('purchaseRequests.confirmReject') }}</button>
          </form>
          <p v-if="approvalActionError" class="mt-2 text-sm text-safety-600 dark:text-safety-500">{{ approvalActionError }}</p>
        </div>

        <div v-for="cycle in approvalsByCycle" :key="cycle.cycleNumber" class="mb-4 last:mb-0">
          <p class="mb-1.5 text-xs font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
            {{ t('purchaseRequests.cycleLabel') }} {{ cycle.cycleNumber }}
          </p>
          <ol class="space-y-1.5 border-l-2 border-steel-200 pl-4 dark:border-steel-700">
            <li v-for="approval in cycle.approvals" :key="approval.id" class="flex items-center justify-between text-sm">
              <span class="text-steel-700 dark:text-steel-200">
                {{ t('purchaseRequests.stepLabel') }} {{ approval.stepOrder }} — {{ t(`purchaseRequests.approverFunction.${approval.approverFunction}`) }}
              </span>
              <StatusBadge kind="approval" :status="approval.status" />
            </li>
          </ol>
        </div>
      </section>

      <!-- Materials (post-conclusion) -->
      <section v-if="isConcluded" class="card card-pad border-2 border-emerald-200 bg-emerald-50/40 dark:border-emerald-900 dark:bg-emerald-900/10">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('purchaseRequests.materialsTitle') }}</h2>
        <p v-if="materialActionError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ materialActionError }}</p>
        <p v-if="materials.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.materialsEmpty') }}</p>
        <ul v-else class="space-y-2">
          <li v-for="material in materials" :key="material.id" class="rounded-lg border border-steel-200 bg-white px-4 py-3 dark:border-steel-700 dark:bg-steel-800">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p class="font-medium text-steel-800 dark:text-steel-50">{{ material.name }}</p>
                <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('purchaseRequests.form.quantity') }}: {{ material.quantity }}</p>
              </div>
              <span class="badge bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200">
                {{ t(`materialDelivery.status.${material.status}`) }}
              </span>
            </div>
            <div v-if="material.status === 'AWAITING_DELIVERY'" class="mt-2">
              <button type="button" class="btn-secondary px-3 py-1.5 text-xs" @click="onMarkDelivered(material.id)">
                {{ t('materialDelivery.markDeliveredButton') }}
              </button>
            </div>
            <div v-else-if="material.status === 'DELIVERED'" class="mt-2 flex flex-wrap items-center gap-2">
              <label class="text-xs font-medium text-blueprint-600 dark:text-blueprint-400">
                {{ t('materialDelivery.uploadPhotoLabel') }}
                <input type="file" accept="image/*" multiple class="hidden" @change="onPhotosSelected(material.id, $event)" />
              </label>
              <button type="button" class="btn-secondary px-3 py-1.5 text-xs" @click="onMarkChecked(material.id)">
                {{ t('materialDelivery.markCheckedButton') }}
              </button>
            </div>
          </li>
        </ul>
      </section>
    </main>

    <p v-else-if="loadError" class="app-container max-w-5xl! py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
  </div>
</template>
