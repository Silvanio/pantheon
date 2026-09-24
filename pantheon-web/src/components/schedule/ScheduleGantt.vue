<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ScheduleStage } from '../../composables/useConstructionSchedule'

const props = defineProps<{ stages: ScheduleStage[]; expandedStageIds: Set<string>; canManage: boolean }>()
const emit = defineEmits<{
  (e: 'select-stage', stageId: string): void
  (e: 'select-task', taskId: string): void
  (e: 'toggle-expand', stageId: string): void
  (e: 'toggle-done', taskId: string, done: boolean): void
  (e: 'add-task', stageId: string): void
}>()

const { locale } = useI18n()

const visibleMonth = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1))
const gridEl = ref<HTMLElement | null>(null)
const barRefs = ref<Record<string, HTMLElement>>({})
const svgLines = ref<{ x1: number; y1: number; x2: number; y2: number }[]>([])

const WEEKDAY_LABELS = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb']

/** Local-date ISO string (unlike `Date#toISOString`, never rolls to an adjacent day based on timezone offset). */
function toLocalIso(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const daysInMonth = computed(() => {
  const year = visibleMonth.value.getFullYear()
  const month = visibleMonth.value.getMonth()
  const count = new Date(year, month + 1, 0).getDate()
  return Array.from({ length: count }, (_, i) => new Date(year, month, i + 1))
})

const monthLabel = computed(() =>
  new Intl.DateTimeFormat(locale.value || 'pt-BR', { month: 'long', year: 'numeric' }).format(visibleMonth.value),
)

const todayIso = toLocalIso(new Date())

function prevMonth() {
  visibleMonth.value = new Date(visibleMonth.value.getFullYear(), visibleMonth.value.getMonth() - 1, 1)
}
function nextMonthFn() {
  visibleMonth.value = new Date(visibleMonth.value.getFullYear(), visibleMonth.value.getMonth() + 1, 1)
}
function goToday() {
  visibleMonth.value = new Date(new Date().getFullYear(), new Date().getMonth(), 1)
}

/** Clamps [start, end] to the visible month, returning 1-based day indices, or null if no overlap. */
function clampToMonth(startIso: string, endIso: string): { startDay: number; endDay: number } | null {
  const monthStart = visibleMonth.value
  const monthEnd = daysInMonth.value[daysInMonth.value.length - 1]
  const start = new Date(startIso + 'T00:00:00')
  const end = new Date(endIso + 'T00:00:00')
  if (end < monthStart || start > monthEnd) return null
  const clampedStart = start < monthStart ? monthStart : start
  const clampedEnd = end > monthEnd ? monthEnd : end
  return { startDay: clampedStart.getDate(), endDay: clampedEnd.getDate() }
}

interface GanttRow {
  kind: 'stage' | 'task' | 'add-task'
  id: string
  stageId: string
  label: string
  color: string
  percentComplete: number
  startDate: string
  endDate: string
  done?: boolean
}

const rows = computed<GanttRow[]>(() => {
  const result: GanttRow[] = []
  for (const stage of props.stages) {
    result.push({
      kind: 'stage',
      id: stage.id,
      stageId: stage.id,
      label: stage.name,
      color: stage.color,
      percentComplete: stage.percentComplete,
      startDate: stage.startDate,
      endDate: stage.endDate,
    })
    if (props.expandedStageIds.has(stage.id)) {
      for (const task of stage.tasks) {
        result.push({
          kind: 'task',
          id: task.id,
          stageId: stage.id,
          label: task.title,
          color: stage.color,
          percentComplete: task.percentComplete,
          startDate: task.startDate,
          endDate: task.endDate,
          done: task.percentComplete >= 100,
        })
      }
      if (props.canManage) {
        result.push({
          kind: 'add-task',
          id: 'add-task-' + stage.id,
          stageId: stage.id,
          label: '',
          color: stage.color,
          percentComplete: 0,
          startDate: '',
          endDate: '',
        })
      }
    }
  }
  return result
})

function onBarClick(row: GanttRow) {
  if (row.kind === 'stage') emit('select-stage', row.id)
  else if (row.kind === 'task') emit('select-task', row.id)
}

function setBarRef(id: string, el: unknown) {
  if (el instanceof HTMLElement) barRefs.value[id] = el
  else delete barRefs.value[id]
}

async function recomputeLines() {
  await nextTick()
  if (!gridEl.value) {
    svgLines.value = []
    return
  }
  const containerRect = gridEl.value.getBoundingClientRect()
  const lines: { x1: number; y1: number; x2: number; y2: number }[] = []
  for (const stage of props.stages) {
    if (!props.expandedStageIds.has(stage.id)) continue
    for (const task of stage.tasks) {
      for (const dep of task.dependsOn) {
        const fromEl = barRefs.value[dep.predecessorTaskId]
        const toEl = barRefs.value[task.id]
        if (!fromEl || !toEl) continue
        const fromRect = fromEl.getBoundingClientRect()
        const toRect = toEl.getBoundingClientRect()
        lines.push({
          x1: fromRect.right - containerRect.left,
          y1: fromRect.top - containerRect.top + fromRect.height / 2,
          x2: toRect.left - containerRect.left,
          y2: toRect.top - containerRect.top + toRect.height / 2,
        })
      }
    }
  }
  svgLines.value = lines
}

let resizeObserver: ResizeObserver | null = null
onMounted(() => {
  recomputeLines()
  resizeObserver = new ResizeObserver(() => recomputeLines())
  if (gridEl.value) resizeObserver.observe(gridEl.value)
})
onUnmounted(() => resizeObserver?.disconnect())
watch([rows, visibleMonth], recomputeLines, { flush: 'post' })
</script>

<template>
  <div class="schedule-gantt">
    <div class="mb-3 flex items-center justify-between gap-2">
      <div class="flex items-center gap-1.5">
        <button type="button" class="btn-ghost h-8 w-8 !p-0" @click="prevMonth">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4"><path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" /></svg>
        </button>
        <span class="min-w-[140px] text-center text-sm font-bold capitalize text-steel-800 dark:text-steel-50">{{ monthLabel }}</span>
        <button type="button" class="btn-ghost h-8 w-8 !p-0" @click="nextMonthFn">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4"><path stroke-linecap="round" stroke-linejoin="round" d="M9 18l6-6-6-6" /></svg>
        </button>
      </div>
      <button type="button" class="btn-secondary py-1 text-xs" @click="goToday">Hoje</button>
    </div>

    <div class="relative overflow-x-auto rounded-lg border border-steel-200 dark:border-steel-700">
      <div
        ref="gridEl"
        class="grid"
        :style="{ gridTemplateColumns: `200px repeat(${daysInMonth.length}, minmax(26px, 1fr))`, gridAutoRows: '36px' }"
      >
        <!-- Header row -->
        <div class="sticky left-0 z-10 border-b border-r border-steel-200 bg-steel-50 dark:border-steel-700 dark:bg-steel-800" style="grid-row: 1; grid-column: 1"></div>
        <div
          v-for="(day, i) in daysInMonth"
          :key="'h' + i"
          class="flex flex-col items-center justify-center border-b border-steel-200 text-[10px] font-semibold dark:border-steel-700"
          :class="toLocalIso(day) === todayIso ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-900/40 dark:text-blueprint-300' : 'bg-steel-50 text-steel-500 dark:bg-steel-800 dark:text-steel-400'"
          :style="{ gridRow: 1, gridColumn: i + 2 }"
        >
          <span>{{ WEEKDAY_LABELS[day.getDay()] }}</span>
          <span class="font-bold">{{ day.getDate() }}</span>
        </div>

        <!-- Today column highlight -->
        <div
          v-for="(day, i) in daysInMonth"
          v-show="toLocalIso(day) === todayIso"
          :key="'t' + i"
          class="pointer-events-none bg-blueprint-50/60 dark:bg-blueprint-900/20"
          :style="{ gridRow: `2 / ${rows.length + 2}`, gridColumn: i + 2 }"
        ></div>

        <template v-for="(row, rowIndex) in rows" :key="row.id">
          <div
            class="sticky left-0 z-10 flex items-center gap-1.5 truncate border-b border-r border-steel-200 bg-white px-2 text-xs dark:border-steel-700 dark:bg-steel-900"
            :class="row.kind === 'task' ? 'pl-5 text-steel-600 dark:text-steel-300' : row.kind === 'add-task' ? 'pl-5' : 'font-bold text-steel-800 dark:text-steel-50'"
            :style="{ gridRow: rowIndex + 2, gridColumn: 1 }"
          >
            <button
              v-if="row.kind === 'stage'"
              type="button"
              class="flex h-4 w-4 shrink-0 items-center justify-center text-steel-400"
              @click="emit('toggle-expand', row.id)"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"
                class="h-3 w-3 transition-transform"
                :class="{ 'rotate-90': expandedStageIds.has(row.id) }"
              ><path stroke-linecap="round" stroke-linejoin="round" d="M9 18l6-6-6-6" /></svg>
            </button>
            <input
              v-else-if="row.kind === 'task' && canManage"
              type="checkbox"
              class="field-checkbox h-3.5 w-3.5"
              :checked="row.done"
              @change="emit('toggle-done', row.id, ($event.target as HTMLInputElement).checked)"
            />
            <span v-if="row.kind !== 'add-task'" class="h-2 w-2 shrink-0 rounded-full" :style="{ backgroundColor: row.color }"></span>
            <button v-if="row.kind === 'add-task'" type="button" class="truncate text-blueprint-600 hover:underline dark:text-blueprint-400" @click="emit('add-task', row.stageId)">
              + Nova tarefa
            </button>
            <button v-else type="button" class="truncate text-left hover:underline" @click="onBarClick(row)">{{ row.label }}</button>
          </div>
          <div
            v-for="(_, i) in daysInMonth"
            :key="row.id + '-' + i"
            class="border-b border-steel-100 dark:border-steel-800"
            :style="{ gridRow: rowIndex + 2, gridColumn: i + 2 }"
          ></div>
          <button
            v-if="row.kind !== 'add-task' && clampToMonth(row.startDate, row.endDate)"
            type="button"
            :ref="(el) => setBarRef(row.id, el)"
            class="z-[1] my-1.5 flex items-center justify-end overflow-hidden rounded-md px-1.5 text-[10px] font-bold text-white shadow-sm transition hover:brightness-110"
            :style="{
              gridRow: rowIndex + 2,
              gridColumn: `${clampToMonth(row.startDate, row.endDate)!.startDay + 1} / ${clampToMonth(row.startDate, row.endDate)!.endDay + 2}`,
              backgroundColor: row.color,
            }"
            @click="onBarClick(row)"
          >
            {{ row.percentComplete }}%
          </button>
        </template>
      </div>

      <svg class="pointer-events-none absolute left-0 top-0 h-full w-full" style="z-index: 2">
        <line
          v-for="(line, i) in svgLines"
          :key="i"
          :x1="line.x1"
          :y1="line.y1"
          :x2="line.x2"
          :y2="line.y2"
          stroke="#6b7290"
          stroke-width="1.5"
          stroke-dasharray="3,3"
          marker-end="url(#arrow)"
        />
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M0,0 L10,5 L0,10 z" fill="#6b7290" />
          </marker>
        </defs>
      </svg>
    </div>
  </div>
</template>
