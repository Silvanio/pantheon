<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useEquipment, type Equipment, type EquipmentStatus } from '../composables/useEquipment'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listEquipment, createEquipment, updateEquipmentStatus } = useEquipment()

const PAGE_SIZE_OPTIONS = [1, 5, 10] as const

const items = ref<Equipment[]>([])
const totalPages = ref(0)
const totalElements = ref(0)
const page = ref(0)
const pageSize = ref<number>(10)
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const name = ref('')
const type = ref('')
const status = ref<EquipmentStatus>('AVAILABLE')

const statuses: EquipmentStatus[] = ['AVAILABLE', 'IN_USE', 'MAINTENANCE', 'UNAVAILABLE']

async function load() {
  loading.value = true
  try {
    const result = await listEquipment(props.siteId, { page: page.value, size: pageSize.value })
    items.value = result.content
    totalPages.value = result.totalPages
    totalElements.value = result.totalElements
  } finally {
    loading.value = false
  }
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

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createEquipment(props.siteId, { name: name.value, type: type.value || null, status: status.value })
    name.value = ''
    type.value = ''
    status.value = 'AVAILABLE'
    showForm.value = false
    page.value = 0
    await load()
  } catch {
    errorMessage.value = t('equipment.form.error')
  } finally {
    submitting.value = false
  }
}

async function onStatusChange(item: Equipment, newStatus: EquipmentStatus) {
  try {
    await updateEquipmentStatus(item.id, newStatus)
    await load()
  } catch {
    errorMessage.value = t('equipment.status.updateError')
  }
}

onMounted(load)
</script>

<template>
  <div class="card card-pad">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h3 class="font-semibold text-steel-800 dark:text-steel-50">{{ t('equipment.title') }}</h3>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('equipment.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary px-3 py-1.5 text-xs" @click="showForm = !showForm">
        {{ t('equipment.newButton') }}
      </button>
    </div>

    <div v-if="showForm" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="showForm = false">
      <form class="modal-panel card-pad w-full max-w-md space-y-2" @submit.prevent="onSubmit">
        <h3 class="mb-1 font-semibold text-steel-800 dark:text-steel-50">{{ t('equipment.newButton') }}</h3>
        <input v-model="name" type="text" required :placeholder="t('equipment.form.name')" class="field-input" />
        <input v-model="type" type="text" :placeholder="t('equipment.form.type')" class="field-input" />
        <select v-model="status" class="field-input">
          <option v-for="s in statuses" :key="s" :value="s">{{ t(`equipment.status.${s}`) }}</option>
        </select>
        <p v-if="errorMessage" class="text-xs text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="btn-primary px-3 py-1.5 text-xs">{{ t('equipment.form.submit') }}</button>
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" @click="showForm = false">{{ t('equipment.form.cancel') }}</button>
        </div>
      </form>
    </div>

    <p v-if="!loading && items.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('equipment.empty') }}</p>
    <ul v-else class="space-y-1.5">
      <li v-for="item in items" :key="item.id" class="flex items-center justify-between rounded-lg border border-steel-200 px-3 py-2 text-sm dark:border-steel-700">
        <div>
          <span class="text-steel-800 dark:text-steel-50">{{ item.name }}</span>
          <span v-if="item.type" class="ml-1 text-steel-500 dark:text-steel-400">({{ item.type }})</span>
        </div>
        <select
          :value="item.status"
          class="rounded-md border border-steel-300 bg-white px-1.5 py-0.5 text-xs text-steel-700 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-100"
          @change="onStatusChange(item, ($event.target as HTMLSelectElement).value as EquipmentStatus)"
        >
          <option v-for="s in statuses" :key="s" :value="s">{{ t(`equipment.status.${s}`) }}</option>
        </select>
      </li>
    </ul>

    <div v-if="!loading && items.length > 0" class="mt-4 flex items-center justify-between gap-3 border-t border-steel-200 pt-3 dark:border-steel-700">
      <div class="flex items-center gap-2">
        <label class="text-xs text-steel-500 dark:text-steel-400">{{ t('common.pagination.pageSizeLabel') }}</label>
        <select v-model.number="pageSize" class="field-input w-auto py-1 text-xs" @change="onPageSizeChange">
          <option v-for="size in PAGE_SIZE_OPTIONS" :key="size" :value="size">{{ size }}</option>
        </select>
      </div>
      <div v-if="totalPages > 1" class="flex items-center gap-3">
        <p class="text-xs text-steel-500 dark:text-steel-400">
          {{ t('equipment.pagination.summary', { page: page + 1, totalPages, totalElements }) }}
        </p>
        <div class="flex gap-2">
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page === 0" @click="goToPage(page - 1)">
            {{ t('equipment.pagination.previous') }}
          </button>
          <button type="button" class="btn-secondary px-3 py-1.5 text-xs" :disabled="page >= totalPages - 1" @click="goToPage(page + 1)">
            {{ t('equipment.pagination.next') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
