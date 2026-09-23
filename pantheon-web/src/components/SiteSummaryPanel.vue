<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useSiteSummary, type RecentItem, type SiteSummary } from '../composables/useSiteSummary'

const props = defineProps<{ siteId: string }>()
const emit = defineEmits<{ (e: 'open-tab', tab: 'purchaseRequests' | 'orcamentos' | 'tasks' | 'projects'): void }>()

const { t } = useI18n()
const router = useRouter()
const { getSummary } = useSiteSummary()

const summary = ref<SiteSummary | null>(null)
const loading = ref(false)
const loadError = ref('')

const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000

function isNew(item: RecentItem): boolean {
  return Date.now() - new Date(item.createdAt).getTime() <= SEVEN_DAYS_MS
}

const dateFormatter = new Intl.DateTimeFormat('pt-BR')
function formatDate(value: string): string {
  return dateFormatter.format(new Date(`${value}T00:00:00`))
}

// Combines purchase-requests-awaiting-approval + draft-orçamentos — both already computed by the
// backend (see design.md's "Pendências computed client-side from fields already in the response").
const pendingCount = computed(() => {
  if (!summary.value) return 0
  const awaitingApproval = summary.value.purchaseRequests ? summary.value.purchaseRequests.awaitingApproval : 0
  const draftOrcamentos = summary.value.orcamentos ? summary.value.orcamentos.draft : 0
  return awaitingApproval + draftOrcamentos
})

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    summary.value = await getSummary(props.siteId)
  } catch {
    loadError.value = t('siteSummary.loadError')
  } finally {
    loading.value = false
  }
}

function openPurchaseRequest(id: string) {
  router.push(`/purchase-requests/${id}`)
}

function openOrcamento(id: string) {
  router.push(`/orcamentos/${id}`)
}

onMounted(load)
</script>

<template>
  <section class="space-y-5">
    <p v-if="loadError" class="text-sm text-safety-600 dark:text-safety-500">{{ loadError }}</p>

    <template v-if="summary">
      <div
        v-if="summary.schedulePercentComplete !== null"
        class="rounded-2xl border border-steel-200 bg-white p-5 dark:border-steel-700 dark:bg-steel-800/60"
      >
        <div class="flex items-center justify-between">
          <span class="text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('siteSummary.schedule.title') }}</span>
          <span class="text-lg font-extrabold text-emerald-600">{{ summary.schedulePercentComplete }}%</span>
        </div>
        <div class="mt-2 h-2 overflow-hidden rounded-full bg-steel-100 dark:bg-steel-700">
          <div class="h-full rounded-full bg-emerald-500" :style="{ width: summary.schedulePercentComplete + '%' }"></div>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3.5 lg:grid-cols-4">
        <div v-if="summary.dailyReports" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('siteSummary.dailyReports.title') }}</p>
          <p class="text-2xl font-extrabold text-blueprint-600">{{ summary.dailyReports.total }}</p>
          <p class="mt-1 text-xs text-steel-500 dark:text-steel-400">
            {{ summary.dailyReports.lastReportDate ? t('siteSummary.dailyReports.last', { date: formatDate(summary.dailyReports.lastReportDate) }) : t('siteSummary.dailyReports.empty') }}
          </p>
        </div>

        <div v-if="summary.equipment" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('siteSummary.equipment.title') }}</p>
          <p class="text-2xl font-extrabold text-blueprint-600">{{ summary.equipment.total }}</p>
          <p v-if="summary.equipment.unavailable > 0" class="mt-1 text-xs font-medium text-safety-600 dark:text-safety-500">
            {{ t('siteSummary.equipment.unavailable', { count: summary.equipment.unavailable }) }}
          </p>
        </div>

        <div v-if="summary.teamMembersCount !== null" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-steel-500 dark:text-steel-400">{{ t('siteSummary.team.title') }}</p>
          <p class="text-2xl font-extrabold text-blueprint-600">{{ summary.teamMembersCount }}</p>
        </div>

        <div v-if="summary.orcamentos && summary.purchaseRequests" class="rounded-2xl border border-amber-200 bg-amber-50 p-4 dark:border-amber-900/40 dark:bg-amber-900/20">
          <p class="mb-1.5 text-[11px] font-bold uppercase tracking-wide text-amber-700 dark:text-amber-400">{{ t('siteSummary.pending.title') }}</p>
          <p class="text-2xl font-extrabold text-amber-700 dark:text-amber-400">{{ pendingCount }}</p>
          <p class="mt-1 text-xs text-amber-700/80 dark:text-amber-400/80">{{ t('siteSummary.pending.hint') }}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-3.5 sm:grid-cols-2">
        <div v-if="summary.purchaseRequests" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-bold text-steel-800 dark:text-steel-50">{{ t('siteSummary.purchaseRequests.title') }}</p>
            <button type="button" class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="emit('open-tab', 'purchaseRequests')">
              {{ t('siteSummary.viewAll', { count: summary.purchaseRequests.total }) }}
            </button>
          </div>
          <p v-if="summary.purchaseRequests.recent.length === 0" class="text-xs text-steel-500 dark:text-steel-400">{{ t('siteSummary.empty') }}</p>
          <ul v-else class="space-y-1.5">
            <li
              v-for="item in summary.purchaseRequests.recent"
              :key="item.id"
              class="flex cursor-pointer items-center justify-between rounded-lg px-2 py-1.5 text-xs transition hover:bg-steel-50 dark:hover:bg-steel-800"
              @click="openPurchaseRequest(item.id)"
            >
              <span class="truncate text-steel-700 dark:text-steel-200">{{ item.title }}</span>
              <span v-if="isNew(item)" class="ml-2 shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400">
                {{ t('siteSummary.new') }}
              </span>
            </li>
          </ul>
        </div>

        <div v-if="summary.orcamentos" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-bold text-steel-800 dark:text-steel-50">{{ t('siteSummary.orcamentos.title') }}</p>
            <button type="button" class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="emit('open-tab', 'orcamentos')">
              {{ t('siteSummary.viewAll', { count: summary.orcamentos.total }) }}
            </button>
          </div>
          <p v-if="summary.orcamentos.recent.length === 0" class="text-xs text-steel-500 dark:text-steel-400">{{ t('siteSummary.empty') }}</p>
          <ul v-else class="space-y-1.5">
            <li
              v-for="item in summary.orcamentos.recent"
              :key="item.id"
              class="flex cursor-pointer items-center justify-between rounded-lg px-2 py-1.5 text-xs transition hover:bg-steel-50 dark:hover:bg-steel-800"
              @click="openOrcamento(item.id)"
            >
              <span class="truncate text-steel-700 dark:text-steel-200">{{ item.title }}</span>
              <span v-if="isNew(item)" class="ml-2 shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400">
                {{ t('siteSummary.new') }}
              </span>
            </li>
          </ul>
        </div>

        <div v-if="summary.tasks" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-bold text-steel-800 dark:text-steel-50">{{ t('siteSummary.tasks.title') }}</p>
            <button type="button" class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="emit('open-tab', 'tasks')">
              {{ t('siteSummary.viewAll', { count: summary.tasks.total }) }}
            </button>
          </div>
          <p v-if="summary.tasks.recent.length === 0" class="text-xs text-steel-500 dark:text-steel-400">{{ t('siteSummary.empty') }}</p>
          <ul v-else class="space-y-1.5">
            <li
              v-for="item in summary.tasks.recent"
              :key="item.id"
              class="flex cursor-pointer items-center justify-between rounded-lg px-2 py-1.5 text-xs transition hover:bg-steel-50 dark:hover:bg-steel-800"
              @click="emit('open-tab', 'tasks')"
            >
              <span class="truncate text-steel-700 dark:text-steel-200">{{ item.title }}</span>
              <span v-if="isNew(item)" class="ml-2 shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400">
                {{ t('siteSummary.new') }}
              </span>
            </li>
          </ul>
        </div>

        <div v-if="summary.projects" class="rounded-2xl border border-steel-200 bg-white p-4 dark:border-steel-700 dark:bg-steel-800/60">
          <div class="mb-2 flex items-center justify-between">
            <p class="text-sm font-bold text-steel-800 dark:text-steel-50">{{ t('siteSummary.projects.title') }}</p>
            <button type="button" class="text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400" @click="emit('open-tab', 'projects')">
              {{ t('siteSummary.viewAll', { count: summary.projects.total }) }}
            </button>
          </div>
          <p v-if="summary.projects.recent.length === 0" class="text-xs text-steel-500 dark:text-steel-400">{{ t('siteSummary.empty') }}</p>
          <ul v-else class="space-y-1.5">
            <li
              v-for="item in summary.projects.recent"
              :key="item.id"
              class="flex cursor-pointer items-center justify-between rounded-lg px-2 py-1.5 text-xs transition hover:bg-steel-50 dark:hover:bg-steel-800"
              @click="emit('open-tab', 'projects')"
            >
              <span class="truncate text-steel-700 dark:text-steel-200">{{ item.title }}</span>
              <span v-if="isNew(item)" class="ml-2 shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-400">
                {{ t('siteSummary.new') }}
              </span>
            </li>
          </ul>
        </div>
      </div>
    </template>
  </section>
</template>
