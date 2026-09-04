<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useProjects, type MaterialItem } from '../composables/useProjects'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listMaterials, createMaterial } = useProjects()

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
  <div class="rounded-lg border border-steel-200 p-4 dark:border-steel-700">
    <div class="mb-3 flex items-center justify-between">
      <div>
        <h3 class="text-sm font-semibold text-steel-800 dark:text-steel-50">{{ t('materials.title') }}</h3>
        <p class="text-xs text-steel-500 dark:text-steel-400">{{ t('materials.subtitle') }}</p>
      </div>
      <button
        type="button"
        class="rounded-md bg-blueprint-600 px-2.5 py-1 text-xs font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        @click="showForm = !showForm"
      >
        {{ t('materials.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-3 space-y-2 rounded-md border border-steel-200 p-3 dark:border-steel-700" @submit.prevent="onSubmit">
      <input v-model="name" type="text" required :placeholder="t('materials.form.name')" class="w-full rounded-md border border-steel-300 bg-white px-2 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      <input v-model="unit" type="text" required :placeholder="t('materials.form.unitPlaceholder')" class="w-full rounded-md border border-steel-300 bg-white px-2 py-1.5 text-sm text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      <p v-if="errorMessage" class="text-xs text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-3 py-1 text-xs font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">{{ t('materials.form.submit') }}</button>
        <button type="button" class="rounded-md border border-steel-300 px-3 py-1 text-xs font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">{{ t('materials.form.cancel') }}</button>
      </div>
    </form>

    <p v-if="!loading && items.length === 0" class="text-xs text-steel-500 dark:text-steel-400">{{ t('materials.empty') }}</p>
    <ul v-else class="space-y-1.5">
      <li v-for="item in items" :key="item.id" class="flex items-center justify-between rounded-md border border-steel-200 px-3 py-1.5 text-sm dark:border-steel-700">
        <span class="text-steel-800 dark:text-steel-50">{{ item.name }}</span>
        <span class="text-steel-500 dark:text-steel-400">{{ item.unit }}</span>
      </li>
    </ul>
  </div>
</template>
