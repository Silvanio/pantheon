<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useCompanyOnboarding } from '../composables/useCompanyOnboarding'
import { useConstructionSites, type ConstructionSite } from '../composables/useConstructionSites'
import { useMySites } from '../composables/useMySites'
import { useSiteMembers, type SiteMember } from '../composables/useSiteMembers'
import { usePurchaseRequests } from '../composables/usePurchaseRequests'
import { useOrcamentos } from '../composables/useOrcamentos'
import EventLog from '../components/EventLog.vue'
import CompanyLogo from '../components/CompanyLogo.vue'
import SiteCompanyBadge from '../components/SiteCompanyBadge.vue'
import SitePhoto from '../components/SitePhoto.vue'
import AppSidebar from '../components/AppSidebar.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { vDatePicker } from '../lib/datePicker'
import BrandMark from '../components/BrandMark.vue'

const router = useRouter()
const { t } = useI18n()
const { status, activeCompany } = useCompanyOnboarding()
const { listSites, createSite } = useConstructionSites()
const { listMine } = useMySites()
const { listMembers } = useSiteMembers()
const { listPurchaseRequests } = usePurchaseRequests()
const { listOrcamentos } = useOrcamentos()

const companyId = computed(() => (status.value ? activeCompany(status.value)?.companyId ?? null : null))
const companyName = computed(() => (status.value ? activeCompany(status.value)?.companyName ?? null : null))
const isCompanyAdmin = computed(() => (status.value ? activeCompany(status.value)?.role === 'ADMIN' : false))
const sites = ref<(ConstructionSite & { companyName?: string | null })[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const search = ref('')

const name = ref('')
const address = ref('')
const startDate = ref('')
const expectedEndDate = ref('')

const filteredSites = computed(() =>
  search.value.trim() ? sites.value.filter((s) => s.name.toLowerCase().includes(search.value.trim().toLowerCase())) : sites.value,
)

const activeSitesCount = computed(() => sites.value.filter((s) => s.status === 'PLANNING' || s.status === 'IN_PROGRESS').length)
const pendingPurchaseRequests = ref<number | null>(null)
const pendingOrcamentos = ref<number | null>(null)

const summaryText = computed(() => {
  const parts = [t('dashboard.summary.activeSites', { count: activeSitesCount.value })]
  if ((pendingPurchaseRequests.value ?? 0) > 0) {
    parts.push(t('dashboard.summary.pendingPurchaseRequests', { count: pendingPurchaseRequests.value }))
  }
  if ((pendingOrcamentos.value ?? 0) > 0) {
    parts.push(t('dashboard.summary.pendingOrcamentos', { count: pendingOrcamentos.value }))
  }
  return parts.join(' · ')
})

// Per-site team members, for the avatar badge on each card — best-effort: a viewer without
// TEAM_MANAGE access to a given site simply shows no avatars for it, rather than failing the page.
const membersBySite = ref<Record<string, SiteMember[]>>({})

// Per-site pending purchase-request/orçamento counts, for the badges at the bottom of each card.
const statsBySite = ref<Record<string, { pendingPr: number; pendingOrc: number }>>({})

// Status-derived approximation used only until an obra has a real Cronograma (see the
// construction-schedule capability) — once it has schedule tasks, `schedulePercentComplete`
// (computed from them) takes over and this placeholder is never consulted for that obra again.
const PLACEHOLDER_PROGRESS: Record<ConstructionSite['status'], number> = {
  PLANNING: 10,
  IN_PROGRESS: 55,
  PAUSED: 40,
  COMPLETED: 100,
}

function progressFor(site: ConstructionSite): number {
  return site.schedulePercentComplete ?? PLACEHOLDER_PROGRESS[site.status]
}

// A site-only member (no CompanyMembership at all) has no single company to scope the board
// to — their obras are gathered across every company they have SiteMembership access to.
async function loadSites() {
  loading.value = true
  try {
    sites.value = companyId.value ? await listSites(companyId.value) : await listMine()
    loadMembers()
    loadStats()
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  const entries = await Promise.all(
    sites.value.map(async (site) => {
      try {
        return [site.id, await listMembers(site.id)] as const
      } catch {
        return [site.id, []] as const
      }
    }),
  )
  membersBySite.value = Object.fromEntries(entries)
}

async function loadStats() {
  pendingPurchaseRequests.value = null
  pendingOrcamentos.value = null
  const entries = await Promise.all(
    sites.value.map(async (site) => {
      const [pendingPr, pendingOrc] = await Promise.all([
        listPurchaseRequests(site.id, { size: 100 })
          .then((page) => page.content.filter((pr) => pr.status !== 'CONCLUIDO').length)
          .catch(() => 0),
        listOrcamentos(site.id, { size: 100 })
          .then((page) => page.content.filter((o) => o.status === 'DRAFT').length)
          .catch(() => 0),
      ])
      return [site.id, { pendingPr, pendingOrc }] as const
    }),
  )
  statsBySite.value = Object.fromEntries(entries)
  pendingPurchaseRequests.value = entries.reduce((sum, [, s]) => sum + s.pendingPr, 0)
  pendingOrcamentos.value = entries.reduce((sum, [, s]) => sum + s.pendingOrc, 0)
}

function membersFor(siteId: string): SiteMember[] {
  return membersBySite.value[siteId] ?? []
}

function statsFor(siteId: string): { pendingPr: number; pendingOrc: number } {
  return statsBySite.value[siteId] ?? { pendingPr: 0, pendingOrc: 0 }
}

function memberInitials(member: SiteMember): string {
  const label = member.displayName || member.email || '?'
  return label.trim().charAt(0).toUpperCase()
}

const AVATAR_COLORS = ['#2F53F0', '#0E9F6E', '#D97C0A', '#E14F4F', '#8B5CF6']
function avatarColor(index: number): string {
  return AVATAR_COLORS[index % AVATAR_COLORS.length]
}

async function onSubmit() {
  if (!companyId.value) return
  errorMessage.value = ''
  submitting.value = true
  try {
    await createSite(companyId.value, {
      name: name.value,
      address: address.value,
      startDate: startDate.value,
      expectedEndDate: expectedEndDate.value || null,
    })
    name.value = ''
    address.value = ''
    startDate.value = ''
    expectedEndDate.value = ''
    showForm.value = false
    await loadSites()
  } catch {
    errorMessage.value = t('dashboard.form.error')
  } finally {
    submitting.value = false
  }
}

function openSite(site: ConstructionSite) {
  router.push(`/sites/${site.id}`)
}

const showEventLog = ref(false)

onMounted(loadSites)
watch(companyId, loadSites)
</script>

<template>
  <div class="flex min-h-screen bg-steel-50">
    <AppSidebar />

    <div class="min-w-0 flex-1 px-8 py-9 sm:px-12 lg:px-14">
      <div class="flex flex-wrap items-center justify-between gap-4">
        <div v-if="companyId" class="flex items-center gap-2.5">
          <CompanyLogo :company-id="companyId" />
          <span class="truncate text-[17px] font-extrabold tracking-tight text-steel-900">{{ companyName }}</span>
        </div>
        <BrandMark v-else />

        <div class="flex items-center gap-3">
          <div class="flex items-center gap-2 rounded-[10px] border-[1.5px] border-steel-200 bg-white px-3.5 py-[9px]">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#6B7290" stroke-width="2" class="h-[15px] w-[15px] shrink-0">
              <circle cx="11" cy="11" r="7" />
              <path stroke-linecap="round" d="M21 21l-4.3-4.3" />
            </svg>
            <input
              v-model="search"
              type="text"
              :placeholder="t('dashboard.searchPlaceholder')"
              class="w-44 bg-transparent text-[13px] text-steel-800 placeholder:text-steel-400 focus:outline-none"
            />
          </div>
          <router-link v-if="isCompanyAdmin && companyId" :to="`/companies/${companyId}/tasks-board`" class="btn-secondary py-2.5 text-[13px]">
            {{ t('dashboard.globalTasksBoardButton') }}
          </router-link>
          <button v-if="companyId" type="button" class="btn-primary py-2.5 text-[13px]" @click="showForm = !showForm">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="h-3.5 w-3.5">
              <path stroke-linecap="round" d="M12 5v14M5 12h14" />
            </svg>
            {{ t('dashboard.newSiteButton') }}
          </button>
        </div>
      </div>

      <h1 class="mb-1 mt-7 text-[25px] font-extrabold tracking-tight text-steel-900">{{ t('dashboard.title') }}</h1>
      <p class="text-[13.5px] text-steel-500">{{ summaryText }}</p>

      <form v-if="companyId && showForm" class="card card-pad mt-6 space-y-4" @submit.prevent="onSubmit">
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label class="field-label">{{ t('constructionSites.form.name') }}</label>
            <input v-model="name" type="text" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('constructionSites.form.address') }}</label>
            <input v-model="address" type="text" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('constructionSites.form.startDate') }}</label>
            <input v-model="startDate" v-date-picker type="date" required class="field-input" />
          </div>
          <div>
            <label class="field-label">{{ t('constructionSites.form.expectedEndDate') }}</label>
            <input v-model="expectedEndDate" v-date-picker type="date" class="field-input" />
          </div>
        </div>
        <p v-if="errorMessage" class="text-sm text-safety-600">{{ errorMessage }}</p>
        <div class="flex gap-2">
          <button type="submit" :disabled="submitting" class="btn-primary">
            {{ t('constructionSites.form.submit') }}
          </button>
          <button type="button" class="btn-secondary" @click="showForm = false">
            {{ t('constructionSites.form.cancel') }}
          </button>
        </div>
      </form>

      <div v-if="!loading && sites.length === 0" class="card card-pad mt-6 flex flex-col items-center gap-3 py-16 text-center">
        <div class="flex h-14 w-14 items-center justify-center rounded-full bg-blueprint-50 text-blueprint-500">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="h-7 w-7">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6" />
          </svg>
        </div>
        <p class="text-steel-500">{{ t('constructionSites.empty') }}</p>
      </div>

      <div v-else class="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <button
          v-for="site in filteredSites"
          :key="site.id"
          type="button"
          class="flex flex-col rounded-2xl border border-steel-200 bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
          @click="openSite(site)"
        >
          <div class="relative h-28 w-full shrink-0 overflow-hidden rounded-t-2xl bg-gradient-to-br from-blueprint-600 to-ink-900">
            <SitePhoto :site-id="site.id" :has-photo="!!site.photoObjectKey" />
            <div
              v-if="site.photoObjectKey"
              class="pointer-events-none absolute inset-0 bg-gradient-to-t from-black/45 via-transparent to-transparent"
            ></div>
            <SiteCompanyBadge
              v-if="site.companyName"
              :site-id="site.id"
              :company-name="site.companyName"
              class="absolute left-2 top-2 max-w-[calc(100%-1rem)]"
            />
          </div>
          <div class="p-4">
            <p class="truncate text-[15px] font-extrabold text-steel-900">{{ site.name }}</p>
            <p class="mt-1 flex items-center gap-1.5 text-xs text-steel-500">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5 shrink-0">
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <path stroke-linecap="round" d="M16 2v4M8 2v4M3 10h18" />
              </svg>
              {{ site.startDate }}
            </p>

            <div class="mt-3.5 flex items-center justify-between">
              <span class="text-[11px] font-bold text-steel-500">{{ t('dashboard.card.progress') }}</span>
              <span class="text-xs font-extrabold text-emerald-600">{{ progressFor(site) }}%</span>
            </div>
            <div class="mt-1.5 h-1.5 overflow-hidden rounded-full bg-steel-100">
              <div class="h-full rounded-full bg-emerald-500" :style="{ width: progressFor(site) + '%' }"></div>
            </div>

            <div v-if="statsFor(site.id).pendingPr > 0 || statsFor(site.id).pendingOrc > 0" class="mt-3.5 flex flex-wrap items-center gap-1.5">
              <span
                v-if="statsFor(site.id).pendingPr > 0"
                class="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-1 text-[11px] font-bold text-amber-700"
                :title="t('dashboard.card.pendingPurchaseRequests', { count: statsFor(site.id).pendingPr })"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3 w-3 shrink-0">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6M9 16h6M9 8h1M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                </svg>
                {{ t('dashboard.card.pendingPurchaseRequests', { count: statsFor(site.id).pendingPr }) }}
              </span>
              <span
                v-if="statsFor(site.id).pendingOrc > 0"
                class="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-1 text-[11px] font-bold text-amber-700"
                :title="t('dashboard.card.pendingOrcamentos', { count: statsFor(site.id).pendingOrc })"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3 w-3 shrink-0">
                  <circle cx="12" cy="12" r="9" />
                  <path stroke-linecap="round" d="M12 7v10M15 9.5c0-1.1-1.34-2-3-2s-3 .9-3 2 1.34 2 3 2 3 .9 3 2-1.34 2-3 2-3-.9-3-2" />
                </svg>
                {{ t('dashboard.card.pendingOrcamentos', { count: statsFor(site.id).pendingOrc }) }}
              </span>
            </div>

            <div class="mt-3.5 flex items-center justify-between">
              <div v-if="membersFor(site.id).length" class="flex">
                <span
                  v-for="(member, index) in membersFor(site.id).slice(0, 3)"
                  :key="member.membershipId"
                  class="flex h-6 w-6 items-center justify-center rounded-full border-2 border-white text-[10px] font-bold text-white"
                  :style="{ backgroundColor: avatarColor(index), marginLeft: index > 0 ? '-8px' : '0' }"
                  :title="member.displayName || member.email || ''"
                >
                  {{ memberInitials(member) }}
                </span>
                <span
                  v-if="membersFor(site.id).length > 3"
                  class="flex h-6 w-6 items-center justify-center rounded-full border-2 border-white bg-steel-400 text-[10px] font-bold text-white"
                  style="margin-left: -8px"
                >
                  +{{ membersFor(site.id).length - 3 }}
                </span>
              </div>
              <span v-else></span>
              <StatusBadge kind="constructionSite" :status="site.status" />
            </div>
          </div>
        </button>

        <button
          type="button"
          class="flex min-h-[214px] flex-col items-center justify-center gap-2.5 rounded-2xl border-2 border-dashed border-steel-300 text-steel-500 transition hover:border-blueprint-400 hover:text-blueprint-600"
          :disabled="!companyId"
          @click="showForm = true"
        >
          <div class="flex h-10 w-10 items-center justify-center rounded-full bg-blueprint-50">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="#2F53F0" stroke-width="2.3" class="h-[19px] w-[19px]">
              <path stroke-linecap="round" d="M12 5v14M5 12h14" />
            </svg>
          </div>
          <span class="text-[13.5px] font-bold">{{ t('dashboard.addSiteCard') }}</span>
        </button>
      </div>

      <div class="pt-6">
        <button type="button" class="text-xs font-medium text-steel-400 hover:text-steel-600" @click="showEventLog = !showEventLog">
          {{ showEventLog ? '— Ocultar eventos em tempo real' : '+ Eventos em tempo real' }}
        </button>
        <div v-if="showEventLog" class="mt-3">
          <EventLog />
        </div>
      </div>
    </div>
  </div>
</template>
