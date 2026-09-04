<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { useProjectOnboarding } from '../composables/useProjectOnboarding'
import type { ConstructionSite } from '../composables/useProjects'
import ThemeToggle from '../components/ThemeToggle.vue'
import EventLog from '../components/EventLog.vue'
import ConstructionSitesPanel from '../components/ConstructionSitesPanel.vue'
import ArchitecturalProjectsPanel from '../components/ArchitecturalProjectsPanel.vue'
import TeamPanel from '../components/TeamPanel.vue'

const router = useRouter()
const { logout } = useAuth()
const { status } = useProjectOnboarding()

const activeProjectId = computed(() => status.value?.activeProject?.id ?? null)
const sites = ref<ConstructionSite[]>([])

function onLogout() {
  logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-steel-50 dark:bg-steel-900">
    <header class="border-b border-steel-200 bg-white dark:border-steel-700 dark:bg-steel-800">
      <div class="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
        <div class="flex items-center gap-2">
          <div class="h-8 w-8 rounded-md bg-blueprint-600 dark:bg-blueprint-400"></div>
          <span class="text-lg font-semibold text-steel-800 dark:text-steel-50">Pantheon</span>
        </div>
        <div class="flex items-center gap-3">
          <ThemeToggle />
          <button
            type="button"
            class="rounded-md border border-steel-300 px-3 py-1.5 text-sm font-medium text-steel-600 transition hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700"
            @click="onLogout"
          >
            Sair
          </button>
        </div>
      </div>
    </header>

    <main class="mx-auto max-w-4xl space-y-6 px-6 py-8">
      <div>
        <h1 class="text-2xl font-semibold text-steel-800 dark:text-steel-50">Painel</h1>
        <p class="mt-1 text-steel-500 dark:text-steel-400">
          Fundação técnica do Pantheon: autenticação, mensageria e eventos em tempo real.
        </p>
      </div>

      <template v-if="activeProjectId">
        <ConstructionSitesPanel :project-id="activeProjectId" @updated="sites = $event" />
        <ArchitecturalProjectsPanel :project-id="activeProjectId" :sites="sites" />
        <TeamPanel :project-id="activeProjectId" />
      </template>

      <EventLog />
    </main>
  </div>
</template>
