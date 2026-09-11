<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { usePurchaseRequests, type PurchaseRequestDetail } from '../composables/usePurchaseRequests'
import type { FornecedorInput } from '../composables/useFornecedores'
import FornecedorPicker from '../components/FornecedorPicker.vue'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getPurchaseRequest, convertToOrcamento } = usePurchaseRequests()

const purchaseRequestId = route.params.id as string
const detail = ref<PurchaseRequestDetail | null>(null)
const loading = ref(false)
const loadError = ref('')

const selectedIds = ref<Set<string>>(new Set())
const showFornecedorPicker = ref(false)
const converting = ref(false)
const convertError = ref('')

const pendingItems = computed(() => detail.value?.items.filter((i) => i.status === 'PENDING') ?? [])
const convertedItems = computed(() => detail.value?.items.filter((i) => i.status === 'CONVERTED') ?? [])

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getPurchaseRequest(purchaseRequestId)
  } catch {
    loadError.value = t('purchaseRequests.convertError')
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
      <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">{{ detail.purchaseRequest.name }}</h1>

      <section class="card card-pad">
        <div class="mb-5 flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-sm font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
            {{ t('purchaseRequests.pendingTitle') }}
          </h2>
          <button
            v-if="selectedIds.size > 0 && !showFornecedorPicker"
            type="button"
            :disabled="converting"
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

        <p v-if="!loading && pendingItems.length === 0" class="mb-5 text-sm text-steel-500 dark:text-steel-400">
          {{ t('purchaseRequests.empty') }}
        </p>
        <ul v-else class="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <li
            v-for="item in pendingItems"
            :key="item.id"
            class="flex items-center gap-3 rounded-lg border px-4 py-3 transition"
            :class="selectedIds.has(item.id) ? 'border-blueprint-500 bg-blueprint-50 dark:bg-blueprint-900/30' : 'border-steel-200 dark:border-steel-700'"
          >
            <input type="checkbox" :checked="selectedIds.has(item.id)" class="h-4 w-4 shrink-0" @change="toggleSelection(item.id)" />
            <div class="min-w-0 flex-1">
              <p class="truncate font-medium text-steel-800 dark:text-steel-50">{{ item.name }}</p>
              <p class="truncate text-sm text-steel-500 dark:text-steel-400">
                {{ item.type ? `${item.type} — ` : '' }}{{ item.quantity }}{{ item.unit ? ` ${item.unit}` : '' }}
              </p>
            </div>
          </li>
        </ul>

        <template v-if="convertedItems.length > 0">
          <h2 class="mb-3 text-sm font-semibold uppercase tracking-wide text-steel-500 dark:text-steel-400">
            {{ t('purchaseRequests.convertedTitle') }}
          </h2>
          <ul class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <li v-for="item in convertedItems" :key="item.id" class="flex items-center justify-between rounded-lg border border-steel-200 px-4 py-3 text-sm opacity-70 dark:border-steel-700">
              <span class="truncate text-steel-700 dark:text-steel-200">{{ item.name }}</span>
              <router-link :to="`/orcamentos/${item.convertedToOrcamentoId}`" class="shrink-0 text-blueprint-600 hover:underline dark:text-blueprint-400">
                {{ t('orcamento.label') }}
              </router-link>
            </li>
          </ul>
        </template>
      </section>
    </main>

    <p v-else-if="loadError" class="app-container max-w-5xl! py-8 text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>
  </div>
</template>
