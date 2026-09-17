<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useSiteDocumentProjects,
  type SiteDocumentBreadcrumb,
  type SiteDocumentFile,
  type SiteDocumentFolder,
} from '../composables/useSiteDocumentProjects'
import { useTaskCards, type TaskCard } from '../composables/useTaskCards'

const props = defineProps<{ siteId: string; canManage: boolean }>()

const { t } = useI18n()
const {
  listContents,
  getBreadcrumbs,
  createFolder,
  renameFolder,
  setFolderTaskLink,
  clearFolderTaskLink,
  deleteFolder,
  uploadFile,
  deleteFile,
  getFileContentBlob,
} = useSiteDocumentProjects()
const { getBoard } = useTaskCards()

const currentFolderId = ref<string | null>(null)
const breadcrumbs = ref<SiteDocumentBreadcrumb[]>([])
const folders = ref<SiteDocumentFolder[]>([])
const files = ref<SiteDocumentFile[]>([])
const loading = ref(false)
const errorMessage = ref('')
const filterText = ref('')

const showNewFolderForm = ref(false)
const newFolderName = ref('')
const creatingFolder = ref(false)

const renamingFolderId = ref<string | null>(null)
const renameValue = ref('')

const confirmingDeleteFolderId = ref<string | null>(null)
const confirmingDeleteFileId = ref<string | null>(null)

const taskLinkFolderId = ref<string | null>(null)
const taskBoardCards = ref<TaskCard[]>([])
const loadingTaskBoard = ref(false)

const uploading = ref(false)

const previewFile = ref<SiteDocumentFile | null>(null)
const previewUrl = ref('')
const previewLoading = ref(false)
const previewError = ref('')
const previewIsPdf = computed(() => previewFile.value !== null && fileIconKind(previewFile.value) === 'pdf')

const filteredFolders = computed(() => {
  const term = filterText.value.trim().toLowerCase()
  const list = term ? folders.value.filter((folder) => folder.name.toLowerCase().includes(term)) : folders.value
  return [...list].sort((a, b) => a.name.localeCompare(b.name))
})

const filteredFiles = computed(() => {
  const term = filterText.value.trim().toLowerCase()
  const list = term ? files.value.filter((file) => file.originalName.toLowerCase().includes(term)) : files.value
  return [...list].sort((a, b) => a.originalName.localeCompare(b.originalName))
})

const isEmpty = computed(() => !loading.value && filteredFolders.value.length === 0 && filteredFiles.value.length === 0)

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [contents, crumbs] = await Promise.all([
      listContents(props.siteId, currentFolderId.value),
      currentFolderId.value ? getBreadcrumbs(currentFolderId.value) : Promise.resolve([]),
    ])
    folders.value = contents.folders
    files.value = contents.files
    breadcrumbs.value = crumbs
  } catch {
    errorMessage.value = t('siteDocumentProjects.error')
  } finally {
    loading.value = false
  }
}

function navigateTo(folderId: string | null) {
  currentFolderId.value = folderId
  filterText.value = ''
  renamingFolderId.value = null
  confirmingDeleteFolderId.value = null
  confirmingDeleteFileId.value = null
  taskLinkFolderId.value = null
  load()
}

async function onCreateFolder() {
  if (!newFolderName.value.trim()) return
  creatingFolder.value = true
  errorMessage.value = ''
  try {
    await createFolder(props.siteId, newFolderName.value.trim(), currentFolderId.value)
    newFolderName.value = ''
    showNewFolderForm.value = false
    await load()
  } catch {
    errorMessage.value = t('siteDocumentProjects.form.error')
  } finally {
    creatingFolder.value = false
  }
}

function startRename(folder: SiteDocumentFolder) {
  renamingFolderId.value = folder.id
  renameValue.value = folder.name
}

async function onConfirmRename(folder: SiteDocumentFolder) {
  if (!renameValue.value.trim()) return
  errorMessage.value = ''
  try {
    await renameFolder(folder.id, renameValue.value.trim())
    renamingFolderId.value = null
    await load()
  } catch {
    errorMessage.value = t('siteDocumentProjects.renameError')
  }
}

async function onDeleteFolder(folder: SiteDocumentFolder) {
  errorMessage.value = ''
  try {
    await deleteFolder(folder.id)
    confirmingDeleteFolderId.value = null
    await load()
  } catch {
    errorMessage.value = t('siteDocumentProjects.deleteError')
    confirmingDeleteFolderId.value = null
  }
}

async function onDeleteFile(file: SiteDocumentFile) {
  errorMessage.value = ''
  try {
    await deleteFile(file.id)
    confirmingDeleteFileId.value = null
    await load()
  } catch {
    errorMessage.value = t('siteDocumentProjects.deleteError')
    confirmingDeleteFileId.value = null
  }
}

async function onDownloadFile(file: SiteDocumentFile) {
  errorMessage.value = ''
  try {
    const { blob, filename } = await getFileContentBlob(file.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename ?? file.originalName
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    errorMessage.value = t('siteDocumentProjects.downloadError')
  }
}

async function onPreviewFile(file: SiteDocumentFile) {
  if (fileIconKind(file) === 'file') {
    await onDownloadFile(file)
    return
  }
  errorMessage.value = ''
  previewFile.value = file
  previewLoading.value = true
  previewError.value = ''
  try {
    const { blob } = await getFileContentBlob(file.id)
    previewUrl.value = URL.createObjectURL(blob)
  } catch {
    previewError.value = t('siteDocumentProjects.previewError')
  } finally {
    previewLoading.value = false
  }
}

function closePreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewFile.value = null
  previewUrl.value = ''
  previewError.value = ''
}

async function onUploadFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const selected = input.files
  if (!selected || selected.length === 0) return
  uploading.value = true
  errorMessage.value = ''
  try {
    for (const file of Array.from(selected)) {
      await uploadFile(props.siteId, currentFolderId.value, file)
    }
    await load()
  } catch {
    errorMessage.value = t('siteDocumentProjects.uploadError')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function openTaskLinkEditor(folder: SiteDocumentFolder) {
  taskLinkFolderId.value = taskLinkFolderId.value === folder.id ? null : folder.id
  if (taskLinkFolderId.value && taskBoardCards.value.length === 0) {
    loadingTaskBoard.value = true
    try {
      const board = await getBoard(props.siteId)
      taskBoardCards.value = board.cards
    } finally {
      loadingTaskBoard.value = false
    }
  }
}

async function onSelectTaskLink(folder: SiteDocumentFolder, event: Event) {
  const value = (event.target as HTMLSelectElement).value
  errorMessage.value = ''
  try {
    if (value) {
      await setFolderTaskLink(folder.id, value)
    } else {
      await clearFolderTaskLink(folder.id)
    }
    taskLinkFolderId.value = null
    await load()
  } catch {
    errorMessage.value = t('siteDocumentProjects.taskLinkError')
  }
}

function fileIconKind(file: SiteDocumentFile): 'pdf' | 'image' | 'video' | 'file' {
  if (file.contentType === 'application/pdf') return 'pdf'
  if (file.contentType.startsWith('image/')) return 'image'
  if (file.contentType.startsWith('video/')) return 'video'
  // Content-Type can be missing/generic for a legitimately allowed extension; the extension
  // itself is authoritative since the backend only ever accepts jpg/jpeg/png/pdf/mp4.
  const extension = file.originalName.split('.').pop()?.toLowerCase()
  if (extension === 'pdf') return 'pdf'
  if (extension === 'jpg' || extension === 'jpeg' || extension === 'png') return 'image'
  if (extension === 'mp4') return 'video'
  return 'file'
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString('pt-BR')
}

onMounted(load)
onBeforeUnmount(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<template>
  <section class="card card-pad">
    <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('siteDocumentProjects.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.subtitle') }}</p>
      </div>
      <div v-if="canManage" class="flex flex-wrap items-center gap-2">
        <button type="button" class="btn-secondary" @click="showNewFolderForm = !showNewFolderForm">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="mr-1.5 inline h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 13h6m-3-3v6M4 7a2 2 0 012-2h3.5l2 2H18a2 2 0 012 2v8a2 2 0 01-2 2H6a2 2 0 01-2-2V7z" />
          </svg>
          {{ t('siteDocumentProjects.newFolderButton') }}
        </button>
        <label class="btn-primary cursor-pointer">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="mr-1.5 inline h-4 w-4">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 0L7 9m5-5l5 5M5 20h14" />
          </svg>
          {{ uploading ? t('siteDocumentProjects.uploading') : t('siteDocumentProjects.uploadButton') }}
          <input
            type="file"
            multiple
            accept=".jpg,.jpeg,.png,.pdf,.mp4"
            class="hidden"
            :disabled="uploading"
            @change="onUploadFiles"
          />
        </label>
      </div>
    </div>

    <div class="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-steel-200 pb-3 dark:border-steel-700">
      <nav class="flex flex-wrap items-center gap-1 text-sm">
        <button
          type="button"
          class="rounded-md px-2 py-1 font-medium transition"
          :class="currentFolderId === null ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-500/10 dark:text-blueprint-300' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
          @click="navigateTo(null)"
        >
          {{ t('siteDocumentProjects.root') }}
        </button>
        <template v-for="crumb in breadcrumbs" :key="crumb.id">
          <span class="text-steel-400 dark:text-steel-600">/</span>
          <button
            type="button"
            class="rounded-md px-2 py-1 font-medium transition"
            :class="crumb.id === currentFolderId ? 'bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-500/10 dark:text-blueprint-300' : 'text-steel-600 hover:bg-steel-100 dark:text-steel-300 dark:hover:bg-steel-800'"
            @click="navigateTo(crumb.id)"
          >
            {{ crumb.name }}
          </button>
        </template>
      </nav>
      <input
        v-model="filterText"
        type="text"
        :placeholder="t('siteDocumentProjects.filterPlaceholder')"
        class="field-input w-full max-w-[220px] text-sm"
      />
    </div>

    <form v-if="showNewFolderForm" class="mb-4 flex flex-wrap gap-2 rounded-lg border border-steel-200 p-3 dark:border-steel-700" @submit.prevent="onCreateFolder">
      <input v-model="newFolderName" type="text" required :placeholder="t('siteDocumentProjects.form.name')" class="field-input flex-1 text-sm" />
      <button type="submit" :disabled="creatingFolder" class="btn-primary py-1.5 text-sm">{{ t('siteDocumentProjects.form.submit') }}</button>
      <button type="button" class="btn-secondary py-1.5 text-sm" @click="showNewFolderForm = false">{{ t('siteDocumentProjects.form.cancel') }}</button>
    </form>

    <p v-if="errorMessage" class="mb-3 text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <div v-if="loading" class="py-10 text-center text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.loading') }}</div>

    <div v-else-if="isEmpty" class="flex flex-col items-center gap-2 py-14 text-center">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="h-12 w-12 text-steel-300 dark:text-steel-600">
        <path stroke-linecap="round" stroke-linejoin="round" d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
      </svg>
      <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.empty') }}</p>
    </div>

    <ul v-else class="divide-y divide-steel-100 dark:divide-steel-800">
      <li v-for="folder in filteredFolders" :key="folder.id" class="group flex items-center gap-3 py-2.5">
        <button type="button" class="flex flex-1 items-center gap-3 text-left" @click="navigateTo(folder.id)">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="h-6 w-6 shrink-0 text-blueprint-500">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
          </svg>
          <span class="min-w-0 flex-1">
            <span v-if="renamingFolderId !== folder.id" class="block truncate font-medium text-steel-800 dark:text-steel-50">
              {{ folder.name }}
            </span>
            <span v-if="folder.linkedTaskTitle" class="mt-0.5 flex items-center gap-1 text-xs text-blueprint-600 dark:text-blueprint-400">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3 w-3">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.828 10.172a4 4 0 010 5.656l-3 3a4 4 0 11-5.656-5.656l1.5-1.5M10.172 13.828a4 4 0 010-5.656l3-3a4 4 0 115.656 5.656l-1.5 1.5" />
              </svg>
              {{ folder.linkedTaskTitle }}
            </span>
          </span>
        </button>

        <input
          v-if="renamingFolderId === folder.id"
          v-model="renameValue"
          type="text"
          class="field-input mr-2 max-w-[200px] text-sm"
          @keyup.enter="onConfirmRename(folder)"
          @keyup.esc="renamingFolderId = null"
        />

        <span class="hidden shrink-0 text-xs text-steel-500 sm:block dark:text-steel-400">{{ formatDate(folder.updatedAt) }}</span>
        <span class="hidden shrink-0 text-xs text-steel-500 md:block dark:text-steel-400">{{ folder.updatedByName }}</span>

        <div v-if="canManage" class="relative flex shrink-0 items-center gap-1 opacity-0 transition group-hover:opacity-100">
          <template v-if="renamingFolderId === folder.id">
            <button type="button" class="btn-secondary px-2 py-1 text-xs" @click="onConfirmRename(folder)">{{ t('siteDocumentProjects.save') }}</button>
            <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="renamingFolderId = null">{{ t('siteDocumentProjects.form.cancel') }}</button>
          </template>
          <template v-else>
            <button type="button" class="btn-ghost p-1.5" :title="t('siteDocumentProjects.linkTask')" @click="openTaskLinkEditor(folder)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M13.828 10.172a4 4 0 010 5.656l-3 3a4 4 0 11-5.656-5.656l1.5-1.5M10.172 13.828a4 4 0 010-5.656l3-3a4 4 0 115.656 5.656l-1.5 1.5" />
              </svg>
            </button>
            <button type="button" class="btn-ghost p-1.5" :title="t('siteDocumentProjects.rename')" @click="startRename(folder)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.5-9.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
            </button>
            <button type="button" class="btn-ghost p-1.5 text-safety-600" :title="t('siteDocumentProjects.delete')" @click="confirmingDeleteFolderId = folder.id">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 7h12M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m2 0v13a1 1 0 01-1 1H8a1 1 0 01-1-1V7h10z" />
              </svg>
            </button>
          </template>

          <div v-if="taskLinkFolderId === folder.id" class="modal-panel absolute right-0 top-full z-10 mt-2 w-64 p-3 shadow-lg" @click.stop>
            <p class="mb-2 text-xs font-medium text-steel-600 dark:text-steel-300">{{ t('siteDocumentProjects.linkTaskLabel') }}</p>
            <p v-if="loadingTaskBoard" class="text-xs text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.loading') }}</p>
            <select v-else class="field-input w-full text-sm" :value="folder.taskCardId ?? ''" @change="onSelectTaskLink(folder, $event)">
              <option value="">{{ t('siteDocumentProjects.noTaskLink') }}</option>
              <option v-for="card in taskBoardCards" :key="card.id" :value="card.id">{{ card.title }}</option>
            </select>
          </div>

          <div v-if="confirmingDeleteFolderId === folder.id" class="modal-panel absolute right-0 top-full z-10 mt-2 w-72 p-3 shadow-lg" @click.stop>
            <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('siteDocumentProjects.deleteFolderConfirm') }}</p>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDeleteFolderId = null">{{ t('siteDocumentProjects.form.cancel') }}</button>
              <button type="button" class="btn-danger py-1 text-xs" @click="onDeleteFolder(folder)">{{ t('siteDocumentProjects.delete') }}</button>
            </div>
          </div>
        </div>
      </li>

      <li v-for="file in filteredFiles" :key="file.id" class="group flex items-center gap-3 py-2.5">
        <button type="button" class="flex flex-1 items-center gap-3 text-left" @click="onPreviewFile(file)">
          <svg
            v-if="fileIconKind(file) === 'pdf'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
            class="h-6 w-6 shrink-0 text-safety-500"
          >
            <path stroke-linecap="round" stroke-linejoin="round" d="M7 3h7l5 5v11a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M14 3v5h5" />
          </svg>
          <svg
            v-else-if="fileIconKind(file) === 'image'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
            class="h-6 w-6 shrink-0 text-emerald-500"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 15l-5-5L5 21" />
          </svg>
          <svg
            v-else-if="fileIconKind(file) === 'video'"
            xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
            class="h-6 w-6 shrink-0 text-blueprint-500"
          >
            <rect x="3" y="5" width="14" height="14" rx="2" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M17 9l4-2v10l-4-2" />
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="h-6 w-6 shrink-0 text-steel-400">
            <path stroke-linecap="round" stroke-linejoin="round" d="M7 3h7l5 5v11a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M14 3v5h5" />
          </svg>
          <span class="min-w-0 flex-1 truncate font-medium text-steel-800 dark:text-steel-50">{{ file.originalName }}</span>
        </button>

        <span class="hidden shrink-0 text-xs text-steel-500 sm:block dark:text-steel-400">{{ formatDate(file.createdAt) }}</span>
        <span class="hidden shrink-0 text-xs text-steel-500 md:block dark:text-steel-400">{{ file.uploadedByName }}</span>

        <div class="relative flex shrink-0 items-center gap-1 opacity-0 transition group-hover:opacity-100">
          <button type="button" class="btn-ghost p-1.5" :title="t('siteDocumentProjects.download')" @click="onDownloadFile(file)">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 12l-4-4m4 4l4-4M5 20h14" />
            </svg>
          </button>
          <button v-if="canManage" type="button" class="btn-ghost p-1.5 text-safety-600" :title="t('siteDocumentProjects.delete')" @click="confirmingDeleteFileId = file.id">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 7h12M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m2 0v13a1 1 0 01-1 1H8a1 1 0 01-1-1V7h10z" />
            </svg>
          </button>

          <div v-if="confirmingDeleteFileId === file.id" class="modal-panel absolute right-0 top-full z-10 mt-2 w-64 p-3 shadow-lg" @click.stop>
            <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('siteDocumentProjects.deleteFileConfirm') }}</p>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDeleteFileId = null">{{ t('siteDocumentProjects.form.cancel') }}</button>
              <button type="button" class="btn-danger py-1 text-xs" @click="onDeleteFile(file)">{{ t('siteDocumentProjects.delete') }}</button>
            </div>
          </div>
        </div>
      </li>
    </ul>

    <div v-if="previewFile" class="fixed inset-0 z-30 flex items-center justify-center bg-black/60 p-4" @click.self="closePreview">
      <div
        class="modal-panel flex flex-col overflow-hidden"
        :class="previewIsPdf ? 'h-[85vh] w-[85vw] max-w-none' : 'max-h-[90vh] w-full max-w-3xl'"
      >
        <div class="flex shrink-0 items-center justify-between gap-3 border-b border-steel-200 px-4 py-3 dark:border-steel-700">
          <p class="min-w-0 flex-1 truncate text-sm font-medium text-steel-800 dark:text-steel-50">{{ previewFile.originalName }}</p>
          <div class="flex shrink-0 items-center gap-1">
            <button type="button" class="btn-ghost p-1.5" :title="t('siteDocumentProjects.download')" @click="onDownloadFile(previewFile)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 12l-4-4m4 4l4-4M5 20h14" />
              </svg>
            </button>
            <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="closePreview">{{ t('siteDocumentProjects.close') }}</button>
          </div>
        </div>
        <div class="flex min-h-0 flex-1 items-center justify-center" :class="previewIsPdf ? '' : 'overflow-auto p-4'">
          <p v-if="previewLoading" class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.loading') }}</p>
          <p v-else-if="previewError" class="text-sm text-safety-600 dark:text-safety-500">{{ previewError }}</p>
          <img
            v-else-if="fileIconKind(previewFile) === 'image'"
            :src="previewUrl"
            :alt="previewFile.originalName"
            class="max-h-[75vh] max-w-full object-contain"
          />
          <video v-else-if="fileIconKind(previewFile) === 'video'" :src="previewUrl" controls autoplay class="max-h-[75vh] max-w-full" />
          <iframe v-else-if="fileIconKind(previewFile) === 'pdf'" :src="previewUrl" :title="previewFile.originalName" class="h-full w-full" />
        </div>
      </div>
    </div>
  </section>
</template>
