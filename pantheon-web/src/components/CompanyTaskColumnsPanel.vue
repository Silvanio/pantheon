<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTaskColumns, type TaskColumn } from '../composables/useTaskColumns'
import { useCompanies } from '../composables/useCompanies'

const props = defineProps<{ companyId: string }>()

const { t } = useI18n()
const { listColumns, createColumn, renameColumn, reorderColumn, deleteColumn } = useTaskColumns()
const { listMyCompanies } = useCompanies()

const isAdmin = ref(false)
const columns = ref<TaskColumn[]>([])
const loading = ref(false)
const newColumnName = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const editingId = ref<string | null>(null)
const editingName = ref('')

const sortedColumns = computed(() => [...columns.value].sort((a, b) => a.sortOrder - b.sortOrder))

async function load() {
  loading.value = true
  try {
    const memberships = await listMyCompanies()
    isAdmin.value = memberships.some((m) => m.companyId === props.companyId && m.role === 'ADMIN')
    if (isAdmin.value) {
      columns.value = await listColumns(props.companyId)
    }
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  if (!newColumnName.value.trim()) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await createColumn(props.companyId, newColumnName.value.trim())
    newColumnName.value = ''
    await load()
  } catch {
    errorMessage.value = t('company.taskColumns.error')
  } finally {
    submitting.value = false
  }
}

function startEdit(column: TaskColumn) {
  editingId.value = column.id
  editingName.value = column.name
}

async function onRename(column: TaskColumn) {
  if (!editingName.value.trim()) return
  errorMessage.value = ''
  try {
    await renameColumn(props.companyId, column.id, editingName.value.trim())
    editingId.value = null
    await load()
  } catch {
    errorMessage.value = t('company.taskColumns.error')
  }
}

async function onMove(column: TaskColumn, direction: -1 | 1) {
  const index = sortedColumns.value.findIndex((c) => c.id === column.id)
  const target = sortedColumns.value[index + direction]
  if (!target) return
  errorMessage.value = ''
  try {
    await Promise.all([
      reorderColumn(props.companyId, column.id, target.sortOrder),
      reorderColumn(props.companyId, target.id, column.sortOrder),
    ])
    await load()
  } catch {
    errorMessage.value = t('company.taskColumns.error')
  }
}

async function onDelete(column: TaskColumn) {
  errorMessage.value = ''
  try {
    await deleteColumn(props.companyId, column.id)
    await load()
  } catch {
    errorMessage.value = t('company.taskColumns.inUseError')
  }
}

onMounted(load)
</script>

<template>
  <section v-if="!loading && isAdmin" class="card card-pad">
    <h2 class="mb-1 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('company.taskColumns.title') }}</h2>
    <p class="mb-4 text-sm text-steel-500 dark:text-steel-400">{{ t('company.taskColumns.subtitle') }}</p>

    <p v-if="errorMessage" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <ul class="mb-4 space-y-2">
      <li
        v-for="(column, index) in sortedColumns"
        :key="column.id"
        class="flex items-center justify-between gap-3 rounded-lg border border-steel-200 px-3 py-2 dark:border-steel-700"
      >
        <template v-if="editingId === column.id">
          <input v-model="editingName" type="text" class="field-input flex-1" @keyup.enter="onRename(column)" />
          <button type="button" class="btn-primary py-1 text-xs" @click="onRename(column)">{{ t('company.taskColumns.save') }}</button>
          <button type="button" class="btn-secondary py-1 text-xs" @click="editingId = null">{{ t('company.taskColumns.cancel') }}</button>
        </template>
        <template v-else>
          <span class="flex-1 truncate text-sm font-medium text-steel-800 dark:text-steel-50">{{ column.name }}</span>
          <button type="button" class="btn-ghost px-2 py-1 text-xs" :disabled="index === 0" @click="onMove(column, -1)">↑</button>
          <button
            type="button"
            class="btn-ghost px-2 py-1 text-xs"
            :disabled="index === sortedColumns.length - 1"
            @click="onMove(column, 1)"
          >
            ↓
          </button>
          <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="startEdit(column)">{{ t('company.taskColumns.rename') }}</button>
          <button type="button" class="btn-ghost px-2 py-1 text-xs text-safety-600 dark:text-safety-500" @click="onDelete(column)">
            {{ t('company.taskColumns.delete') }}
          </button>
        </template>
      </li>
    </ul>

    <form class="flex gap-2" @submit.prevent="onCreate">
      <input v-model="newColumnName" type="text" :placeholder="t('company.taskColumns.newPlaceholder')" class="field-input flex-1" />
      <button type="submit" :disabled="submitting" class="btn-primary">{{ t('company.taskColumns.newButton') }}</button>
    </form>
  </section>
</template>
