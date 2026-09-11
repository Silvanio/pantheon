<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTaskCards, type TaskBoard, type TaskCard, type TaskComment, type TaskLabel } from '../composables/useTaskCards'

const props = defineProps<{ siteId: string }>()

const { t } = useI18n()
const { getBoard, createCard, moveCard, listLabels, createLabel, attachLabel, detachLabel, listComments, addComment } =
  useTaskCards()

const LABEL_COLORS = ['#EF4444', '#F97316', '#F59E0B', '#22C55E', '#06B6D4', '#3B82F6', '#8B5CF6', '#EC4899']

const board = ref<TaskBoard>({ columns: [], cards: [] })
const labels = ref<TaskLabel[]>([])
const loading = ref(false)
const errorMessage = ref('')

const newCardOpenFor = ref<string | null>(null)
const newCardTitle = ref('')
const creatingCard = ref(false)

const draggedCardId = ref<string | null>(null)

const selectedCard = ref<TaskCard | null>(null)
const comments = ref<TaskComment[]>([])
const newCommentBody = ref('')
const postingComment = ref(false)
const newLabelName = ref('')
const newLabelColor = ref(LABEL_COLORS[0])

const sortedColumns = computed(() => [...board.value.columns].sort((a, b) => a.sortOrder - b.sortOrder))

function cardsForColumn(columnId: string): TaskCard[] {
  return board.value.cards.filter((c) => c.columnId === columnId).sort((a, b) => a.sortOrder - b.sortOrder)
}

function labelById(id: string): TaskLabel | undefined {
  return labels.value.find((l) => l.id === id)
}

async function load() {
  loading.value = true
  errorMessage.value = ''
  try {
    const [boardResult, labelsResult] = await Promise.all([getBoard(props.siteId), listLabels(props.siteId)])
    board.value = boardResult
    labels.value = labelsResult
  } finally {
    loading.value = false
  }
}

function openNewCardForm(columnId: string) {
  newCardOpenFor.value = columnId
  newCardTitle.value = ''
}

async function onCreateCard(columnId: string) {
  if (!newCardTitle.value.trim()) return
  creatingCard.value = true
  errorMessage.value = ''
  try {
    await createCard(props.siteId, columnId, newCardTitle.value.trim(), null)
    newCardOpenFor.value = null
    newCardTitle.value = ''
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
  comments.value = await listComments(card.id)
}

function closeCard() {
  selectedCard.value = null
  comments.value = []
  newCommentBody.value = ''
}

async function onToggleLabel(label: TaskLabel) {
  if (!selectedCard.value) return
  const card = selectedCard.value
  errorMessage.value = ''
  try {
    if (card.labelIds.includes(label.id)) {
      await detachLabel(card.id, label.id)
    } else {
      await attachLabel(card.id, label.id)
    }
    await load()
    selectedCard.value = board.value.cards.find((c) => c.id === card.id) ?? null
  } catch {
    errorMessage.value = t('tasks.error')
  }
}

async function onCreateLabel() {
  if (!newLabelName.value.trim()) return
  errorMessage.value = ''
  try {
    await createLabel(props.siteId, newLabelName.value.trim(), newLabelColor.value)
    newLabelName.value = ''
    labels.value = await listLabels(props.siteId)
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
  } catch {
    errorMessage.value = t('tasks.error')
  } finally {
    postingComment.value = false
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
          <button
            v-for="card in cardsForColumn(column.id)"
            :key="card.id"
            type="button"
            draggable="true"
            class="block w-full rounded-lg bg-white p-3 text-left shadow-sm transition hover:shadow dark:bg-steel-900"
            @dragstart="onDragStart(card)"
            @click="openCard(card)"
          >
            <p class="text-sm font-medium text-steel-800 dark:text-steel-50">{{ card.title }}</p>
            <div v-if="card.labelIds.length" class="mt-2 flex flex-wrap gap-1">
              <span
                v-for="labelId in card.labelIds"
                :key="labelId"
                class="h-2 w-6 rounded-full"
                :style="{ backgroundColor: labelById(labelId)?.colorHex }"
              />
            </div>
          </button>
        </div>

        <form v-if="newCardOpenFor === column.id" class="mt-2 space-y-2" @submit.prevent="onCreateCard(column.id)">
          <input
            v-model="newCardTitle"
            type="text"
            :placeholder="t('tasks.newCardPlaceholder')"
            class="field-input w-full text-sm"
            autofocus
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
      <div class="card card-pad max-h-[85vh] w-full max-w-lg overflow-y-auto">
        <div class="mb-4 flex items-start justify-between gap-3">
          <h3 class="text-lg font-semibold text-steel-800 dark:text-steel-50">{{ selectedCard.title }}</h3>
          <button type="button" class="btn-ghost px-2 py-1 text-xs" @click="closeCard">{{ t('tasks.close') }}</button>
        </div>

        <div class="mb-4">
          <p class="field-label mb-2">{{ t('tasks.labels') }}</p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="label in labels"
              :key="label.id"
              type="button"
              class="rounded-full px-3 py-1 text-xs font-medium text-white transition"
              :style="{ backgroundColor: label.colorHex, opacity: selectedCard.labelIds.includes(label.id) ? 1 : 0.35 }"
              @click="onToggleLabel(label)"
            >
              {{ label.name }}
            </button>
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
            <button type="button" class="btn-secondary py-1 text-xs" @click="onCreateLabel">{{ t('tasks.newLabelButton') }}</button>
          </div>
        </div>

        <div>
          <p class="field-label mb-2">{{ t('tasks.comments') }}</p>
          <ul class="mb-3 space-y-2">
            <li v-for="comment in comments" :key="comment.id" class="rounded-lg bg-steel-100 px-3 py-2 text-sm dark:bg-steel-800">
              {{ comment.body }}
            </li>
            <li v-if="comments.length === 0" class="text-sm text-steel-500 dark:text-steel-400">{{ t('tasks.noComments') }}</li>
          </ul>
          <form class="flex gap-2" @submit.prevent="onAddComment">
            <input v-model="newCommentBody" type="text" :placeholder="t('tasks.newCommentPlaceholder')" class="field-input flex-1 text-sm" />
            <button type="submit" :disabled="postingComment" class="btn-primary py-1 text-xs">{{ t('tasks.addComment') }}</button>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>
