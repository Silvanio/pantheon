<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useGlobalTasksBoard, type GlobalTaskBoard, type GlobalTaskCard } from '../composables/useGlobalTasksBoard'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getGlobalBoard } = useGlobalTasksBoard()

const companyId = route.params.companyId as string
const board = ref<GlobalTaskBoard>({ columns: [], cards: [] })
const loading = ref(false)

const sortedColumns = computed(() => [...board.value.columns].sort((a, b) => a.sortOrder - b.sortOrder))

function cardsForColumn(columnId: string): GlobalTaskCard[] {
  return board.value.cards.filter((c) => c.columnId === columnId).sort((a, b) => a.sortOrder - b.sortOrder)
}

async function load() {
  loading.value = true
  try {
    board.value = await getGlobalBoard(companyId)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppHeader>
      <template #left>
        <button type="button" class="btn-ghost -ml-2" @click="router.push('/')">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15 18l-6-6 6-6" />
          </svg>
          {{ t('globalTasksBoard.back') }}
        </button>
      </template>
    </AppHeader>

    <main class="app-container space-y-6 py-8">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('globalTasksBoard.title') }}</h1>
        <p class="mt-1 text-steel-500 dark:text-steel-400">{{ t('globalTasksBoard.subtitle') }}</p>
      </div>

      <p v-if="!loading && board.columns.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
        {{ t('globalTasksBoard.noColumns') }}
      </p>

      <div v-else class="flex gap-4 overflow-x-auto pb-2">
        <div v-for="column in sortedColumns" :key="column.id" class="w-72 shrink-0 rounded-xl bg-steel-100 p-3 dark:bg-steel-800/60">
          <h3 class="mb-3 px-1 text-sm font-semibold text-steel-700 dark:text-steel-200">{{ column.name }}</h3>

          <div class="space-y-2">
            <div v-for="card in cardsForColumn(column.id)" :key="card.id" class="rounded-lg bg-white p-3 shadow-sm dark:bg-steel-900">
              <span
                class="mb-1.5 inline-block rounded-full px-2 py-0.5 text-[11px] font-medium text-white"
                :style="{ backgroundColor: card.siteColorHex }"
              >
                {{ card.siteName }}
              </span>
              <p class="text-sm font-medium text-steel-800 dark:text-steel-50">{{ card.title }}</p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>
