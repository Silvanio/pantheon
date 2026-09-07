<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useEquipmentMaterials, type MaterialItem } from '../composables/useEquipmentMaterials'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listMaterials, createMaterial } = useEquipmentMaterials()

const items = ref<MaterialItem[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const name = ref('')
const unit = ref('')

async function load() {
  loading.value = true
  try {
    items.value = await listMaterials(props.siteId)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createMaterial(props.siteId, { name: name.value, unit: unit.value })
    name.value = ''
    unit.value = ''
    showForm.value = false
    await load()
  } catch {
    errorMessage.value = t('materials.form.error')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="card card-pad">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h3 class="font-semibold text-steel-800 dark:text-steel-50">{{ t('materials.title') }}</h3>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('materials.subtitle') }}</p>
      </div>
      <button type="button" class="btn-primary px-3 py-1.5 text-xs" @click="showForm = !showForm">
        {{ t('materials.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-4 space-y-2 rounded-lg border border-steel-200 p-3 dark:border-steel-700" @submit.prevent="onSubmit">
      <input v-model="name" type="text" required :placeholder="t('materials.form.name')" class="field-input" />
      <input v-model="unit" type="text" required :placeholder="t('materials.form.unitPlaceholder')" class="field-input" />
      <p v-if="errorMessage" class="text-xs text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="btn-primary px-3 py-1.5 text-xs">{{ t('materials.form.submit') }}</button>
        <button type="button" class="btn-secondary px-3 py-1.5 text-xs" @click="showForm = false">{{ t('materials.form.cancel') }}</button>
      </div>
    </form>

    <p v-if="!loading && items.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('materials.empty') }}</p>
    <ul v-else class="space-y-1.5">
      <li v-for="item in items" :key="item.id" class="flex items-center justify-between rounded-lg border border-steel-200 px-3 py-2 text-sm dark:border-steel-700">
        <span class="text-steel-800 dark:text-steel-50">{{ item.name }}</span>
        <span class="text-steel-500 dark:text-steel-400">{{ item.unit }}</span>
      </li>
    </ul>
  </div>
</template>
