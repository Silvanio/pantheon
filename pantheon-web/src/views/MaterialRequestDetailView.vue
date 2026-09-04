<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  useMaterialRequests,
  type AttachmentKind,
  type MaterialRequestDetail,
  type Orcamento,
  type OrcamentoDetail,
} from '../composables/useMaterialRequests'
import { useEquipmentMaterials, type MaterialItem } from '../composables/useEquipmentMaterials'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const {
  getDetail,
  approve,
  reject,
  recordVerification,
  uploadVerificationPhoto,
  listOrcamentos,
  createOrcamento,
  getOrcamento,
  sendOrcamento,
  approveOrcamento,
  rejectOrcamento,
  uploadOrcamentoAttachment,
} = useMaterialRequests()
const { listMaterials } = useEquipmentMaterials()

const requestId = route.params.id as string
const detail = ref<MaterialRequestDetail | null>(null)
const materialCatalog = ref<MaterialItem[]>([])
const loading = ref(false)
const loadError = ref('')

const isPending = computed(() => detail.value?.request.status === 'PENDING')
const canVerify = computed(() =>
  ['APPROVED', 'PARTIALLY_RECEIVED', 'RECEIVED'].includes(detail.value?.request.status ?? ''),
)

const decisionError = ref('')
const deciding = ref(false)
const showRejectForm = ref(false)
const rejectReason = ref('')

const verificationError = ref('')
const verifyingItemId = ref<string | null>(null)
const receivedQuantityByItem = ref<Record<string, string>>({})
const noteByItem = ref<Record<string, string>>({})

const orcamentos = ref<Orcamento[]>([])
const orcamentoDetails = ref<Record<string, OrcamentoDetail>>({})
const expandedOrcamentoId = ref<string | null>(null)
const showCreateOrcamentoForm = ref(false)
const orcamentoError = ref('')
const orcamentoSubmitting = ref(false)
const orcamentoPriceByItem = ref<Record<string, string>>({})
const rejectOrcamentoReason = ref<Record<string, string>>({})

function materialLabel(materialId: string): string {
  const material = materialCatalog.value.find((m) => m.id === materialId)
  return material ? `${material.name} (${material.unit})` : materialId
}

function verificationForItem(itemId: string) {
  return detail.value?.verifications.find((v) => v.materialRequestItemId === itemId) ?? null
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getDetail(requestId)
    materialCatalog.value = await listMaterials(detail.value.request.constructionSiteId)
    orcamentos.value = await listOrcamentos(requestId)
  } catch {
    loadError.value = t('materialRequests.detail.loadError')
  } finally {
    loading.value = false
  }
}

async function onApprove() {
  decisionError.value = ''
  deciding.value = true
  try {
    const updated = await approve(requestId)
    if (detail.value) detail.value.request = updated
  } catch {
    decisionError.value = t('materialRequests.detail.decisionError')
  } finally {
    deciding.value = false
  }
}

async function onReject() {
  decisionError.value = ''
  deciding.value = true
  try {
    const updated = await reject(requestId, rejectReason.value)
    if (detail.value) detail.value.request = updated
    showRejectForm.value = false
    rejectReason.value = ''
  } catch {
    decisionError.value = t('materialRequests.detail.decisionError')
  } finally {
    deciding.value = false
  }
}

async function onVerify(itemId: string) {
  verificationError.value = ''
  verifyingItemId.value = itemId
  try {
    const verification = await recordVerification(
      requestId,
      itemId,
      receivedQuantityByItem.value[itemId] ?? '',
      noteByItem.value[itemId] || null,
    )
    detail.value?.verifications.push(verification)
    const updated = await getDetail(requestId)
    if (detail.value) detail.value.request = updated.request
  } catch {
    verificationError.value = t('materialRequests.detail.verificationError')
  } finally {
    verifyingItemId.value = null
  }
}

async function onUploadVerificationPhoto(itemId: string, event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const verification = verificationForItem(itemId)
  if (!verification) return
  try {
    await uploadVerificationPhoto(requestId, verification.id, file)
  } catch {
    verificationError.value = t('materialRequests.detail.verificationError')
  }
}

async function toggleOrcamento(orcamentoId: string) {
  if (expandedOrcamentoId.value === orcamentoId) {
    expandedOrcamentoId.value = null
    return
  }
  expandedOrcamentoId.value = orcamentoId
  if (!orcamentoDetails.value[orcamentoId]) {
    orcamentoDetails.value[orcamentoId] = await getOrcamento(orcamentoId)
  }
}

async function onCreateOrcamento() {
  if (!detail.value) return
  orcamentoError.value = ''
  orcamentoSubmitting.value = true
  try {
    const items = detail.value.items
      .filter((item) => orcamentoPriceByItem.value[item.id])
      .map((item) => ({ materialRequestItemId: item.id, unitPrice: orcamentoPriceByItem.value[item.id] }))
    const created = await createOrcamento(requestId, items)
    orcamentos.value.push(created)
    orcamentoPriceByItem.value = {}
    showCreateOrcamentoForm.value = false
  } catch {
    orcamentoError.value = t('orcamento.form.error')
  } finally {
    orcamentoSubmitting.value = false
  }
}

async function onSendOrcamento(id: string) {
  orcamentoError.value = ''
  try {
    const updated = await sendOrcamento(id)
    orcamentos.value = orcamentos.value.map((o) => (o.id === id ? updated : o))
  } catch {
    orcamentoError.value = t('orcamento.error')
  }
}

async function onApproveOrcamento(id: string) {
  orcamentoError.value = ''
  try {
    const updated = await approveOrcamento(id)
    orcamentos.value = orcamentos.value.map((o) => (o.id === id ? updated : o))
    const updatedRequest = await getDetail(requestId)
    if (detail.value) detail.value.request = updatedRequest.request
  } catch {
    orcamentoError.value = t('orcamento.error')
  }
}

async function onRejectOrcamento(id: string) {
  orcamentoError.value = ''
  try {
    const updated = await rejectOrcamento(id, rejectOrcamentoReason.value[id] ?? '')
    orcamentos.value = orcamentos.value.map((o) => (o.id === id ? updated : o))
  } catch {
    orcamentoError.value = t('orcamento.error')
  }
}

async function onUploadOrcamentoAttachment(id: string, kind: AttachmentKind, event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    const attachment = await uploadOrcamentoAttachment(id, kind, file)
    if (orcamentoDetails.value[id]) {
      orcamentoDetails.value[id].attachments.push(attachment)
    }
  } catch {
    orcamentoError.value = t('orcamento.error')
  }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
        <button type="button" class="text-sm text-blueprint-600 dark:text-blueprint-400" @click="router.back()">
          ← {{ t('materialRequests.list.back') }}
        </button>
      </div>
    </header>

    <main v-if="detail" class="mx-auto max-w-4xl space-y-6 px-6 py-8">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">
            {{ t('materialRequests.list.requestLabel') }} #{{ detail.request.id.slice(0, 8) }}
          </h1>
          <p class="mt-1 text-sm text-steel-500 dark:text-steel-400">{{ t(`materialRequests.status.${detail.request.status}`) }}</p>
        </div>
        <div v-if="isPending" class="flex gap-2">
          <button
            type="button"
            :disabled="deciding"
            class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500"
            @click="onApprove"
          >
            {{ t('materialRequests.detail.approveButton') }}
          </button>
          <button
            type="button"
            class="rounded-md bg-safety-500 px-4 py-2 text-sm font-medium text-white transition hover:bg-safety-600"
            @click="showRejectForm = !showRejectForm"
          >
            {{ t('materialRequests.detail.rejectButton') }}
          </button>
        </div>
      </div>

      <form v-if="showRejectForm" class="space-y-2 rounded-xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800" @submit.prevent="onReject">
        <label class="block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('materialRequests.detail.rejectReasonLabel') }}</label>
        <input v-model="rejectReason" type="text" required :placeholder="t('materialRequests.detail.rejectReasonPlaceholder')" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
        <button type="submit" :disabled="deciding" class="rounded-md bg-safety-500 px-4 py-2 text-sm font-medium text-white disabled:opacity-60">
          {{ t('materialRequests.detail.confirmReject') }}
        </button>
      </form>
      <p v-if="detail.request.decisionNote" class="text-sm text-steel-500 dark:text-steel-400">
        {{ t('materialRequests.detail.decisionNote') }}: {{ detail.request.decisionNote }}
      </p>
      <p v-if="decisionError" class="text-sm text-safety-600 dark:text-safety-500">{{ decisionError }}</p>

      <section class="rounded-xl border border-steel-200 bg-white p-6 dark:border-steel-700 dark:bg-steel-800">
        <h2 class="mb-3 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('materialRequests.detail.itemsTitle') }}</h2>
        <ul class="space-y-2">
          <li v-for="item in detail.items" :key="item.id" class="rounded-md border border-steel-200 px-4 py-3 dark:border-steel-700">
            <div class="flex items-center justify-between">
              <span class="font-medium text-steel-800 dark:text-steel-50">{{ materialLabel(item.materialId) }}</span>
              <span class="text-sm text-steel-500 dark:text-steel-400">{{ t('materialRequests.form.quantity') }}: {{ item.requestedQuantity }}</span>
            </div>

            <div v-if="canVerify" class="mt-2 border-t border-steel-100 pt-2 dark:border-steel-700">
              <template v-if="verificationForItem(item.id)">
                <p class="text-sm text-steel-600 dark:text-steel-300">
                  {{ t('materialRequests.detail.alreadyVerified') }}: {{ verificationForItem(item.id)!.receivedQuantity }}
                  <span v-if="verificationForItem(item.id)!.divergent" class="ml-2 rounded-full bg-safety-500/10 px-2 py-0.5 text-xs font-medium text-safety-600 dark:text-safety-500">
                    {{ t('materialRequests.detail.divergent') }}
                  </span>
                </p>
                <label class="mt-1 inline-block text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">
                  {{ t('materialRequests.detail.uploadPhoto') }}
                  <input type="file" accept="image/*" class="hidden" @change="onUploadVerificationPhoto(item.id, $event)" />
                </label>
              </template>
              <form v-else class="mt-2 flex flex-wrap items-end gap-2" @submit.prevent="onVerify(item.id)">
                <input
                  v-model="receivedQuantityByItem[item.id]"
                  type="number"
                  step="0.001"
                  min="0"
                  required
                  :placeholder="t('materialRequests.detail.receivedQuantity')"
                  class="w-36 rounded-md border border-steel-300 bg-white px-3 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
                />
                <input
                  v-model="noteByItem[item.id]"
                  type="text"
                  :placeholder="t('materialRequests.detail.verificationNotePlaceholder')"
                  class="flex-1 rounded-md border border-steel-300 bg-white px-3 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
                />
                <button
                  type="submit"
                  :disabled="verifyingItemId === item.id"
                  class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500"
                >
                  {{ t('materialRequests.detail.verifyButton') }}
                </button>
              </form>
            </div>
          </li>
        </ul>
        <p v-if="verificationError" class="mt-2 text-sm text-safety-600 dark:text-safety-500">{{ verificationError }}</p>
      </section>

      <section class="rounded-xl border border-steel-200 bg-white p-6 dark:border-steel-700 dark:bg-steel-800">
        <div class="mb-3 flex items-center justify-between">
          <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('orcamento.title') }}</h2>
          <button
            type="button"
            class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500"
            @click="showCreateOrcamentoForm = !showCreateOrcamentoForm"
          >
            {{ t('orcamento.newButton') }}
          </button>
        </div>

        <form
          v-if="showCreateOrcamentoForm && detail"
          class="mb-4 space-y-2 rounded-md border border-steel-200 p-4 dark:border-steel-700"
          @submit.prevent="onCreateOrcamento"
        >
          <div v-for="item in detail.items" :key="item.id" class="flex items-center gap-2">
            <span class="w-48 text-sm text-steel-600 dark:text-steel-300">{{ materialLabel(item.materialId) }}</span>
            <input
              v-model="orcamentoPriceByItem[item.id]"
              type="number"
              step="0.01"
              min="0"
              :placeholder="t('orcamento.form.unitPrice')"
              class="w-32 rounded-md border border-steel-300 bg-white px-3 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
            />
          </div>
          <p v-if="orcamentoError" class="text-sm text-safety-600 dark:text-safety-500">{{ orcamentoError }}</p>
          <button type="submit" :disabled="orcamentoSubmitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
            {{ t('orcamento.form.submit') }}
          </button>
        </form>

        <p v-if="orcamentos.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('orcamento.empty') }}</p>
        <ul v-else class="space-y-2">
          <li v-for="orcamento in orcamentos" :key="orcamento.id" class="rounded-md border border-steel-200 dark:border-steel-700">
            <button type="button" class="flex w-full items-center justify-between px-4 py-3 text-left" @click="toggleOrcamento(orcamento.id)">
              <span class="font-medium text-steel-800 dark:text-steel-50">{{ t('orcamento.label') }} #{{ orcamento.id.slice(0, 8) }}</span>
              <span class="text-sm text-steel-500 dark:text-steel-400">{{ t(`orcamento.status.${orcamento.status}`) }}</span>
            </button>
            <div v-if="expandedOrcamentoId === orcamento.id && orcamentoDetails[orcamento.id]" class="space-y-3 border-t border-steel-200 p-4 dark:border-steel-700">
              <ul class="space-y-1 text-sm text-steel-600 dark:text-steel-300">
                <li v-for="line in orcamentoDetails[orcamento.id].items" :key="line.id">
                  {{ materialLabel(detail?.items.find((i) => i.id === line.materialRequestItemId)?.materialId ?? '') }}
                  — {{ t('orcamento.form.unitPrice') }}: {{ line.unitPrice }}
                </li>
              </ul>

              <div v-if="orcamento.status === 'DRAFT'">
                <button type="button" class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white dark:bg-blueprint-500" @click="onSendOrcamento(orcamento.id)">
                  {{ t('orcamento.sendButton') }}
                </button>
              </div>

              <div v-if="orcamento.status === 'SENT'" class="space-y-2">
                <button type="button" class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white dark:bg-blueprint-500" @click="onApproveOrcamento(orcamento.id)">
                  {{ t('orcamento.approveButton') }}
                </button>
                <div class="flex items-center gap-2">
                  <input
                    v-model="rejectOrcamentoReason[orcamento.id]"
                    type="text"
                    :placeholder="t('orcamento.rejectReasonPlaceholder')"
                    class="flex-1 rounded-md border border-steel-300 bg-white px-3 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50"
                  />
                  <button type="button" class="rounded-md bg-safety-500 px-3 py-1.5 text-sm font-medium text-white" @click="onRejectOrcamento(orcamento.id)">
                    {{ t('orcamento.rejectButton') }}
                  </button>
                </div>
              </div>

              <p v-if="orcamento.rejectionReason" class="text-sm text-safety-600 dark:text-safety-500">
                {{ t('orcamento.rejectionReason') }}: {{ orcamento.rejectionReason }}
              </p>

              <div class="flex flex-wrap gap-3">
                <label class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">
                  {{ t('orcamento.uploadPaymentProof') }}
                  <input type="file" class="hidden" @change="onUploadOrcamentoAttachment(orcamento.id, 'PAYMENT_PROOF', $event)" />
                </label>
                <label class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">
                  {{ t('orcamento.uploadInvoice') }}
                  <input type="file" class="hidden" @change="onUploadOrcamentoAttachment(orcamento.id, 'INVOICE', $event)" />
                </label>
              </div>

              <ul v-if="orcamentoDetails[orcamento.id].attachments.length > 0" class="text-sm text-steel-600 dark:text-steel-300">
                <li v-for="attachment in orcamentoDetails[orcamento.id].attachments" :key="attachment.id">
                  {{ t(`orcamento.attachmentKind.${attachment.kind}`) }}: {{ attachment.originalName }}
                </li>
              </ul>
            </div>
          </li>
        </ul>
      </section>
    </main>

    <p v-else-if="loadError" class="mx-auto max-w-4xl px-6 py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
  </div>
</template>
