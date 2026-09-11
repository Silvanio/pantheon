<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTaskCards, type TaskBoard, type TaskCard, type TaskComment, type TaskLabel } from '../composables/useTaskCards'
import { useTaskLabels } from '../composables/useTaskLabels'
import { useSiteMembers, type SiteMember } from '../composables/useSiteMembers'
import { vDatePicker } from '../lib/datePicker'

const props = defineProps<{ siteId: string }>()

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
} = useTaskCards()
const { listCompanyLabels } = useTaskLabels()
const { listMembers } = useSiteMembers()

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

const sortedColumns = computed(() => [...board.value.columns].sort((a, b) => a.sortOrder - b.sortOrder))
const companyId = computed(() => board.value.columns[0]?.companyId ?? null)

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
      predefinedLabels.value = await listCompanyLabels(companyId.value)
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
  comments.value = await listComments(card.id)
}

function closeCard() {
  selectedCard.value = null
  comments.value = []
  newCommentBody.value = ''
  confirmingDelete.value = false
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

onMounted(load)
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
        v-for="column in sortedColumns"
        :key="column.id"
        class="w-72 shrink-0 rounded-xl bg-steel-100 p-3 dark:bg-steel-800/60"
        @dragover.prevent
        @drop="onDrop(column.id)"
      >
        <h3 class="mb-3 px-1 text-sm font-semibold text-steel-700 dark:text-steel-200">{{ column.name }}</h3>

        <div class="space-y-2">
          <div
            v-for="card in cardsForColumn(column.id)"
            :key="card.id"
            draggable="true"
            class="relative w-full cursor-pointer rounded-lg bg-white p-3 pr-14 pb-5 text-left shadow-sm transition hover:shadow dark:bg-steel-900"
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

            <span
              v-if="card.commentCount > 0"
              class="absolute bottom-1.5 right-2 flex items-center gap-1 text-[10px] text-steel-400 dark:text-steel-500"
            >
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
  </section>
</template>
