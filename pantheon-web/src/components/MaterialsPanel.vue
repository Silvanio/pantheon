<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMaterialDeliveries, type Material } from '../composables/useMaterialDeliveries'
import StatusBadge from './StatusBadge.vue'

const props = defineProps<{ siteId: string; canManage: boolean }>()

const { t } = useI18n()
const { listMaterials, markDelivered, markChecked } = useMaterialDeliveries()

const PAGE_SIZE_OPTIONS = [5, 10, 20] as const

const materials = ref<Material[]>([])
const loading = ref(false)
const errorMessage = ref('')
const photosByMaterial = ref<Record<string, File[]>>({})
const markingId = ref<string | null>(null)

const page = ref(0)
const pageSize = ref<number>(10)

const totalElements = computed(() => materials.value.length)
const totalPages = computed(() => Math.max(1, Math.ceil(totalElements.value / pageSize.value)))
const paginatedMaterials = computed(() => {
  const start = page.value * pageSize.value
  return materials.value.slice(start, start + pageSize.value)
})

async function load() {
  loading.value = true
  try {
    materials.value = await listMaterials(props.siteId)
    if (page.value >= totalPages.value) {
      page.value = 0
    }
  } finally {
    loading.value = false
  }
}

function goToPage(target: number) {
  if (target < 0 || target >= totalPages.value) return
  page.value = target
}

function onPageSizeChange() {
  page.value = 0
}

function photoCount(materialId: string): number {
  return photosByMaterial.value[materialId]?.length ?? 0
}

function onPhotosSelected(materialId: string, event: Event) {
  const files = (event.target as HTMLInputElement).files
  photosByMaterial.value[materialId] = files ? Array.from(files) : []
}

async function onMarkDelivered(materialId: string) {
  errorMessage.value = ''
  markingId.value = materialId
  try {
    await markDelivered(materialId)
    await load()
  } catch {
    errorMessage.value = t('materialDelivery.error')
  } finally {
    markingId.value = null
  }
}

async function onMarkChecked(materialId: string) {
  errorMessage.value = ''
  markingId.value = materialId
  try {
    await markChecked(materialId, photosByMaterial.value[materialId] ?? [])
    await load()
  } catch {
    errorMessage.value = t('materialDelivery.error')
  } finally {
    markingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <section class="space-y-5">
    <div>
      <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('materialDelivery.title') }}</h2>
      <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('materialDelivery.subtitle') }}</p>
    </div>

    <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <p v-if="!loading && materials.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('materialDelivery.empty') }}
    </p>
    <template v-else>
      <div class="overflow-hidden rounded-2xl border border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-900">
        <table class="w-full border-collapse">
          <thead>
            <tr>
              <th class="pb-3 pl-5 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('materialDelivery.table.material') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('materialDelivery.table.quantity') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('materialDelivery.table.purchaseRequest') }}</th>
              <th class="pb-3 pt-4 text-left text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('materialDelivery.table.status') }}</th>
              <th v-if="canManage" class="pb-3 pr-5 pt-4 text-right text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('materialDelivery.table.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="material in paginatedMaterials"
              :key="material.id"
              class="border-t border-steel-100 transition hover:bg-steel-50 dark:border-steel-800 dark:hover:bg-steel-800/40"
            >
              <td class="py-3.5 pl-5 text-[13.5px] font-bold text-steel-800 dark:text-steel-50">
                {{ material.name }}
                <span v-if="material.type" class="font-normal text-steel-500 dark:text-steel-400"> · {{ material.type }}</span>
              </td>
              <td class="py-3.5 text-[13px] text-steel-600 dark:text-steel-300">{{ material.quantity }}</td>
              <td class="py-3.5 text-[13px]">
                <router-link
                  v-if="material.sourcePurchaseRequestId"
                  :to="`/purchase-requests/${material.sourcePurchaseRequestId}`"
                  class="font-medium text-blueprint-600 hover:underline dark:text-blueprint-400"
                >
                  {{ material.sourcePurchaseRequestName ?? t('materialDelivery.table.purchaseRequest') }}
                </router-link>
                <span v-else class="text-steel-400 dark:text-steel-500">—</span>
              </td>
              <td class="py-3.5">
                <StatusBadge kind="material" :status="material.status" />
              </td>
              <td v-if="canManage" class="py-3.5 pr-5">
                <div class="flex items-center justify-end gap-1.5">
                  <button
                    v-if="material.status === 'AWAITING_DELIVERY'"
                    type="button"
                    :disabled="markingId === material.id"
                    :title="t('materialDelivery.markDeliveredButton')"
                    class="inline-flex h-8 w-8 items-center justify-center rounded-md text-blueprint-600 transition hover:bg-blueprint-50 disabled:opacity-50 dark:text-blueprint-400 dark:hover:bg-blueprint-900/30"
                    @click="onMarkDelivered(material.id)"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M3 7h11v8H3V7zM14 10h4l3 3v2h-7v-5zM6.5 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3zM17.5 19a1.5 1.5 0 100-3 1.5 1.5 0 000 3z" />
                    </svg>
                  </button>
                  <template v-else-if="material.status === 'DELIVERED'">
                    <label
                      class="relative inline-flex h-8 w-8 cursor-pointer items-center justify-center rounded-md text-steel-500 transition hover:bg-steel-100 dark:text-steel-400 dark:hover:bg-steel-800"
                      :title="t('materialDelivery.uploadPhotoLabel')"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M4 7h3l2-2h6l2 2h3a1 1 0 011 1v11a1 1 0 01-1 1H4a1 1 0 01-1-1V8a1 1 0 011-1z" />
                        <circle cx="12" cy="13" r="3.5" />
                      </svg>
                      <span
                        v-if="photoCount(material.id) > 0"
                        class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-blueprint-600 text-[9px] font-bold text-white"
                      >
                        {{ photoCount(material.id) }}
                      </span>
                      <input type="file" accept="image/*" multiple class="hidden" @change="onPhotosSelected(material.id, $event)" />
                    </label>
                    <button
                      type="button"
                      :disabled="markingId === material.id"
                      :title="t('materialDelivery.markCheckedButton')"
                      class="inline-flex h-8 w-8 items-center justify-center rounded-md text-emerald-600 transition hover:bg-emerald-50 disabled:opacity-50 dark:text-emerald-400 dark:hover:bg-emerald-900/30"
                      @click="onMarkChecked(material.id)"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </button>
                  </template>
                  <span v-else class="text-steel-300 dark:text-steel-600">—</span>
                </div>
              </td>
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
            {{ t('materialDelivery.pagination.summary', { page: page + 1, totalPages, totalElements }) }}
          </p>
          <div class="flex gap-2">
            <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page === 0" @click="goToPage(page - 1)">
              {{ t('materialDelivery.pagination.previous') }}
            </button>
            <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
              {{ t('materialDelivery.pagination.next') }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>
