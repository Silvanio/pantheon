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
import AppHeader from '../components/AppHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getOrcamento, addLineItem, updateLineItem, removeLineItem, deleteOrcamento } = useOrcamentos()

const orcamentoId = route.params.id as string
const detail = ref<OrcamentoDetail | null>(null)
const loading = ref(false)
const loadError = ref('')
const confirmingDelete = ref(false)
const deleting = ref(false)
const deleteError = ref('')

const isDraft = computed(() => detail.value?.orcamento.status === 'DRAFT')

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

// Inline-editable unit price per line item — always a text field so only digits/one decimal
// separator can ever be typed, rather than relying on a native number input's quirks.
const priceDrafts = ref<Record<string, string>>({})
const priceSaving = ref<Record<string, boolean>>({})
const priceError = ref('')

function syncPriceDrafts() {
  const drafts: Record<string, string> = {}
  for (const item of detail.value?.lineItems ?? []) {
    drafts[item.id] = item.unitPrice ?? ''
  }
  priceDrafts.value = drafts
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getOrcamento(orcamentoId)
    syncPriceDrafts()
  } catch {
    loadError.value = t('orcamento.loadError')
  } finally {
    loading.value = false
  }
}

function sanitizeMoneyInput(value: string): string {
  let result = ''
  let seenSeparator = false
  for (const char of value) {
    if (char >= '0' && char <= '9') {
      result += char
    } else if ((char === '.' || char === ',') && !seenSeparator) {
      result += '.'
      seenSeparator = true
    }
  }
  return result
}

function onPriceInput(itemId: string, event: Event) {
  const input = event.target as HTMLInputElement
  const sanitized = sanitizeMoneyInput(input.value)
  priceDrafts.value[itemId] = sanitized
  if (sanitized !== input.value) {
    input.value = sanitized
  }
}

async function onPriceBlur(item: OrcamentoLineItem) {
  const newValue = priceDrafts.value[item.id] ?? ''
  const currentValue = item.unitPrice ?? ''
  if (newValue === currentValue) return
  priceError.value = ''
  priceSaving.value[item.id] = true
  try {
    await updateLineItem(orcamentoId, item.id, {
      name: item.name,
      type: item.type,
      quantity: item.quantity,
      unitPrice: newValue || null,
    })
    await load()
  } catch {
    priceError.value = t('orcamento.form.error')
    priceDrafts.value[item.id] = currentValue
  } finally {
    priceSaving.value[item.id] = false
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

async function onDelete() {
  deleteError.value = ''
  deleting.value = true
  try {
    await deleteOrcamento(orcamentoId)
    router.back()
  } catch {
    deleteError.value = t('orcamento.deleteError')
    deleting.value = false
    confirmingDelete.value = false
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
            <StatusBadge kind="orcamento" :status="detail.orcamento.status" />
            <router-link
              v-if="detail.orcamento.sourcePurchaseRequestId"
              :to="`/purchase-requests/${detail.orcamento.sourcePurchaseRequestId}`"
              class="text-sm text-blueprint-600 hover:underline dark:text-blueprint-400"
            >
              {{ t('orcamento.sourcePurchaseRequestLabel') }}: {{ detail.orcamento.sourcePurchaseRequestName }}
            </router-link>
          </div>
        </div>
        <div v-if="isDraft" class="relative">
          <button type="button" :disabled="deleting" class="btn-danger" @click="confirmingDelete = !confirmingDelete">
            {{ t('orcamento.deleteButton') }}
          </button>
          <div v-if="confirmingDelete" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-3 shadow-lg" @click.stop>
            <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('orcamento.deleteConfirm') }}</p>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDelete = false">{{ t('orcamento.form.cancel') }}</button>
              <button type="button" :disabled="deleting" class="btn-danger py-1 text-xs" @click="onDelete">{{ t('orcamento.deleteButton') }}</button>
            </div>
          </div>
        </div>
      </div>
      <p v-if="deleteError" class="text-sm text-safety-600 dark:text-safety-500">{{ deleteError }}</p>

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
          <div v-if="detail.orcamento.fornecedorFormaPagamento">
            <dt class="text-steel-500 dark:text-steel-400">{{ t('fornecedor.paymentMethodLabel') }}</dt>
            <dd class="text-steel-700 dark:text-steel-200">{{ t(`fornecedor.paymentMethod.${detail.orcamento.fornecedorFormaPagamento}`) }}</dd>
          </div>
          <div v-if="detail.orcamento.fornecedorFormaPagamento === 'PIX'">
            <dt class="text-steel-500 dark:text-steel-400">{{ t('fornecedor.pixKeyLabel') }}</dt>
            <dd class="text-steel-700 dark:text-steel-200">{{ detail.orcamento.fornecedorPixKey }}</dd>
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
        <p v-if="!isDraft" class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.lockedHint') }}</p>

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

        <p v-if="priceError" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ priceError }}</p>
        <p v-if="detail.lineItems.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.lineItemsEmpty') }}</p>
        <div v-else class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-steel-200 text-left text-xs uppercase tracking-wide text-steel-500 dark:border-steel-700 dark:text-steel-400">
                <th class="py-2 pr-3 font-medium">{{ t('orcamento.form.name') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('orcamento.form.quantity') }}</th>
                <th class="py-2 pr-3 font-medium">{{ t('orcamento.form.unitPrice') }}</th>
                <th class="py-2 pr-3 font-medium"></th>
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
                <td class="py-2.5 pr-3 text-steel-700 dark:text-steel-200">
                  <input
                    v-if="isDraft"
                    type="text"
                    inputmode="decimal"
                    class="field-input w-28 py-1"
                    :disabled="priceSaving[item.id]"
                    :value="priceDrafts[item.id]"
                    @input="onPriceInput(item.id, $event)"
                    @blur="onPriceBlur(item)"
                    @keyup.enter="($event.target as HTMLInputElement).blur()"
                  />
                  <span v-else>{{ item.unitPrice ?? '—' }}</span>
                </td>
                <td class="py-2.5 pr-3">
                  <span v-if="item.selected" class="badge bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300">
                    {{ t('orcamento.selectedLabel') }}
                  </span>
                </td>
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
                <td></td>
                <td v-if="isDraft"></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </section>
    </main>

    <p v-else-if="loadError" class="app-container max-w-5xl! py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
  </div>
</template>
