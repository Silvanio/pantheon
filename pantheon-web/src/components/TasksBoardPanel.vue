<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  useTaskCards,
  type TaskBoard,
  type TaskCard,
  type TaskCardAttachment,
  type TaskComment,
  type TaskLabel,
} from '../composables/useTaskCards'
import { useTaskLabels } from '../composables/useTaskLabels'
import { useSiteMembers, type SiteMember } from '../composables/useSiteMembers'
import { useSse } from '../composables/useSse'
import { useSiteDocumentProjects, type SiteDocumentBreadcrumb, type SiteDocumentFolder } from '../composables/useSiteDocumentProjects'
import { vDatePicker } from '../lib/datePicker'

const props = defineProps<{ siteId: string; canManageTasks: boolean; canViewProjects: boolean }>()

const { t } = useI18n()
const {
  getBoard,
  createCard,
  moveCard,
  updateDueDate,
  deleteCard,
  createCustomLabel,
  attachLabel,
  detachLabel,
  assignMember,
  unassignMember,
  listComments,
  addComment,
  listCardAttachments,
  attachFileToCard,
  getCardAttachmentContentBlob,
} = useTaskCards()
const { listCompanyLabels } = useTaskLabels()
const { listMembers } = useSiteMembers()
const { listContents: listProjectContents, getBreadcrumbs: getProjectBreadcrumbs } = useSiteDocumentProjects()

const LABEL_COLORS = ['#EF4444', '#F97316', '#F59E0B', '#22C55E', '#06B6D4', '#3B82F6', '#8B5CF6', '#EC4899']

const board = ref<TaskBoard>({ columns: [], cards: [], labels: [] })
const predefinedLabels = ref<TaskLabel[]>([])
const siteMembers = ref<SiteMember[]>([])
const loading = ref(false)
const errorMessage = ref('')

const newCardOpenFor = ref<string | null>(null)
const newCardTitle = ref('')
const newCardDueDate = ref('')
const creatingCard = ref(false)

const draggedCardId = ref<string | null>(null)

const selectedCard = ref<TaskCard | null>(null)
const comments = ref<TaskComment[]>([])
const newCommentBody = ref('')
const postingComment = ref(false)
const newLabelName = ref('')
const newLabelColor = ref(LABEL_COLORS[0])
const confirmingDelete = ref(false)

const cardAttachments = ref<TaskCardAttachment[]>([])
const attachmentsError = ref('')
const pendingAttachFile = ref<File | null>(null)
const showFolderPicker = ref(false)
const pickerFolderId = ref<string | null>(null)
const pickerBreadcrumbs = ref<SiteDocumentBreadcrumb[]>([])
const pickerFolders = ref<SiteDocumentFolder[]>([])
const pickerLoading = ref(false)
const attaching = ref(false)

const previewAttachment = ref<TaskCardAttachment | null>(null)
const previewUrl = ref('')
const previewLoading = ref(false)
const previewError = ref('')
const previewIsPdf = computed(
  () => previewAttachment.value !== null && attachmentIconKind(previewAttachment.value) === 'pdf',
)

const sortedColumns = computed(() => [...board.value.columns].sort((a, b) => a.sortOrder - b.sortOrder))
const companyId = computed(() => board.value.columns[0]?.companyId ?? null)

interface TaskCardMovedEvent {
  cardId: string
  constructionSiteId: string
  columnId: string
  sortOrder: number
}

interface TaskCardCreatedEvent {
  cardId: string
  constructionSiteId: string
  columnId: string
  title: string
  description: string | null
  dueDate: string | null
  sortOrder: number
  createdBy: string
  createdAt: string
}

interface TaskCardDeletedEvent {
  cardId: string
  constructionSiteId: string
}

interface TaskCardUpdatedEvent {
  cardId: string
  constructionSiteId: string
  columnId: string
  title: string
  description: string | null
  dueDate: string | null
  sortOrder: number
  labelIds: string[]
  labels: TaskLabel[]
  assigneeIds: string[]
  commentCount: number
  attachmentCount: number
  updatedAt: string
}

function handleTaskCardMoved(event: TaskCardMovedEvent) {
  if (event.constructionSiteId !== props.siteId) return
  const card = board.value.cards.find((c) => c.id === event.cardId)
  if (!card) return
  card.columnId = event.columnId
  card.sortOrder = event.sortOrder
}

function handleTaskCardCreated(event: TaskCardCreatedEvent) {
  if (event.constructionSiteId !== props.siteId) return
  if (board.value.cards.some((c) => c.id === event.cardId)) return
  board.value.cards.push({
    id: event.cardId,
    constructionSiteId: event.constructionSiteId,
    columnId: event.columnId,
    title: event.title,
    description: event.description,
    dueDate: event.dueDate,
    sortOrder: event.sortOrder,
    labelIds: [],
    assigneeIds: [],
    commentCount: 0,
    attachmentCount: 0,
    createdBy: event.createdBy,
    createdAt: event.createdAt,
    updatedAt: event.createdAt,
  })
}

function handleTaskCardDeleted(event: TaskCardDeletedEvent) {
  if (event.constructionSiteId !== props.siteId) return
  board.value.cards = board.value.cards.filter((c) => c.id !== event.cardId)
  if (selectedCard.value?.id === event.cardId) {
    closeCard()
  }
}

function handleTaskCardUpdated(event: TaskCardUpdatedEvent) {
  if (event.constructionSiteId !== props.siteId) return
  const card = board.value.cards.find((c) => c.id === event.cardId)
  if (card) {
    card.columnId = event.columnId
    card.title = event.title
    card.description = event.description
    card.dueDate = event.dueDate
    card.sortOrder = event.sortOrder
    card.labelIds = event.labelIds
    card.assigneeIds = event.assigneeIds
    card.commentCount = event.commentCount
    card.attachmentCount = event.attachmentCount
    card.updatedAt = event.updatedAt
  }

  // A label a recipient hasn't loaded yet (a card-only custom label, or a predefined one their
  // role can't independently fetch) rides along in the event so it renders immediately, not
  // just its id.
  for (const label of event.labels) {
    const existing = board.value.labels.find((l) => l.id === label.id)
    if (existing) {
      existing.name = label.name
      existing.colorHex = label.colorHex
    } else {
      board.value.labels.push(label)
    }
  }
}

function handleTaskEvent(eventName: string, data: unknown) {
  if (eventName === 'task-card-moved') {
    handleTaskCardMoved(data as TaskCardMovedEvent)
  } else if (eventName === 'task-card-created') {
    handleTaskCardCreated(data as TaskCardCreatedEvent)
  } else if (eventName === 'task-card-deleted') {
    handleTaskCardDeleted(data as TaskCardDeletedEvent)
  } else if (eventName === 'task-card-updated') {
    handleTaskCardUpdated(data as TaskCardUpdatedEvent)
  }
}

const { connect: connectTaskEvents } = useSse(
  ['task-card-moved', 'task-card-created', 'task-card-deleted', 'task-card-updated'],
  { onEvent: handleTaskEvent },
)

const customLabelsForSelectedCard = computed(() => {
  if (!selectedCard.value) return []
  return selectedCard.value.labelIds
    .map((id) => labelById(id))
    .filter((label): label is TaskLabel => !!label && !!label.cardId)
})

function cardsForColumn(columnId: string): TaskCard[] {
  return board.value.cards.filter((c) => c.columnId === columnId).sort((a, b) => a.sortOrder - b.sortOrder)
}

function labelById(id: string): TaskLabel | undefined {
  return board.value.labels.find((l) => l.id === id) ?? predefinedLabels.value.find((l) => l.id === id)
}

function memberById(membershipId: string): SiteMember | undefined {
  return siteMembers.value.find((m) => m.membershipId === membershipId)
}

function memberLabel(membershipId: string): string {
  const member = memberById(membershipId)
  return member?.displayName || member?.email || '?'
}

function memberInitials(membershipId: string): string {
  const label = memberLabel(membershipId)
  return label.slice(0, 2).toUpperCase()
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
  errorMessage.value = ''
  try {
    const [boardResult, membersResult] = await Promise.all([getBoard(props.siteId), listMembers(props.siteId)])
    board.value = boardResult
    siteMembers.value = membersResult
    if (companyId.value) {
      // A site-only member (e.g. an external engineer/service provider with no CompanyMembership)
      // can access this board but isn't authorized to read the company's predefined label catalog.
      // That's an expected 403 for them, not a board-load failure, so it's isolated here rather
      // than left to bubble up and falsely mark the caller's action (move, create, ...) as failed.
      try {
        predefinedLabels.value = await listCompanyLabels(companyId.value)
      } catch {
        predefinedLabels.value = []
      }
    }
  } finally {
    loading.value = false
  }
}

function openNewCardForm(columnId: string) {
  newCardOpenFor.value = columnId
  newCardTitle.value = ''
  newCardDueDate.value = ''
}

async function onCreateCard(columnId: string) {
  if (!newCardTitle.value.trim()) return
  creatingCard.value = true
  errorMessage.value = ''
  try {
    await createCard(props.siteId, columnId, newCardTitle.value.trim(), null, newCardDueDate.value || null)
    newCardOpenFor.value = null
    newCardTitle.value = ''
    newCardDueDate.value = ''
    await load()
  } catch {
    errorMessage.value = t('tasks.error')
  } finally {
    creatingCard.value = false
  }
}

function onDragStart(card: TaskCard) {
  draggedCardId.value = card.id
}

async function onDrop(columnId: string) {
  if (!draggedCardId.value) return
  const cardId = draggedCardId.value
  draggedCardId.value = null
  const card = board.value.cards.find((c) => c.id === cardId)
  if (!card || card.columnId === columnId) return
  errorMessage.value = ''
  try {
    const endOfColumn = cardsForColumn(columnId).length
    await moveCard(cardId, columnId, endOfColumn)
    await load()
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function openCard(card: TaskCard) {
  selectedCard.value = card
  newCommentBody.value = ''
  attachmentsError.value = ''
  comments.value = await listComments(card.id)
  cardAttachments.value = await listCardAttachments(card.id)
}

function closeCard() {
  selectedCard.value = null
  comments.value = []
  newCommentBody.value = ''
  confirmingDelete.value = false
  cardAttachments.value = []
  attachmentsError.value = ''
  showFolderPicker.value = false
  pendingAttachFile.value = null
  closeAttachmentPreview()
}

async function refreshSelectedCard() {
  await load()
  selectedCard.value = selectedCard.value
    ? (board.value.cards.find((c) => c.id === selectedCard.value?.id) ?? null)
    : null
}

async function onChangeDueDate(event: Event) {
  if (!selectedCard.value) return
  const card = selectedCard.value
  const value = (event.target as HTMLInputElement).value
  errorMessage.value = ''
  try {
    await updateDueDate(card.id, value || null)
    await refreshSelectedCard()
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function onTogglePredefinedLabel(label: TaskLabel) {
  if (!selectedCard.value) return
  const card = selectedCard.value
  errorMessage.value = ''
  try {
    if (card.labelIds.includes(label.id)) {
      await detachLabel(card.id, label.id)
    } else {
      await attachLabel(card.id, label.id)
    }
    await refreshSelectedCard()
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function onRemoveCustomLabel(label: TaskLabel) {
  if (!selectedCard.value) return
  errorMessage.value = ''
  try {
    await detachLabel(selectedCard.value.id, label.id)
    await refreshSelectedCard()
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function onCreateCustomLabel() {
  if (!selectedCard.value || !newLabelName.value.trim()) return
  errorMessage.value = ''
  try {
    await createCustomLabel(selectedCard.value.id, newLabelName.value.trim(), newLabelColor.value)
    newLabelName.value = ''
    await refreshSelectedCard()
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function onToggleAssignee(member: SiteMember) {
  if (!selectedCard.value) return
  const card = selectedCard.value
  errorMessage.value = ''
  try {
    if (card.assigneeIds.includes(member.membershipId)) {
      await unassignMember(card.id, member.membershipId)
    } else {
      await assignMember(card.id, member.membershipId)
    }
    await refreshSelectedCard()
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function onAddComment() {
  if (!selectedCard.value || !newCommentBody.value.trim()) return
  postingComment.value = true
  errorMessage.value = ''
  try {
    await addComment(selectedCard.value.id, newCommentBody.value.trim())
    newCommentBody.value = ''
    comments.value = await listComments(selectedCard.value.id)
    await load()
  } catch {
    errorMessage.value = t('tasks.error')
  } finally {
    postingComment.value = false
  }
}

function onChooseAttachFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  pendingAttachFile.value = file
  showFolderPicker.value = true
  pickerFolderId.value = null
  loadPickerFolder()
}

async function loadPickerFolder() {
  pickerLoading.value = true
  try {
    const [contents, crumbs] = await Promise.all([
      listProjectContents(props.siteId, pickerFolderId.value),
      pickerFolderId.value ? getProjectBreadcrumbs(pickerFolderId.value) : Promise.resolve([]),
    ])
    pickerFolders.value = contents.folders
    pickerBreadcrumbs.value = crumbs
  } finally {
    pickerLoading.value = false
  }
}

function pickerNavigateTo(folderId: string | null) {
  pickerFolderId.value = folderId
  loadPickerFolder()
}

async function onConfirmAttach() {
  if (!selectedCard.value || !pendingAttachFile.value) return
  attaching.value = true
  attachmentsError.value = ''
  try {
    await attachFileToCard(selectedCard.value.id, pickerFolderId.value, pendingAttachFile.value)
    cardAttachments.value = await listCardAttachments(selectedCard.value.id)
    showFolderPicker.value = false
    pendingAttachFile.value = null
    await refreshSelectedCard()
  } catch {
    attachmentsError.value = t('tasks.attachments.error')
  } finally {
    attaching.value = false
  }
}

function onCancelAttach() {
  showFolderPicker.value = false
  pendingAttachFile.value = null
}

async function onDownloadCardAttachment(attachment: TaskCardAttachment) {
  attachmentsError.value = ''
  try {
    const { blob, filename } = await getCardAttachmentContentBlob(attachment.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename ?? attachment.originalName
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    attachmentsError.value = t('tasks.attachments.downloadError')
  }
}

function attachmentIconKind(attachment: TaskCardAttachment): 'pdf' | 'image' | 'video' | 'file' {
  if (attachment.contentType === 'application/pdf') return 'pdf'
  if (attachment.contentType.startsWith('image/')) return 'image'
  if (attachment.contentType.startsWith('video/')) return 'video'
  const extension = attachment.originalName.split('.').pop()?.toLowerCase()
  if (extension === 'pdf') return 'pdf'
  if (extension === 'jpg' || extension === 'jpeg' || extension === 'png') return 'image'
  if (extension === 'mp4') return 'video'
  return 'file'
}

async function onPreviewCardAttachment(attachment: TaskCardAttachment) {
  if (attachmentIconKind(attachment) === 'file') {
    await onDownloadCardAttachment(attachment)
    return
  }
  attachmentsError.value = ''
  previewAttachment.value = attachment
  previewLoading.value = true
  previewError.value = ''
  try {
    const { blob } = await getCardAttachmentContentBlob(attachment.id)
    previewUrl.value = URL.createObjectURL(blob)
  } catch {
    previewError.value = t('siteDocumentProjects.previewError')
  } finally {
    previewLoading.value = false
  }
}

function closeAttachmentPreview() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewAttachment.value = null
  previewUrl.value = ''
  previewError.value = ''
}

async function onConfirmDeleteCard() {
  if (!selectedCard.value) return
  errorMessage.value = ''
  try {
    await deleteCard(selectedCard.value.id)
    closeCard()
    await load()
  } catch {
    errorMessage.value = t('tasks.error')
    confirmingDelete.value = false
  }
}

onMounted(() => {
  load()
  connectTaskEvents()
})
onBeforeUnmount(() => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
})
</script>

<template>
  <section class="space-y-4">
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ t('tasks.title') }}</h2>
        <p class="text-sm text-steel-500 dark:text-steel-400">{{ t('tasks.subtitle') }}</p>
      </div>
    </div>

    <p v-if="errorMessage" class="text-sm text-safety-600 dark:text-safety-500">{{ errorMessage }}</p>

    <p v-if="!loading && board.columns.length === 0" class="text-sm text-steel-500 dark:text-steel-400">
      {{ t('tasks.noColumns') }}
    </p>

    <div v-else class="flex gap-4 overflow-x-auto pb-2">
      <div
        v-for="(column, columnIndex) in sortedColumns"
        :key="column.id"
        class="w-72 shrink-0 rounded-xl bg-steel-100 p-3 dark:bg-steel-800/60"
        @dragover.prevent
        @drop="onDrop(column.id)"
      >
        <h3 class="mb-3 flex items-center gap-2 px-1 text-sm font-semibold text-steel-700 dark:text-steel-200">
          <span
            class="h-2 w-2 shrink-0 rounded-full"
            :class="['bg-steel-400', 'bg-blueprint-500', 'bg-amber-500', 'bg-emerald-500', 'bg-safety-500'][columnIndex % 5]"
          ></span>
          {{ column.name }}
          <span class="ml-auto text-xs font-semibold text-steel-400 dark:text-steel-500">{{ cardsForColumn(column.id).length }}</span>
        </h3>

        <div class="space-y-2">
          <div
            v-for="card in cardsForColumn(column.id)"
            :key="card.id"
            draggable="true"
            class="relative w-full cursor-pointer rounded-xl border border-steel-200 bg-white p-3 pr-14 pb-5 text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-steel-700 dark:bg-steel-900"
            @dragstart="onDragStart(card)"
            @click="openCard(card)"
          >
            <span
              v-if="card.dueDate"
              class="absolute right-2 top-1.5 text-[10px] font-medium"
              :class="isDueOrOverdue(card.dueDate) ? 'text-safety-600 dark:text-safety-500' : 'text-steel-400 dark:text-steel-500'"
            >
              {{ formatDate(card.dueDate) }}
            </span>

            <p class="text-sm font-medium text-steel-800 dark:text-steel-50">{{ card.title }}</p>

            <div v-if="card.labelIds.length" class="mt-1.5 flex flex-wrap gap-1">
              <span
                v-for="labelId in card.labelIds"
                :key="labelId"
                class="rounded-full px-1.5 py-0.5 text-[9px] font-medium text-white"
                :style="{ backgroundColor: labelById(labelId)?.colorHex }"
              >
                {{ labelById(labelId)?.name }}
              </span>
            </div>

            <div v-if="card.assigneeIds.length" class="mt-2 flex -space-x-1.5">
              <span
                v-for="memberId in card.assigneeIds.slice(0, 3)"
                :key="memberId"
                class="flex h-5 w-5 items-center justify-center rounded-full border border-white bg-blueprint-500 text-[9px] font-semibold text-white dark:border-steel-900"
                :title="memberLabel(memberId)"
              >
                {{ memberInitials(memberId) }}
              </span>
              <span
                v-if="card.assigneeIds.length > 3"
                class="flex h-5 w-5 items-center justify-center rounded-full border border-white bg-steel-400 text-[9px] font-semibold text-white dark:border-steel-900"
              >
                +{{ card.assigneeIds.length - 3 }}
              </span>
            </div>

            <div class="absolute bottom-1.5 right-2 flex items-center gap-2 text-[10px] text-steel-400 dark:text-steel-500">
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

        <form v-if="newCardOpenFor === column.id" class="mt-2 space-y-2" @submit.prevent="onCreateCard(column.id)">
          <input
            v-model="newCardTitle"
            type="text"
            :placeholder="t('tasks.newCardPlaceholder')"
            class="field-input w-full text-sm"
            autofocus
          />
          <input
            v-model="newCardDueDate"
            v-date-picker
            type="date"
            :aria-label="t('tasks.dueDate')"
            class="field-input w-full text-sm"
          />
          <div class="flex gap-2">
            <button type="submit" :disabled="creatingCard" class="btn-primary py-1 text-xs">{{ t('tasks.addCard') }}</button>
            <button type="button" class="btn-secondary py-1 text-xs" @click="newCardOpenFor = null">{{ t('tasks.cancel') }}</button>
          </div>
        </form>
        <button v-else type="button" class="mt-2 w-full rounded-lg px-2 py-1.5 text-left text-sm text-steel-500 hover:bg-steel-200/60 dark:text-steel-400 dark:hover:bg-steel-700/60" @click="openNewCardForm(column.id)">
          + {{ t('tasks.addCard') }}
        </button>
      </div>
    </div>

    <div v-if="selectedCard" class="fixed inset-0 z-20 flex items-center justify-center bg-black/40 p-4" @click.self="closeCard">
      <div class="modal-panel card-pad max-h-[85vh] w-full max-w-lg overflow-y-auto">
        <div class="mb-4 flex items-start justify-between gap-3">
          <h3 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ selectedCard.title }}</h3>
          <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="closeCard">{{ t('tasks.close') }}</button>
        </div>

        <div class="mb-4">
          <label class="field-label mb-1 block">{{ t('tasks.dueDate') }}</label>
          <input
            type="date"
            v-date-picker
            class="field-input text-sm"
            :value="selectedCard.dueDate ?? ''"
            @change="onChangeDueDate"
          />
        </div>

        <div class="mb-4">
          <p class="field-label mb-2">{{ t('tasks.labels') }}</p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="label in predefinedLabels"
              :key="label.id"
              type="button"
              class="rounded-full px-3 py-1 text-xs font-medium text-white transition"
              :style="{ backgroundColor: label.colorHex, opacity: selectedCard.labelIds.includes(label.id) ? 1 : 0.35 }"
              @click="onTogglePredefinedLabel(label)"
            >
              {{ label.name }}
            </button>
            <span
              v-for="label in customLabelsForSelectedCard"
              :key="label.id"
              class="flex items-center gap-1 rounded-full px-3 py-1 text-xs font-medium text-white"
              :style="{ backgroundColor: label.colorHex }"
            >
              {{ label.name }}
              <button type="button" class="leading-none opacity-80 hover:opacity-100" @click="onRemoveCustomLabel(label)">×</button>
            </span>
          </div>
          <div class="mt-2 flex flex-wrap items-center gap-2">
            <input v-model="newLabelName" type="text" :placeholder="t('tasks.newLabelPlaceholder')" class="field-input flex-1 text-sm" />
            <button
              v-for="color in LABEL_COLORS"
              :key="color"
              type="button"
              class="h-5 w-5 rounded-full border-2"
              :style="{ backgroundColor: color, borderColor: newLabelColor === color ? '#1F2937' : 'transparent' }"
              @click="newLabelColor = color"
            />
            <button type="button" class="btn-secondary py-1 text-xs" @click="onCreateCustomLabel">{{ t('tasks.newLabelButton') }}</button>
          </div>
        </div>

        <div class="mb-4">
          <p class="field-label mb-2">{{ t('tasks.assignees') }}</p>
          <p v-if="siteMembers.length === 0" class="text-xs text-steel-500 dark:text-steel-400">
            {{ t('tasks.noMembers') }}
          </p>
          <div v-else class="flex flex-wrap gap-2">
            <button
              v-for="member in siteMembers"
              :key="member.membershipId"
              type="button"
              class="rounded-full border px-3 py-1 text-xs font-medium transition"
              :class="
                selectedCard.assigneeIds.includes(member.membershipId)
                  ? 'border-blueprint-500 bg-blueprint-50 text-blueprint-700 dark:bg-blueprint-500/10 dark:text-blueprint-300'
                  : 'border-steel-300 text-steel-600 dark:border-steel-700 dark:text-steel-300'
              "
              @click="onToggleAssignee(member)"
            >
              {{ member.displayName || member.email || '—' }}
            </button>
          </div>
        </div>

        <div class="mb-4">
          <p class="field-label mb-2">{{ t('tasks.attachments.title') }}</p>
          <p v-if="attachmentsError" class="mb-2 text-xs text-safety-600 dark:text-safety-500">{{ attachmentsError }}</p>
          <ul class="mb-2 space-y-1.5">
            <li
              v-for="attachment in cardAttachments"
              :key="attachment.id"
              class="group flex items-center gap-2 rounded-lg bg-steel-100 px-3 py-2 dark:bg-steel-800"
            >
              <button type="button" class="flex min-w-0 flex-1 items-center gap-2 text-left text-sm" @click="onPreviewCardAttachment(attachment)">
                <svg
                  v-if="attachmentIconKind(attachment) === 'pdf'"
                  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                  class="h-4 w-4 shrink-0 text-safety-500"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 3h7l5 5v11a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14 3v5h5" />
                </svg>
                <svg
                  v-else-if="attachmentIconKind(attachment) === 'image'"
                  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                  class="h-4 w-4 shrink-0 text-emerald-500"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M21 15l-5-5L5 21" />
                </svg>
                <svg
                  v-else-if="attachmentIconKind(attachment) === 'video'"
                  xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
                  class="h-4 w-4 shrink-0 text-blueprint-500"
                >
                  <rect x="3" y="5" width="14" height="14" rx="2" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M17 9l4-2v10l-4-2" />
                </svg>
                <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="h-4 w-4 shrink-0 text-steel-500 dark:text-steel-400">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M7 3h7l5 5v11a2 2 0 01-2 2H7a2 2 0 01-2-2V5a2 2 0 012-2z" />
                  <path stroke-linecap="round" stroke-linejoin="round" d="M14 3v5h5" />
                </svg>
                <span class="min-w-0 flex-1 truncate">{{ attachment.originalName }}</span>
                <span class="shrink-0 text-xs text-steel-500 dark:text-steel-400">{{ attachment.folderPath ?? t('siteDocumentProjects.root') }}</span>
              </button>
              <button
                type="button"
                class="btn-ghost shrink-0 p-1 opacity-0 transition group-hover:opacity-100"
                :title="t('siteDocumentProjects.download')"
                @click="onDownloadCardAttachment(attachment)"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-3.5 w-3.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 12l-4-4m4 4l4-4M5 20h14" />
                </svg>
              </button>
            </li>
            <li v-if="cardAttachments.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('tasks.attachments.empty') }}</li>
          </ul>

          <label v-if="canManageTasks && canViewProjects" class="inline-block text-xs font-medium text-blueprint-600 hover:underline dark:text-blueprint-400">
            {{ t('tasks.attachments.attachButton') }}
            <input type="file" accept=".jpg,.jpeg,.png,.pdf,.mp4" class="hidden" @change="onChooseAttachFile" />
          </label>

          <div v-if="showFolderPicker" class="mt-2 rounded-lg border border-steel-200 p-3 dark:border-steel-700">
            <p class="mb-2 text-xs font-medium text-steel-600 dark:text-steel-300">{{ t('tasks.attachments.chooseFolder') }}</p>
            <nav class="mb-2 flex flex-wrap items-center gap-1 text-xs">
              <button type="button" class="rounded px-1.5 py-0.5 hover:bg-steel-100 dark:hover:bg-steel-800" @click="pickerNavigateTo(null)">
                {{ t('tasks.attachments.root') }}
              </button>
              <template v-for="crumb in pickerBreadcrumbs" :key="crumb.id">
                <span class="text-steel-400 dark:text-steel-600">/</span>
                <button type="button" class="rounded px-1.5 py-0.5 hover:bg-steel-100 dark:hover:bg-steel-800" @click="pickerNavigateTo(crumb.id)">
                  {{ crumb.name }}
                </button>
              </template>
            </nav>
            <ul v-if="!pickerLoading" class="mb-2 max-h-32 space-y-1 overflow-y-auto">
              <li v-for="folder in pickerFolders" :key="folder.id">
                <button type="button" class="flex w-full items-center gap-1.5 rounded px-1.5 py-1 text-xs hover:bg-steel-100 dark:hover:bg-steel-800" @click="pickerNavigateTo(folder.id)">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" class="h-3.5 w-3.5 shrink-0 text-blueprint-500">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V7z" />
                  </svg>
                  {{ folder.name }}
                </button>
              </li>
            </ul>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-secondary py-1 text-xs" @click="onCancelAttach">{{ t('tasks.attachments.cancel') }}</button>
              <button type="button" :disabled="attaching" class="btn-primary py-1 text-xs" @click="onConfirmAttach">
                {{ attaching ? t('tasks.attachments.uploading') : t('tasks.attachments.uploadHere') }}
              </button>
            </div>
          </div>
        </div>

        <div class="mb-4">
          <p class="field-label mb-2">{{ t('tasks.comments') }}</p>
          <ul class="mb-3 space-y-2">
            <li v-for="comment in comments" :key="comment.id" class="rounded-lg bg-steel-100 px-3 py-2 text-sm dark:bg-steel-800">
              <p class="whitespace-pre-wrap">{{ comment.body }}</p>
              <div class="mt-1.5 flex justify-end">
                <span class="rounded-full bg-steel-200 px-2 py-0.5 text-[10px] font-medium text-steel-600 dark:bg-steel-700 dark:text-steel-300">
                  {{ comment.authorName ?? t('tasks.unknownAuthor') }}
                </span>
              </div>
            </li>
            <li v-if="comments.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('tasks.noComments') }}</li>
          </ul>
          <form class="flex gap-2" @submit.prevent="onAddComment">
            <input v-model="newCommentBody" type="text" :placeholder="t('tasks.newCommentPlaceholder')" class="field-input flex-1 text-sm" />
            <button type="submit" :disabled="postingComment" class="btn-primary py-1 text-xs">{{ t('tasks.addComment') }}</button>
          </form>
        </div>

        <div class="relative border-t border-steel-100 pt-3 dark:border-steel-800">
          <button type="button" class="btn-danger py-1 text-xs" @click="confirmingDelete = true">{{ t('tasks.deleteCard') }}</button>

          <div
            v-if="confirmingDelete"
            class="modal-panel absolute bottom-full left-0 z-10 mb-2 w-64 p-3 shadow-lg"
          >
            <p class="mb-3 text-xs text-steel-600 dark:text-steel-300">{{ t('tasks.deleteCardConfirm') }}</p>
            <div class="flex justify-end gap-2">
              <button type="button" class="btn-secondary py-1 text-xs" @click="confirmingDelete = false">{{ t('tasks.cancel') }}</button>
              <button type="button" class="btn-danger py-1 text-xs" @click="onConfirmDeleteCard">{{ t('tasks.deleteCard') }}</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="previewAttachment" class="fixed inset-0 z-40 flex items-center justify-center bg-black/60 p-4" @click.self="closeAttachmentPreview">
      <div
        class="modal-panel flex flex-col overflow-hidden"
        :class="previewIsPdf ? 'h-[85vh] w-[85vw] max-w-none' : 'max-h-[90vh] w-full max-w-3xl'"
      >
        <div class="flex shrink-0 items-center justify-between gap-3 border-b border-steel-200 px-4 py-3 dark:border-steel-700">
          <p class="min-w-0 flex-1 truncate text-sm font-medium text-steel-800 dark:text-steel-50">{{ previewAttachment.originalName }}</p>
          <div class="flex shrink-0 items-center gap-1">
            <button type="button" class="btn-ghost p-1.5" :title="t('siteDocumentProjects.download')" @click="onDownloadCardAttachment(previewAttachment)">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="h-4 w-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 16V4m0 12l-4-4m4 4l4-4M5 20h14" />
              </svg>
            </button>
            <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="closeAttachmentPreview">{{ t('siteDocumentProjects.close') }}</button>
          </div>
        </div>
        <div class="flex min-h-0 flex-1 items-center justify-center" :class="previewIsPdf ? '' : 'overflow-auto p-4'">
          <p v-if="previewLoading" class="text-sm text-steel-500 dark:text-steel-400">{{ t('siteDocumentProjects.loading') }}</p>
          <p v-else-if="previewError" class="text-sm text-safety-600 dark:text-safety-500">{{ previewError }}</p>
          <img
            v-else-if="attachmentIconKind(previewAttachment) === 'image'"
            :src="previewUrl"
            :alt="previewAttachment.originalName"
            class="max-h-[75vh] max-w-full object-contain"
          />
          <video v-else-if="attachmentIconKind(previewAttachment) === 'video'" :src="previewUrl" controls autoplay class="max-h-[75vh] max-w-full" />
          <iframe v-else-if="attachmentIconKind(previewAttachment) === 'pdf'" :src="previewUrl" :title="previewAttachment.originalName" class="h-full w-full" />
        </div>
      </div>
    </div>
  </section>
</template>
