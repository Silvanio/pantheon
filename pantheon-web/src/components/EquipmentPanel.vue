<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useEquipmentMaterials, type Equipment, type EquipmentStatus } from '../composables/useEquipmentMaterials'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listEquipment, createEquipment, updateEquipmentStatus } = useEquipmentMaterials()

const items = ref<Equipment[]>([])
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
    items.value = await listEquipment(props.siteId)
  } finally {
    loading.value = false
  }
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

    <form v-if="showForm" class="mb-4 space-y-2 rounded-lg border border-steel-200 p-3 dark:border-steel-700" @submit.prevent="onSubmit">
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
  </div>
</template>
