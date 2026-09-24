<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [string] }>()

const { t } = useI18n()

const CENTER = 100
const RADIUS = 78
const HOUR_MARKS = Array.from({ length: 12 }, (_, i) => (i === 0 ? 12 : i))
const MINUTE_MARKS = Array.from({ length: 12 }, (_, i) => i * 5)

const open = ref(false)
const mode = ref<'hour' | 'minute'>('hour')
const hour12 = ref(8)
const minute = ref(0)
const period = ref<'AM' | 'PM'>('AM')

function parseValue(value: string) {
  if (!value) {
    hour12.value = 8
    minute.value = 0
    period.value = 'AM'
    return
  }
  const [h, m] = value.split(':').map(Number)
  period.value = h >= 12 ? 'PM' : 'AM'
  hour12.value = h % 12 === 0 ? 12 : h % 12
  minute.value = m
}

watch(() => props.modelValue, parseValue, { immediate: true })

function commit() {
  let hour24 = hour12.value % 12
  if (period.value === 'PM') hour24 += 12
  emit('update:modelValue', `${String(hour24).padStart(2, '0')}:${String(minute.value).padStart(2, '0')}`)
}

function toggleOpen() {
  if (!open.value) {
    parseValue(props.modelValue)
    mode.value = 'hour'
  }
  open.value = !open.value
}

function selectHour(h: number) {
  hour12.value = h
  mode.value = 'minute'
  commit()
}

function selectMinute(m: number) {
  minute.value = m
  commit()
}

function setPeriod(p: 'AM' | 'PM') {
  period.value = p
  commit()
}

function positionFor(index: number) {
  const angle = ((index * 30 - 90) * Math.PI) / 180
  return { x: CENTER + RADIUS * Math.cos(angle), y: CENTER + RADIUS * Math.sin(angle) }
}

const handEnd = computed(() => positionFor(mode.value === 'hour' ? hour12.value % 12 : minute.value / 5))

function onFaceClick(event: MouseEvent) {
  const svg = event.currentTarget as SVGSVGElement
  const rect = svg.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 200
  const y = ((event.clientY - rect.top) / rect.height) * 200
  let angle = (Math.atan2(y - CENTER, x - CENTER) * 180) / Math.PI + 90
  if (angle < 0) angle += 360
  const index = Math.round(angle / 30) % 12
  if (mode.value === 'hour') {
    selectHour(index === 0 ? 12 : index)
  } else {
    selectMinute(index * 5)
  }
}
</script>

<template>
  <div class="relative">
    <button type="button" class="field-input flex items-center justify-between text-left" @click="toggleOpen">
      <span :class="modelValue ? 'text-steel-800 dark:text-steel-50' : 'text-steel-400 dark:text-steel-500'">
        {{ modelValue || t('timeClockPicker.placeholder') }}
      </span>
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4 shrink-0 text-steel-400">
        <circle cx="12" cy="12" r="9" />
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 7v5l3 3" />
      </svg>
    </button>

    <div v-if="open" class="modal-panel absolute left-0 top-full z-20 mt-2 w-64 p-4 shadow-lg" @click.stop>
      <div class="mb-3 flex items-center justify-center gap-1">
        <button
          type="button"
          class="rounded-lg px-2 py-1 text-2xl font-semibold transition"
          :class="mode === 'hour' ? 'bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400' : 'text-steel-800 dark:text-steel-50'"
          @click="mode = 'hour'"
        >
          {{ String(hour12).padStart(2, '0') }}
        </button>
        <span class="text-2xl font-semibold text-steel-400">:</span>
        <button
          type="button"
          class="rounded-lg px-2 py-1 text-2xl font-semibold transition"
          :class="mode === 'minute' ? 'bg-blueprint-50 text-blueprint-600 dark:bg-blueprint-500/10 dark:text-blueprint-400' : 'text-steel-800 dark:text-steel-50'"
          @click="mode = 'minute'"
        >
          {{ String(minute).padStart(2, '0') }}
        </button>
        <div class="ml-2 flex flex-col overflow-hidden rounded-lg border border-steel-200 text-xs font-bold dark:border-steel-700">
          <button
            type="button"
            class="px-2 py-0.5 transition"
            :class="period === 'AM' ? 'bg-blueprint-600 text-white' : 'text-steel-500 hover:bg-steel-50 dark:text-steel-400 dark:hover:bg-steel-800'"
            @click="setPeriod('AM')"
          >
            AM
          </button>
          <button
            type="button"
            class="px-2 py-0.5 transition"
            :class="period === 'PM' ? 'bg-blueprint-600 text-white' : 'text-steel-500 hover:bg-steel-50 dark:text-steel-400 dark:hover:bg-steel-800'"
            @click="setPeriod('PM')"
          >
            PM
          </button>
        </div>
      </div>

      <svg viewBox="0 0 200 200" class="mx-auto block h-48 w-48 cursor-pointer select-none" @click="onFaceClick">
        <circle cx="100" cy="100" r="96" class="fill-steel-100 dark:fill-steel-800" />
        <line :x1="100" :y1="100" :x2="handEnd.x" :y2="handEnd.y" class="stroke-blueprint-500" stroke-width="2" />
        <circle cx="100" cy="100" r="4" class="fill-blueprint-500" />
        <circle :cx="handEnd.x" :cy="handEnd.y" r="14" class="fill-blueprint-500" />
        <template v-if="mode === 'hour'">
          <text
            v-for="h in HOUR_MARKS"
            :key="h"
            :x="positionFor(h % 12).x"
            :y="positionFor(h % 12).y"
            text-anchor="middle"
            dominant-baseline="central"
            class="pointer-events-none text-sm font-semibold"
            :class="hour12 === h ? 'fill-white' : 'fill-steel-700 dark:fill-steel-200'"
          >
            {{ h }}
          </text>
        </template>
        <template v-else>
          <text
            v-for="m in MINUTE_MARKS"
            :key="m"
            :x="positionFor(m / 5).x"
            :y="positionFor(m / 5).y"
            text-anchor="middle"
            dominant-baseline="central"
            class="pointer-events-none text-sm font-semibold"
            :class="minute === m ? 'fill-white' : 'fill-steel-700 dark:fill-steel-200'"
          >
            {{ String(m).padStart(2, '0') }}
          </text>
        </template>
      </svg>

      <div class="mt-3 flex justify-end">
        <button type="button" class="btn-primary py-1 text-xs" @click="open = false">{{ t('timeClockPicker.confirm') }}</button>
      </div>
    </div>
  </div>
</template>
