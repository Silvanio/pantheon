<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useGlobalTasksBoard, type GlobalTaskBoard, type GlobalTaskCard } from '../composables/useGlobalTasksBoard'
import { useConstructionSites, type ConstructionSite } from '../composables/useConstructionSites'
import type { SiteMember } from '../composables/useSiteMembers'
import AppHeader from '../components/AppHeader.vue'
import AppSidebar from '../components/AppSidebar.vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const { getGlobalBoard } = useGlobalTasksBoard()
const { listSites } = useConstructionSites()

const companyId = route.params.companyId as string
const board = ref<GlobalTaskBoard>({ columns: [], cards: [], labels: [], assignees: [] })
const loading = ref(false)

const SITE_OPTIONS_LIMIT = 10
const SITE_SEARCH_MIN_CHARS = 3

const sites = ref<ConstructionSite[]>([])
const siteFilterId = ref<string | null>(null)
const siteSearchQuery = ref('')
const showSiteFilter = ref(false)

const sortedColumns = computed(() => [...board.value.columns].sort((a, b) => a.sortOrder - b.sortOrder))

const selectedSite = computed(() => sites.value.find((s) => s.id === siteFilterId.value) ?? null)

const siteOptions = computed(() => {
  const query = siteSearchQuery.value.trim().toLowerCase()
  const matches =
    query.length >= SITE_SEARCH_MIN_CHARS
      ? sites.value.filter((s) => s.name.toLowerCase().includes(query))
      : sites.value
  return matches.slice(0, SITE_OPTIONS_LIMIT)
})

function selectSiteFilter(siteId: string | null) {
  siteFilterId.value = siteId
  siteSearchQuery.value = ''
  showSiteFilter.value = false
}

function cardsForColumn(columnId: string): GlobalTaskCard[] {
  return board.value.cards
    .filter((c) => c.columnId === columnId && (!siteFilterId.value || c.constructionSiteId === siteFilterId.value))
    .sort((a, b) => a.sortOrder - b.sortOrder)
}

function labelById(id: string) {
  return board.value.labels.find((l) => l.id === id)
}

function memberById(membershipId: string): SiteMember | undefined {
  return board.value.assignees.find((m) => m.membershipId === membershipId)
}

function memberLabel(membershipId: string): string {
  const member = memberById(membershipId)
  return member?.displayName || member?.email || '?'
}

function memberInitials(membershipId: string): string {
  return memberLabel(membershipId).slice(0, 2).toUpperCase()
}

function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}

function isDueOrOverdue(isoDate: string): boolean {
  const todayIso = new Date().toISOString().slice(0, 10)
  return isoDate <= todayIso
}

async function load() {
  loading.value = true
  try {
    const [boardResult, sitesResult] = await Promise.all([getGlobalBoard(companyId), listSites(companyId)])
    board.value = boardResult
    sites.value = sitesResult
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="flex min-h-screen bg-steel-50 dark:bg-steel-900">
    <AppSidebar />
    <div class="min-w-0 flex-1">
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
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 class="text-2xl font-semibold tracking-tight text-steel-800 dark:text-steel-50">{{ t('globalTasksBoard.title') }}</h1>
          <p class="mt-1 text-steel-500 dark:text-steel-400">{{ t('globalTasksBoard.subtitle') }}</p>
        </div>

        <div class="flex items-center gap-2">
          <div class="relative">
            <button
              type="button"
              class="btn-secondary"
              :class="{ 'ring-2 ring-blueprint-500/30': siteFilterId }"
              @click="showSiteFilter = !showSiteFilter"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3 4h18M6 8h12M10 12h4" />
              </svg>
              {{ selectedSite ? selectedSite.name : t('globalTasksBoard.allSites') }}
            </button>
            <div v-if="showSiteFilter" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-3 shadow-lg" @click.stop>
              <input
                v-model="siteSearchQuery"
                type="text"
                class="field-input mb-2"
                :placeholder="t('globalTasksBoard.searchSitePlaceholder')"
              />
              <ul class="max-h-64 space-y-0.5 overflow-y-auto">
                <li>
                  <button
                    type="button"
                    class="block w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-steel-100 dark:hover:bg-steel-700"
                    :class="!siteFilterId ? 'font-semibold text-blueprint-600 dark:text-blueprint-400' : 'text-steel-700 dark:text-steel-200'"
                    @click="selectSiteFilter(null)"
                  >
                    {{ t('globalTasksBoard.allSites') }}
                  </button>
                </li>
                <li v-if="siteOptions.length === 0" class="px-2 py-1.5 text-xs text-steel-500 dark:text-steel-400">
                  {{ t('globalTasksBoard.noSitesFound') }}
                </li>
                <li v-for="site in siteOptions" :key="site.id">
                  <button
                    type="button"
                    class="block w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-steel-100 dark:hover:bg-steel-700"
                    :class="siteFilterId === site.id ? 'font-semibold text-blueprint-600 dark:text-blueprint-400' : 'text-steel-700 dark:text-steel-200'"
                    @click="selectSiteFilter(site.id)"
                  >
                    {{ site.name }}
                  </button>
                </li>
              </ul>
            </div>
          </div>
          <button v-if="siteFilterId" type="button" class="btn-ghost px-2 py-1.5 text-xs" @click="selectSiteFilter(null)">
            {{ t('globalTasksBoard.clearFilter') }}
          </button>
        </div>
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

              <div v-if="card.labelIds.length" class="mt-2 flex flex-wrap gap-1">
                <span
                  v-for="labelId in card.labelIds"
                  :key="labelId"
                  class="rounded-full px-2.5 py-1 text-xs font-medium text-white"
                  :style="{ backgroundColor: labelById(labelId)?.colorHex }"
                >
                  {{ labelById(labelId)?.name }}
                </span>
              </div>

              <p
                v-if="card.dueDate"
                class="mt-2 text-xs font-medium"
                :class="isDueOrOverdue(card.dueDate) ? 'text-safety-600 dark:text-safety-500' : 'text-steel-500 dark:text-steel-400'"
              >
                {{ t('tasks.dueDateIcon') }} {{ formatDate(card.dueDate) }}
              </p>

              <div v-if="card.assigneeIds.length" class="mt-2 flex -space-x-1.5">
                <span
                  v-for="memberId in card.assigneeIds.slice(0, 3)"
                  :key="memberId"
                  class="group relative flex h-5 w-5 items-center justify-center rounded-full border border-white bg-blueprint-500 text-[9px] font-semibold text-white dark:border-steel-900"
                >
                  {{ memberInitials(memberId) }}
                  <span
                    class="pointer-events-none absolute bottom-full left-1/2 z-20 mb-1.5 -translate-x-1/2 whitespace-nowrap rounded-md bg-ink-900 px-2 py-1 text-[11px] font-medium text-white opacity-0 group-hover:opacity-100 dark:bg-ink-950"
                  >
                    {{ memberLabel(memberId) }}
                  </span>
                </span>
                <span
                  v-if="card.assigneeIds.length > 3"
                  class="flex h-5 w-5 items-center justify-center rounded-full border border-white bg-steel-400 text-[9px] font-semibold text-white dark:border-steel-900"
                >
                  +{{ card.assigneeIds.length - 3 }}
                </span>
              </div>

              <div v-if="card.attachmentCount > 0 || card.commentCount > 0" class="mt-2 flex items-center gap-2 text-[10px] text-steel-400 dark:text-steel-500">
                <span
                  v-if="card.attachmentCount > 0"
                  class="flex items-center gap-1"
                  :title="t('tasks.attachmentsIndicator', { count: card.attachmentCount })"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3 w-3">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M18.375 12.739l-7.693 7.693a4.5 4.5 0 01-6.364-6.364l10.94-10.94a3 3 0 114.243 4.243L8.552 18.32a1.5 1.5 0 01-2.122-2.122l7.492-7.492"
                    />
                  </svg>
                </span>
                <span v-if="card.commentCount > 0" class="flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3 w-3">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z"
                    />
                  </svg>
                  {{ card.commentCount }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    </div>
  </div>
</template>
