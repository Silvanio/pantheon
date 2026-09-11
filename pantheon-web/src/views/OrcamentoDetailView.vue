<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  useOrcamentos,
  type OrcamentoDetail,
  type OrcamentoLineItem,
  type OrcamentoLineItemInput,
} from '../composables/useOrcamentos'
import { useMaterialDeliveries } from '../composables/useMaterialDeliveries'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const {
  getOrcamento,
  addLineItem,
  updateLineItem,
  removeLineItem,
  submitForApproval,
  approveStep,
  rejectStep,
  conclude,
} = useOrcamentos()
const { markDelivered, markChecked } = useMaterialDeliveries()

const orcamentoId = route.params.id as string
const detail = ref<OrcamentoDetail | null>(null)
const loading = ref(false)
const loadError = ref('')

const isDraft = computed(() => detail.value?.orcamento.status === 'DRAFT')
const isApproved = computed(() => detail.value?.orcamento.status === 'APPROVED')

const statusBadgeClass: Record<string, string> = {
  DRAFT: 'bg-steel-100 text-steel-700 dark:bg-steel-700 dark:text-steel-200',
  IN_APPROVAL: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
  APPROVED: 'bg-blueprint-100 text-blueprint-700 dark:bg-blueprint-900/50 dark:text-blueprint-300',
  COMPLETED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300',
}

const approvalStatusBadgeClass: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-200',
  APPROVED: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300',
  REJECTED: 'bg-safety-100 text-safety-700 dark:bg-safety-900/50 dark:text-safety-300',
}

const currentPendingApproval = computed(() => {
  if (!detail.value) return null
  const currentCycle = detail.value.orcamento.currentApprovalCycle
  return (
    detail.value.approvals
      .filter((a) => a.cycleNumber === currentCycle && a.status === 'PENDING')
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

const lineItemsTotal = computed(() => {
  if (!detail.value) return null
  let total = 0
  let hasAnyPrice = false
  for (const item of detail.value.lineItems) {
    if (item.unitPrice) {
      hasAnyPrice = true
      total += parseFloat(item.quantity) * parseFloat(item.unitPrice)
    }
  }
  return hasAnyPrice ? total : null
})

// Line item form
const showItemForm = ref(false)
const editingItemId = ref<string | null>(null)
const itemName = ref('')
const itemType = ref('')
const itemQuantity = ref('')
const itemUnitPrice = ref('')
const itemSubmitting = ref(false)
const itemError = ref('')

const submitting = ref(false)
const submitError = ref('')
const approvalActionError = ref('')
const rejectReason = ref('')
const showRejectForm = ref(false)
const concluding = ref(false)
const concludeError = ref('')

const materialActionError = ref('')
const photosByMaterial = ref<Record<string, File[]>>({})

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getOrcamento(orcamentoId)
  } catch {
    loadError.value = t('orcamento.loadError')
  } finally {
    loading.value = false
  }
}

function startAddItem() {
  editingItemId.value = null
  itemName.value = ''
  itemType.value = ''
  itemQuantity.value = ''
  itemUnitPrice.value = ''
  itemError.value = ''
  showItemForm.value = true
}

function startEditItem(item: OrcamentoLineItem) {
  editingItemId.value = item.id
  itemName.value = item.name
  itemType.value = item.type ?? ''
  itemQuantity.value = item.quantity
  itemUnitPrice.value = item.unitPrice ?? ''
  itemError.value = ''
  showItemForm.value = true
}

async function onSubmitItem() {
  itemError.value = ''
  itemSubmitting.value = true
  try {
    const input: OrcamentoLineItemInput = {
      name: itemName.value,
      type: itemType.value || null,
      quantity: itemQuantity.value,
      unitPrice: itemUnitPrice.value || null,
    }
    if (editingItemId.value) {
      await updateLineItem(orcamentoId, editingItemId.value, input)
    } else {
      await addLineItem(orcamentoId, input)
    }
    showItemForm.value = false
    await load()
  } catch {
    itemError.value = t('orcamento.form.error')
  } finally {
    itemSubmitting.value = false
  }
}

async function onRemoveItem(lineItemId: string) {
  try {
    await removeLineItem(orcamentoId, lineItemId)
    await load()
  } catch {
    itemError.value = t('orcamento.form.error')
  }
}

async function onSubmitForApproval() {
  submitError.value = ''
  submitting.value = true
  try {
    await submitForApproval(orcamentoId)
    await load()
  } catch {
    submitError.value = t('orcamento.submitError')
  } finally {
    submitting.value = false
  }
}

async function onApproveStep() {
  approvalActionError.value = ''
  try {
    await approveStep(orcamentoId)
    await load()
  } catch {
    approvalActionError.value = t('orcamento.error')
  }
}

async function onRejectStep() {
  approvalActionError.value = ''
  try {
    await rejectStep(orcamentoId, rejectReason.value)
    showRejectForm.value = false
    rejectReason.value = ''
    await load()
  } catch {
    approvalActionError.value = t('orcamento.error')
  }
}

async function onConclude() {
  concludeError.value = ''
  concluding.value = true
  try {
    await conclude(orcamentoId)
    await load()
  } catch {
    concludeError.value = t('orcamento.error')
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
    await load()
  } catch {
    materialActionError.value = t('materialDelivery.error')
  }
}

async function onMarkChecked(materialId: string) {
  materialActionError.value = ''
  try {
    await markChecked(materialId, photosByMaterial.value[materialId] ?? [])
    await load()
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
          {{ t('orcamento.back') }}
        </button>
      </template>
    </AppHeader>

    <main v-if="detail" class="app-container max-w-5xl! space-y-6 py-8">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">
            {{ t('orcamento.label') }} #{{ detail.orcamento.id.slice(0, 8) }}
          </h1>
          <div class="mt-1.5 flex flex-wrap items-center gap-2">
            <span class="badge" :class="statusBadgeClass[detail.orcamento.status]">
              {{ t(`orcamento.status.${detail.orcamento.status}`) }}
            </span>
            <router-link
              v-if="detail.orcamento.sourcePurchaseRequestId"
              :to="`/purchase-requests/${detail.orcamento.sourcePurchaseRequestId}`"
              class="text-sm text-blueprint-600 hover:underline dark:text-blueprint-400"
            >
              {{ t('orcamento.sourcePurchaseRequestLabel') }}: {{ detail.orcamento.sourcePurchaseRequestName }}
            </router-link>
          </div>
        </div>
        <div class="flex gap-2">
          <button v-if="isDraft" type="button" :disabled="submitting" class="btn-primary" @click="onSubmitForApproval">
            {{ t('orcamento.submitButton') }}
          </button>
          <button v-if="isApproved" type="button" :disabled="concluding" class="btn-primary" @click="onConclude">
            {{ t('orcamento.concludeButton') }}
          </button>
        </div>
      </div>
      <p v-if="submitError" class="text-sm text-safety-600 dark:text-safety-500">{{ submitError }}</p>
      <p v-if="concludeError" class="text-sm text-safety-600 dark:text-safety-500">{{ concludeError }}</p>
      <p v-if="detail.orcamento.lastRejectionReason && isDraft" class="text-sm text-safety-600 dark:text-safety-500">
        {{ t('orcamento.lastRejectionReason') }}: {{ detail.orcamento.lastRejectionReason }}
      </p>

      <!-- Supplier -->
      <section class="card card-pad">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.supplierSectionTitle') }}</h2>
        <dl class="grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
          <div>
            <dt class="text-steel-500 dark:text-steel-400">{{ t('fornecedor.nameLabel') }}</dt>
            <dd class="font-medium text-steel-800 dark:text-steel-50">{{ detail.orcamento.fornecedorNome }}</dd>
          </div>
          <div>
            <dt class="text-steel-500 dark:text-steel-400">{{ t('fornecedor.cnpjLabel') }}</dt>
            <dd class="font-medium text-steel-800 dark:text-steel-50">{{ detail.orcamento.fornecedorCnpj }}</dd>
          </div>
          <div v-if="detail.orcamento.fornecedorEndereco">
            <dt class="text-steel-500 dark:text-steel-400">{{ t('fornecedor.addressLabel') }}</dt>
            <dd class="text-steel-700 dark:text-steel-200">{{ detail.orcamento.fornecedorEndereco }}</dd>
          </div>
          <div v-if="detail.orcamento.fornecedorContatoNome || detail.orcamento.fornecedorContatoTelefone">
            <dt class="text-steel-500 dark:text-steel-400">{{ t('fornecedor.contactNameLabel') }}</dt>
            <dd class="text-steel-700 dark:text-steel-200">
              {{ detail.orcamento.fornecedorContatoNome }}
              <span v-if="detail.orcamento.fornecedorContatoTelefone"> · {{ detail.orcamento.fornecedorContatoTelefone }}</span>
            </dd>
          </div>
        </dl>
      </section>

      <!-- Line items -->
      <section class="card card-pad">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.lineItemsTitle') }}</h2>
          <button v-if="isDraft" type="button" class="btn-primary px-3 py-1.5" @click="startAddItem">
            {{ t('orcamento.addItemButton') }}
          </button>
        </div>

        <form v-if="showItemForm" class="mb-4 grid grid-cols-1 gap-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700 sm:grid-cols-2" @submit.prevent="onSubmitItem">
          <div>
            <label class="field-label">{{ t('orcamento.form.name') }}</label>
            <input v-model="itemName" type="text" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('orcamento.form.type') }}</label>
            <input v-model="itemType" type="text" class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('orcamento.form.quantity') }}</label>
            <input v-model="itemQuantity" type="number" step="0.001" min="0" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('orcamento.form.unitPrice') }}</label>
            <input v-model="itemUnitPrice" type="number" step="0.01" min="0" class="field-input" />
          </div>
          <p v-if="itemError" class="text-sm text-safety-600 dark:text-safety-500 sm:col-span-2">{{ itemError }}</p>
          <div class="flex gap-2 sm:col-span-2">
            <button type="submit" :disabled="itemSubmitting" class="btn-primary">{{ t('orcamento.form.submit') }}</button>
            <button type="button" class="btn-secondary" @click="showItemForm = false">{{ t('orcamento.form.cancel') }}</button>
          </div>
        </form>

        <p v-if="detail.lineItems.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.lineItemsEmpty') }}</p>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-steel-200 text-left text-xs uppercase tracking-wide text-steel-500 dark:border-steel-700 dark:text-steel-400">
                <th class="py-2 pr-3 font-medium">{{ t('orcamento.form.name') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('orcamento.form.quantity') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('orcamento.form.unitPrice') }}</th>
                <th v-if="isDraft" class="py-2 pr-3 font-medium"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in detail.lineItems" :key="item.id" class="border-b border-steel-100 last:border-0 dark:border-steel-800">
                <td class="py-2.5 pr-3">
                  <p class="font-medium text-steel-800 dark:text-steel-50">{{ item.name }}</p>
                  <p v-if="item.type" class="text-xs text-steel-500 dark:text-steel-400">{{ item.type }}</p>
                </td>
                <td class="py-2.5 pr-3 text-steel-700 dark:text-steel-200">{{ item.quantity }}</td>
                <td class="py-2.5 pr-3 text-steel-700 dark:text-steel-200">{{ item.unitPrice ?? '—' }}</td>
                <td v-if="isDraft" class="py-2.5 pr-3">
                  <div class="flex gap-3">
                    <button type="button" class="text-sm font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="startEditItem(item)">
                      {{ t('orcamento.editItemButton') }}
                    </button>
                    <button type="button" class="text-sm font-medium text-safety-600 hover:underline dark:text-safety-500" @click="onRemoveItem(item.id)">
                      {{ t('orcamento.removeItemButton') }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
            <tfoot v-if="lineItemsTotal !== null">
              <tr class="border-t border-steel-200 dark:border-steel-700">
                <td class="py-2.5 pr-3 font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.totalLabel') }}</td>
                <td></td>
                <td class="py-2.5 pr-3 font-semibold text-steel-800 dark:text-steel-50">{{ lineItemsTotal.toFixed(2) }}</td>
                <td v-if="isDraft"></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>

      <!-- Approval -->
      <section v-if="detail.approvals.length > 0" class="card card-pad">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.approvalHistoryTitle') }}</h2>

        <div v-if="currentPendingApproval" class="mb-4 rounded-lg border border-blueprint-200 bg-blueprint-50 p-4 dark:border-blueprint-800 dark:bg-blueprint-900/30">
          <p class="mb-2 text-sm font-medium text-steel-800 dark:text-steel-50">
            {{ t('orcamento.stepLabel') }} {{ currentPendingApproval.stepOrder }} — {{ t(`orcamento.approverFunction.${currentPendingApproval.approverFunction}`) }}
          </p>
          <div class="flex flex-wrap items-center gap-2">
            <button type="button" class="btn-primary px-3 py-1.5" @click="onApproveStep">{{ t('orcamento.approveStepButton') }}</button>
            <button type="button" class="btn-danger px-3 py-1.5" @click="showRejectForm = !showRejectForm">{{ t('orcamento.rejectStepButton') }}</button>
          </div>
          <form v-if="showRejectForm" class="mt-3 flex flex-wrap items-center gap-2" @submit.prevent="onRejectStep">
            <input v-model="rejectReason" type="text" required :placeholder="t('orcamento.rejectReasonPlaceholder')" class="field-input flex-1" />
            <button type="submit" class="btn-danger px-3 py-1.5">{{ t('orcamento.confirmReject') }}</button>
          </form>
          <p v-if="approvalActionError" class="mt-2 text-sm text-safety-600 dark:text-safety-500">{{ approvalActionError }}</p>
        </div>

        <div v-for="cycle in approvalsByCycle" :key="cycle.cycleNumber" class="mb-4 last:mb-0">
          <p class="mb-1.5 text-xs font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
            {{ t('orcamento.cycleLabel') }} {{ cycle.cycleNumber }}
          </p>
          <ol class="space-y-1.5 border-l-2 border-steel-200 pl-4 dark:border-steel-700">
            <li v-for="approval in cycle.approvals" :key="approval.id" class="flex items-center justify-between text-sm">
              <span class="text-steel-700 dark:text-steel-200">
                {{ t('orcamento.stepLabel') }} {{ approval.stepOrder }} — {{ t(`orcamento.approverFunction.${approval.approverFunction}`) }}
              </span>
              <span class="badge" :class="approvalStatusBadgeClass[approval.status]">{{ t(`orcamento.approvalStatus.${approval.status}`) }}</span>
            </li>
          </ol>
        </div>
      </section>

      <!-- Materials (post-conclusion) -->
      <section v-if="detail.materials.length > 0" class="card card-pad border-2 border-emerald-200 bg-emerald-50/40 dark:border-emerald-900 dark:bg-emerald-900/10">
        <h2 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.materialsTitle') }}</h2>
        <p v-if="materialActionError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ materialActionError }}</p>
        <ul class="space-y-2">
          <li v-for="material in detail.materials" :key="material.id" class="rounded-lg border border-steel-200 bg-white px-4 py-3 dark:border-steel-700 dark:bg-steel-800">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p class="font-medium text-steel-800 dark:text-steel-50">{{ material.name }}</p>
                <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.form.quantity') }}: {{ material.quantity }}</p>
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
