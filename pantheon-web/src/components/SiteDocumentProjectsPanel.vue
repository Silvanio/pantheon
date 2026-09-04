<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useSiteDocumentProjects,
  type SiteDocumentProject,
  type SiteDocumentProjectAttachment,
} from '../composables/useSiteDocumentProjects'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { listProjects, createProject, listAttachments, uploadAttachment, getAttachmentContentBlob } =
  useSiteDocumentProjects()

const projects = ref<SiteDocumentProject[]>([])
const attachmentsByProject = ref<Record<string, SiteDocumentProjectAttachment[]>>({})
const expandedProjectId = ref<string | null>(null)
const loading = ref(false)
const showForm = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const name = ref('')

async function loadProjects() {
  loading.value = true
  try {
    projects.value = await listProjects(props.siteId)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await createProject(props.siteId, name.value)
    name.value = ''
    showForm.value = false
    await loadProjects()
  } catch {
    errorMessage.value = t('siteDocumentProjects.form.error')
  } finally {
    submitting.value = false
  }
}

async function toggleProject(projectId: string) {
  if (expandedProjectId.value === projectId) {
    expandedProjectId.value = null
    return
  }
  expandedProjectId.value = projectId
  if (!attachmentsByProject.value[projectId]) {
    attachmentsByProject.value[projectId] = await listAttachments(projectId)
  }
}

async function onUpload(projectId: string, event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    const attachment = await uploadAttachment(projectId, file)
    attachmentsByProject.value[projectId] = [...(attachmentsByProject.value[projectId] ?? []), attachment]
  } catch {
    errorMessage.value = t('siteDocumentProjects.attachments.error')
  }
}

async function onDownload(projectId: string, attachment: SiteDocumentProjectAttachment) {
  const blob = await getAttachmentContentBlob(projectId, attachment.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.target = '_blank'
  link.rel = 'noopener'
  link.click()
  URL.revokeObjectURL(url)
}

onMounted(loadProjects)
</script>

<template>
  <section class="rounded-xl border border-steel-200 bg-white p-6 shadow-sm dark:border-steel-700 dark:bg-steel-800">
    <div class="mb-4 flex items-center justify-between">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('siteDocumentProjects.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.subtitle') }}</p>
      </div>
      <button
        type="button"
        class="rounded-md bg-blueprint-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-blueprint-700 dark:bg-blueprint-500"
        @click="showForm = !showForm"
      >
        {{ t('siteDocumentProjects.newButton') }}
      </button>
    </div>

    <form v-if="showForm" class="mb-4 space-y-3 rounded-md border border-steel-200 p-4 dark:border-steel-700" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm font-medium text-steel-600 dark:text-steel-300">{{ t('siteDocumentProjects.form.name') }}</label>
        <input v-model="name" type="text" required class="w-full rounded-md border border-steel-300 bg-white px-3 py-2 text-steel-800 dark:border-steel-600 dark:bg-steel-900 dark:text-steel-50" />
      </div>
      <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>
      <div class="flex gap-2">
        <button type="submit" :disabled="submitting" class="rounded-md bg-blueprint-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60 dark:bg-blueprint-500">
          {{ t('siteDocumentProjects.form.submit') }}
        </button>
        <button type="button" class="rounded-md border border-steel-300 px-4 py-2 text-sm font-medium text-steel-600 dark:border-steel-600 dark:text-steel-300" @click="showForm = false">
          {{ t('siteDocumentProjects.form.cancel') }}
        </button>
      </div>
    </form>

    <p v-if="!loading && projects.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.empty') }}</p>
    <ul v-else class="space-y-2">
      <li v-for="project in projects" :key="project.id" class="rounded-md border border-steel-200 dark:border-steel-700">
        <button type="button" class="flex w-full items-center justify-between px-4 py-3 text-left" @click="toggleProject(project.id)">
          <span class="font-medium text-steel-800 dark:text-steel-50">{{ project.name }}</span>
          <span class="text-xs text-steel-500 dark:text-steel-400">{{ project.createdAt.slice(0, 10) }}</span>
        </button>
        <div v-if="expandedProjectId === project.id" class="space-y-2 border-t border-steel-200 p-4 dark:border-steel-700">
          <ul v-if="(attachmentsByProject[project.id]?.length ?? 0) > 0" class="space-y-1">
            <li v-for="attachment in attachmentsByProject[project.id]" :key="attachment.id">
              <button type="button" class="text-sm text-blueprint-600 hover:underline dark:text-blueprint-400" @click="onDownload(project.id, attachment)">
                {{ attachment.originalName }}
              </button>
            </li>
          </ul>
          <p v-else class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.attachments.empty') }}</p>
          <label class="inline-block text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">
            {{ t('siteDocumentProjects.attachments.uploadButton') }}
            <input type="file" accept="application/pdf" class="hidden" @change="onUpload(project.id, $event)" />
          </label>
        </div>
      </li>
    </ul>
  </section>
</template>
