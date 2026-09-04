<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useProjects, type ArchitecturalProject, type ConstructionSite } from '../composables/useProjects'

const props = defineProps<{ projectId: string; sites: ConstructionSite[] }>()

const { t } = useI18n()
const { listArchitecturalProjects, createArchitecturalProject } = useProjects()

const projects = ref<ArchitecturalProject[]>([])
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')

const name = ref('')
const description = ref('')
const constructionSiteId = ref('')

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listArchitecturalProjects(props.projectId)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createArchitecturalProject(props.projectId, {
      name: name.value,
      description: description.value || null,
      constructionSiteId: constructionSiteId.value || null,
    })
    name.value = ''
    description.value = ''
    constructionSiteId.value = ''
    showForm.value = false
    await loadProjects()
  } catch {
    errorMessage.value = t('architecturalProjects.form.error')
  } finally {
    submitting.value = false
  }
}

function siteName(siteId: string | null): string | null {
  if (!siteId) return null
  return props.sites.find((s) => s.id === siteId)?.name ?? null
}

onMounted(loadProjects)
</script>

<template>
  <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('architecturalProjects.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('architecturalProjects.subtitle') }}</p>
      </div>
      <button
        type="button"
        class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500 dark:hover:bg-blueprint-600"
        @click="showForm = !showForm"
      >
        {{ t('architecturalProjects.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-4 space-y-3 rounded-md border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('architecturalProjects.form.name') }}</label>
        <input v-model="name" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      </div>
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('architecturalProjects.form.description') }}</label>
        <input v-model="description" type="text" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 focus:border-blueprint-500 focus:outline-none focus:ring-1 focus:ring-blueprint-500 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      </div>
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('architecturalProjects.form.constructionSite') }}</label>
        <select v-model="constructionSiteId" class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50">
          <option value="">{{ t('architecturalProjects.form.constructionSiteNone') }}</option>
          <option v-for="site in props.sites" :key="site.id" :value="site.id">{{ site.name }}</option>
        </select>
      </div>
      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blueprint-700 disabled:opacity-60 dark:bg-blueprint-500 dark:hover:bg-blueprint-600">
          {{ t('architecturalProjects.form.submit') }}
        </button>
        <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 hover:bg-steel-100 dark:border-steel-600 dark:text-steel-300 dark:hover:bg-steel-700" @click="showForm = false">
          {{ t('architecturalProjects.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="!loading && projects.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('architecturalProjects.empty') }}
    </p>

    <ul v-else class="space-y-2">
      <li
        v-for="project in projects"
        :key="project.id"
        class="rounded-md border border-steel-200 px-4 py-3 dark:border-steel-700"
      >
        <p class="font-medium text-steel-800 dark:text-steel-50">{{ project.name }}</p>
        <p v-if="project.description" class="text-sm text-steel-500 dark:text-steel-400">{{ project.description }}</p>
        <p v-if="siteName(project.constructionSiteId)" class="text-xs text-blueprint-600 dark:text-blueprint-400">
          {{ siteName(project.constructionSiteId) }}
        </p>
      </li>
    </ul>
  </section>
</template>
