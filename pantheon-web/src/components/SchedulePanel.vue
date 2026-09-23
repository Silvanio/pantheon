<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useConstructionSchedule,
  type ScheduleStage,
  type ScheduleTask,
} from '../composables/useConstructionSchedule'
import { useSiteMembers, type SiteMember } from '../composables/useSiteMembers'
import { vDatePicker } from '../lib/datePicker'
import ScheduleGantt from './schedule/ScheduleGantt.vue'

const props = defineProps<{ siteId: string; canManage: boolean; canManageTasks: boolean }>()
const emit = defineEmits<{ (e: 'open-tab', tab: 'tasks'): void }>()

const { t } = useI18n()
const {
  listStages,
  createStage,
  updateStage,
  deleteStage,
  createTask,
  updateTask,
  deleteTask,
  linkDependency,
  unlinkDependency,
  createLinkedTaskCard,
} = useConstructionSchedule()
const { listMembers } = useSiteMembers()

const STAGE_COLORS = ['#2F53F0', '#0E9F6E', '#D97C0A', '#E14F4F', '#8B5CF6', '#14B8A6', '#EC4899', '#4E546E']

const stages = ref<ScheduleStage[]>([])
const siteMembers = ref<SiteMember[]>([])
const loading = ref(false)
const loadError = ref('')
const expandedStageIds = ref<Set<string>>(new Set())

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const [stagesResult, membersResult] = await Promise.all([listStages(props.siteId), listMembers(props.siteId)])
    stages.value = stagesResult
    siteMembers.value = membersResult
  } catch {
    loadError.value = t('schedule.loadError')
  } finally {
    loading.value = false
  }
}

function toggleExpand(stageId: string) {
  const next = new Set(expandedStageIds.value)
  if (next.has(stageId)) next.delete(stageId)
  else next.add(stageId)
  expandedStageIds.value = next
}

// New stage form
const showNewStageForm = ref(false)
const newStageName = ref('')
const newStageColor = ref(STAGE_COLORS[0])
const newStageStart = ref('')
const newStageEnd = ref('')
const newStageError = ref('')
const savingStage = ref(false)

async function onCreateStage() {
  newStageError.value = ''
  savingStage.value = true
  try {
    await createStage(props.siteId, {
      name: newStageName.value,
      color: newStageColor.value,
      startDate: newStageStart.value,
      endDate: newStageEnd.value,
    })
    newStageName.value = ''
    newStageStart.value = ''
    newStageEnd.value = ''
    showNewStageForm.value = false
    await load()
  } catch {
    newStageError.value = t('schedule.stage.error')
  } finally {
    savingStage.value = false
  }
}

// New task form
const addingTaskToStageId = ref<string | null>(null)
const newTaskTitle = ref('')
const newTaskStart = ref('')
const newTaskEnd = ref('')
const newTaskResponsible = ref('')
const newTaskError = ref('')
const savingTask = ref(false)

function openAddTask(stageId: string) {
  addingTaskToStageId.value = stageId
  newTaskTitle.value = ''
  newTaskStart.value = ''
  newTaskEnd.value = ''
  newTaskResponsible.value = ''
  newTaskError.value = ''
}

async function onCreateTask() {
  if (!addingTaskToStageId.value) return
  newTaskError.value = ''
  savingTask.value = true
  try {
    await createTask(addingTaskToStageId.value, {
      title: newTaskTitle.value,
      startDate: newTaskStart.value,
      endDate: newTaskEnd.value,
      responsibleSiteMembershipId: newTaskResponsible.value || null,
    })
    addingTaskToStageId.value = null
    await load()
  } catch {
    newTaskError.value = t('schedule.task.error')
  } finally {
    savingTask.value = false
  }
}

// Stage edit
const editingStage = ref<ScheduleStage | null>(null)
const editStageName = ref('')
const editStageColor = ref('')
const editStageStart = ref('')
const editStageEnd = ref('')
const editStageError = ref('')
const confirmingDeleteStage = ref(false)

function openEditStage(stageId: string) {
  const stage = stages.value.find((s) => s.id === stageId)
  if (!stage) return
  editingStage.value = stage
  editStageName.value = stage.name
  editStageColor.value = stage.color
  editStageStart.value = stage.startDate
  editStageEnd.value = stage.endDate
  editStageError.value = ''
  confirmingDeleteStage.value = false
}

async function onSaveStage() {
  if (!editingStage.value) return
  editStageError.value = ''
  try {
    await updateStage(editingStage.value.id, {
      name: editStageName.value,
      color: editStageColor.value,
      startDate: editStageStart.value,
      endDate: editStageEnd.value,
    })
    editingStage.value = null
    await load()
  } catch {
    editStageError.value = t('schedule.stage.error')
  }
}

async function onDeleteStage() {
  if (!editingStage.value) return
  try {
    await deleteStage(editingStage.value.id)
    editingStage.value = null
    await load()
  } catch {
    editStageError.value = t('schedule.stage.deleteError')
  }
}

// Task edit
const editingTask = ref<ScheduleTask | null>(null)
const editingTaskStage = ref<ScheduleStage | null>(null)
const editTaskTitle = ref('')
const editTaskStart = ref('')
const editTaskEnd = ref('')
const editTaskResponsible = ref('')
const editTaskPercent = ref(0)
const editTaskDependsOn = ref('')
const editTaskError = ref('')
const confirmingDeleteTask = ref(false)

const otherTasksOnSite = computed(() => {
  if (!editingTask.value) return []
  return stages.value.flatMap((s) => s.tasks).filter((t) => t.id !== editingTask.value?.id)
})

function openEditTask(taskId: string) {
  for (const stage of stages.value) {
    const task = stage.tasks.find((t) => t.id === taskId)
    if (task) {
      editingTask.value = task
      editingTaskStage.value = stage
      editTaskTitle.value = task.title
      editTaskStart.value = task.startDate
      editTaskEnd.value = task.endDate
      editTaskResponsible.value = task.responsibleSiteMembershipId ?? ''
      editTaskPercent.value = task.percentComplete
      editTaskDependsOn.value = ''
      editTaskError.value = ''
      confirmingDeleteTask.value = false
      return
    }
  }
}

async function onSaveTask() {
  if (!editingTask.value) return
  editTaskError.value = ''
  try {
    await updateTask(editingTask.value.id, {
      title: editTaskTitle.value,
      startDate: editTaskStart.value,
      endDate: editTaskEnd.value,
      responsibleSiteMembershipId: editTaskResponsible.value || null,
      clearResponsible: !editTaskResponsible.value,
      percentComplete: editTaskPercent.value,
    })
    editingTask.value = null
    await load()
  } catch {
    editTaskError.value = t('schedule.task.error')
  }
}

async function onDeleteTask() {
  if (!editingTask.value) return
  try {
    await deleteTask(editingTask.value.id)
    editingTask.value = null
    await load()
  } catch {
    editTaskError.value = t('schedule.task.deleteError')
  }
}

const creatingTaskCard = ref(false)

async function onCreateLinkedTask() {
  if (!editingTask.value) return
  editTaskError.value = ''
  creatingTaskCard.value = true
  try {
    const taskId = editingTask.value.id
    await createLinkedTaskCard(taskId)
    await load()
    openEditTask(taskId)
  } catch {
    editTaskError.value = t('schedule.task.linkError')
  } finally {
    creatingTaskCard.value = false
  }
}

async function onToggleDone(taskId: string, done: boolean) {
  try {
    await updateTask(taskId, { percentComplete: done ? 100 : 0 })
    await load()
  } catch {
    loadError.value = t('schedule.task.error')
  }
}

async function onAddDependency() {
  if (!editingTask.value || !editTaskDependsOn.value) return
  editTaskError.value = ''
  try {
    await linkDependency(editingTask.value.id, editTaskDependsOn.value)
    const taskId = editingTask.value.id
    editTaskDependsOn.value = ''
    await load()
    openEditTask(taskId)
  } catch {
    editTaskError.value = t('schedule.task.dependencyError')
  }
}

async function onRemoveDependency(dependencyId: string) {
  if (!editingTask.value) return
  try {
    await unlinkDependency(editingTask.value.id, dependencyId)
    const taskId = editingTask.value.id
    await load()
    openEditTask(taskId)
  } catch {
    editTaskError.value = t('schedule.task.dependencyError')
  }
}

function taskLabel(taskId: string): string {
  return otherTasksOnSite.value.find((t) => t.id === taskId)?.title
    ?? stages.value.flatMap((s) => s.tasks).find((t) => t.id === taskId)?.title
    ?? taskId
}

onMounted(load)
</script>

<template>
  <section class="space-y-5">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('schedule.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('schedule.subtitle') }}</p>
      </div>
      <button v-if="canManage" type="button" class="btn-primary" @click="showNewStageForm = !showNewStageForm">
        {{ t('schedule.stage.newButton') }}
      </button>
    </div>

    <form v-if="showNewStageForm" class="space-y-3 rounded-lg border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onCreateStage">
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
        <div>
          <label class="field-label">{{ t('schedule.stage.name') }}</label>
          <input v-model="newStageName" type="text" required class="field-input" />
        </div>
        <div>
          <label class="field-label">{{ t('schedule.stage.color') }}</label>
          <div class="flex gap-1.5 pt-1.5">
            <button
              v-for="color in STAGE_COLORS"
              :key="color"
              type="button"
              class="h-6 w-6 rounded-full ring-offset-2"
              :class="{ 'ring-2 ring-blueprint-500': newStageColor === color }"
              :style="{ backgroundColor: color }"
              @click="newStageColor = color"
            ></button>
          </div>
        </div>
        <div>
          <label class="field-label">{{ t('schedule.stage.startDate') }}</label>
          <input v-model="newStageStart" v-date-picker type="date" required class="field-input" />
        </div>
        <div>
          <label class="field-label">{{ t('schedule.stage.endDate') }}</label>
          <input v-model="newStageEnd" v-date-picker type="date" required class="field-input" />
        </div>
      </div>
      <p v-if="newStageError" class="text-sm text-safety-600 dark:text-safety-500">{{ newStageError }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="savingStage" class="btn-primary">{{ t('schedule.stage.save') }}</button>
        <button type="button" class="btn-secondary" @click="showNewStageForm = false">{{ t('schedule.form.cancel') }}</button>
      </div>
    </form>

    <p v-if="loadError" class="text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>

    <p v-if="!loading && stages.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('schedule.empty') }}</p>

    <ScheduleGantt
      v-else
      :stages="stages"
      :expanded-stage-ids="expandedStageIds"
      :can-manage="canManage"
      @select-stage="openEditStage"
      @select-task="openEditTask"
      @toggle-expand="toggleExpand"
      @toggle-done="onToggleDone"
      @add-task="openAddTask"
    />

    <!-- New task modal -->
    <div v-if="addingTaskToStageId" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="addingTaskToStageId = null">
      <div class="modal-panel card-pad w-full max-w-md">
        <h3 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('schedule.task.newTitle') }}</h3>
        <form class="space-y-3" @submit.prevent="onCreateTask">
          <div>
            <label class="field-label">{{ t('schedule.task.title') }}</label>
            <input v-model="newTaskTitle" type="text" required class="field-input" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="field-label">{{ t('schedule.stage.startDate') }}</label>
              <input v-model="newTaskStart" v-date-picker type="date" required class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('schedule.stage.endDate') }}</label>
              <input v-model="newTaskEnd" v-date-picker type="date" required class="field-input" />
            </div>
          </div>
          <div>
            <label class="field-label">{{ t('schedule.task.responsible') }}</label>
            <select v-model="newTaskResponsible" class="field-input">
              <option value="">{{ t('schedule.task.unassigned') }}</option>
              <option v-for="member in siteMembers" :key="member.membershipId" :value="member.membershipId">{{ member.displayName }}</option>
            </select>
          </div>
          <p v-if="newTaskError" class="text-sm text-safety-600 dark:text-safety-500">{{ newTaskError }}</p>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="btn-secondary" @click="addingTaskToStageId = null">{{ t('schedule.form.cancel') }}</button>
            <button type="submit" :disabled="savingTask" class="btn-primary">{{ t('schedule.stage.save') }}</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit stage modal -->
    <div v-if="editingStage" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="editingStage = null">
      <div class="modal-panel card-pad w-full max-w-md">
        <h3 class="mb-4 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('schedule.stage.editTitle') }}</h3>
        <form class="space-y-3" @submit.prevent="onSaveStage">
          <div>
            <label class="field-label">{{ t('schedule.stage.name') }}</label>
            <input v-model="editStageName" type="text" required :disabled="!canManage" class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('schedule.stage.color') }}</label>
            <div class="flex gap-1.5 pt-1.5">
              <button
                v-for="color in STAGE_COLORS"
                :key="color"
                type="button"
                :disabled="!canManage"
                class="h-6 w-6 rounded-full"
                :class="{ 'ring-2 ring-blueprint-500 ring-offset-2': editStageColor === color }"
                :style="{ backgroundColor: color }"
                @click="editStageColor = color"
              ></button>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="field-label">{{ t('schedule.stage.startDate') }}</label>
              <input v-model="editStageStart" v-date-picker type="date" required :disabled="!canManage" class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('schedule.stage.endDate') }}</label>
              <input v-model="editStageEnd" v-date-picker type="date" required :disabled="!canManage" class="field-input" />
            </div>
          </div>
          <p v-if="editStageError" class="text-sm text-safety-600 dark:text-safety-500">{{ editStageError }}</p>
          <div class="flex items-center justify-between gap-2 pt-1">
            <div v-if="canManage" class="relative">
              <button type="button" class="btn-danger py-1.5 text-xs" @click="confirmingDeleteStage = !confirmingDeleteStage">
                {{ t('schedule.stage.deleteButton') }}
              </button>
              <div v-if="confirmingDeleteStage" class="modal-panel absolute left-0 top-full z-10 mt-2 w-72 p-3 shadow-lg">
                <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('schedule.stage.deleteConfirm') }}</p>
                <div class="flex justify-end gap-2">
                  <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDeleteStage = false">{{ t('schedule.form.cancel') }}</button>
                  <button type="button" class="btn-danger py-1 text-xs" @click="onDeleteStage">{{ t('schedule.stage.deleteButton') }}</button>
                </div>
              </div>
            </div>
            <div class="ml-auto flex gap-2">
              <button type="button" class="btn-secondary" @click="editingStage = null">{{ t('schedule.form.cancel') }}</button>
              <button v-if="canManage" type="submit" class="btn-primary">{{ t('schedule.stage.save') }}</button>
            </div>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit task modal -->
    <div v-if="editingTask" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="editingTask = null">
      <div class="modal-panel card-pad max-h-[85vh] w-full max-w-md overflow-y-auto">
        <h3 class="mb-1 text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('schedule.task.editTitle') }}</h3>
        <p class="mb-4 text-xs text-steel-500 dark:text-steel-400">{{ editingTaskStage?.name }}</p>
        <form class="space-y-3" @submit.prevent="onSaveTask">
          <div>
            <label class="field-label">{{ t('schedule.task.title') }}</label>
            <input v-model="editTaskTitle" type="text" required :disabled="!canManage" class="field-input" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="field-label">{{ t('schedule.stage.startDate') }}</label>
              <input v-model="editTaskStart" v-date-picker type="date" required :disabled="!canManage" class="field-input" />
            </div>
            <div>
              <label class="field-label">{{ t('schedule.stage.endDate') }}</label>
              <input v-model="editTaskEnd" v-date-picker type="date" required :disabled="!canManage" class="field-input" />
            </div>
          </div>
          <div>
            <label class="field-label">{{ t('schedule.task.responsible') }}</label>
            <select v-model="editTaskResponsible" :disabled="!canManage" class="field-input">
              <option value="">{{ t('schedule.task.unassigned') }}</option>
              <option v-for="member in siteMembers" :key="member.membershipId" :value="member.membershipId">{{ member.displayName }}</option>
            </select>
          </div>
          <div>
            <label class="field-label">{{ t('schedule.task.percentComplete') }}: {{ editTaskPercent }}%</label>
            <input v-model.number="editTaskPercent" type="range" min="0" max="100" step="5" :disabled="!canManage" class="w-full" />
          </div>

          <div class="border-t border-steel-100 pt-3 dark:border-steel-800">
            <p class="field-label mb-1">{{ t('schedule.task.dependsOn') }}</p>
            <p class="mb-2 text-xs text-steel-400 dark:text-steel-500">{{ t('schedule.task.dependsOnHint') }}</p>
            <ul v-if="editingTask.dependsOn.length > 0" class="mb-2 space-y-1">
              <li v-for="dep in editingTask.dependsOn" :key="dep.id" class="flex items-center justify-between rounded-md border border-steel-200 px-2.5 py-1.5 text-xs dark:border-steel-700">
                <span class="text-steel-700 dark:text-steel-200">{{ taskLabel(dep.predecessorTaskId) }}</span>
                <button v-if="canManage" type="button" class="text-safety-600 hover:underline dark:text-safety-500" @click="onRemoveDependency(dep.id)">
                  {{ t('schedule.task.removeDependency') }}
                </button>
              </li>
            </ul>
            <div v-if="canManage && otherTasksOnSite.length > 0" class="flex gap-2">
              <select v-model="editTaskDependsOn" class="field-input flex-1">
                <option value="">{{ t('schedule.task.selectDependency') }}</option>
                <option v-for="task in otherTasksOnSite" :key="task.id" :value="task.id">{{ task.title }}</option>
              </select>
              <button type="button" class="btn-secondary py-1.5 text-xs" @click="onAddDependency">{{ t('schedule.task.addDependency') }}</button>
            </div>
          </div>

          <div class="border-t border-steel-100 pt-3 dark:border-steel-800">
            <p class="field-label mb-1.5">{{ t('schedule.task.tasksBoardLabel') }}</p>
            <button
              v-if="editingTask.taskCardId"
              type="button"
              class="flex items-center gap-1.5 text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400"
              @click="emit('open-tab', 'tasks')"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5 shrink-0"><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M9 3v18M3 9h6" /></svg>
              {{ editingTask.taskCardTitle ?? t('schedule.task.linkedTaskFallbackLabel') }}
            </button>
            <button
              v-else-if="canManageTasks"
              type="button"
              :disabled="creatingTaskCard"
              class="btn-secondary py-1.5 text-xs"
              @click="onCreateLinkedTask"
            >
              {{ creatingTaskCard ? t('schedule.task.creatingLinkedTask') : t('schedule.task.createLinkedTask') }}
            </button>
            <p v-else class="text-xs text-steel-400 dark:text-steel-500">{{ t('schedule.task.noLinkedTask') }}</p>
          </div>

          <p v-if="editTaskError" class="text-sm text-safety-600 dark:text-safety-500">{{ editTaskError }}</p>
          <div class="flex items-center justify-between gap-2 pt-1">
            <div v-if="canManage" class="relative">
              <button type="button" class="btn-danger py-1.5 text-xs" @click="confirmingDeleteTask = !confirmingDeleteTask">
                {{ t('schedule.task.deleteButton') }}
              </button>
              <div v-if="confirmingDeleteTask" class="modal-panel absolute left-0 top-full z-10 mt-2 w-72 p-3 shadow-lg">
                <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('schedule.task.deleteConfirm') }}</p>
                <div class="flex justify-end gap-2">
                  <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDeleteTask = false">{{ t('schedule.form.cancel') }}</button>
                  <button type="button" class="btn-danger py-1 text-xs" @click="onDeleteTask">{{ t('schedule.task.deleteButton') }}</button>
                </div>
              </div>
            </div>
            <div class="ml-auto flex gap-2">
              <button type="button" class="btn-secondary" @click="editingTask = null">{{ t('schedule.form.cancel') }}</button>
              <button v-if="canManage" type="submit" class="btn-primary">{{ t('schedule.stage.save') }}</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>
