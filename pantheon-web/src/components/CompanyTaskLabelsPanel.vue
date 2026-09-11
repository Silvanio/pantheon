<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTaskLabels } from '../composables/useTaskLabels'
import type { TaskLabel } from '../composables/useTaskCards'
import { useCompanies } from '../composables/useCompanies'

const props = defineProps<{ companyId: string }>()

const { t } = useI18n()
const { listCompanyLabels, createCompanyLabel, deleteCompanyLabel } = useTaskLabels()
const { listMyCompanies } = useCompanies()

const LABEL_COLORS = ['#EF4444', '#F97316', '#F59E0B', '#22C55E', '#06B6D4', '#3B82F6', '#8B5CF6', '#EC4899']

const isAdmin = ref(false)
const labels = ref<TaskLabel[]>([])
const loading = ref(false)
const newLabelName = ref('')
const newLabelColor = ref(LABEL_COLORS[0])
const submitting = ref(false)
const errorMessage = ref('')

async function load() {
  loading.value = true
  try {
    const memberships = await listMyCompanies()
    isAdmin.value = memberships.some((m) => m.companyId === props.companyId && m.role === 'ADMIN')
    if (isAdmin.value) {
      labels.value = await listCompanyLabels(props.companyId)
    }
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!newLabelName.value.trim()) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await createCompanyLabel(props.companyId, newLabelName.value.trim(), newLabelColor.value)
    newLabelName.value = ''
    await load()
  } catch {
    errorMessage.value = t('company.taskLabels.error')
  } finally {
    submitting.value = false
  }
}

async function onDelete(label: TaskLabel) {
  errorMessage.value = ''
  try {
    await deleteCompanyLabel(props.companyId, label.id)
    await load()
  } catch {
    errorMessage.value = t('company.taskLabels.error')
  }
}

onMounted(load)
</script>

<template>
  <section v-if="!loading && isAdmin" class="card card-pad">
    <h2 class="mb-1 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.taskLabels.title') }}</h2>
    <p class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('company.taskLabels.subtitle') }}</p>

    <p v-if="errorMessage" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <ul class="mb-4 flex flex-wrap gap-2">
      <li
        v-for="label in labels"
        :key="label.id"
        class="flex items-center gap-2 rounded-full py-1 pl-3 pr-1 text-xs font-medium text-white"
        :style="{ backgroundColor: label.colorHex }"
      >
        {{ label.name }}
        <button
          type="button"
          class="rounded-full px-1.5 py-0.5 leading-none opacity-80 hover:bg-black/10 hover:opacity-100"
          @click="onDelete(label)"
        >
          ×
        </button>
      </li>
    </ul>

    <form class="flex flex-wrap items-center gap-2" @submit.prevent="onCreate">
      <input v-model="newLabelName" type="text" :placeholder="t('company.taskLabels.newPlaceholder')" class="field-input flex-1" />
      <button
        v-for="color in LABEL_COLORS"
        :key="color"
        type="button"
        class="h-5 w-5 rounded-full border-2"
        :style="{ backgroundColor: color, borderColor: newLabelColor === color ? '#1F2937' : 'transparent' }"
        @click="newLabelColor = color"
      />
      <button type="submit" :disabled="submitting" class="btn-primary">{{ t('company.taskLabels.newButton') }}</button>
    </form>
  </section>
</template>
