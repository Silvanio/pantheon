## 1. Data model & migration (pantheon-service)

- [x] 1.1 Flyway `V48__add_task_card_due_date.sql`: `ALTER TABLE task_card ADD COLUMN due_date DATE`
- [x] 1.2 `TaskCard` entity: add nullable `dueDate` (`LocalDate`), extend the constructor to accept it, add `getDueDate()`, add `updateDueDate(LocalDate dueDate, Instant now)` setting `updatedAt`

## 2. Backend: create/read with due date

- [x] 2.1 `TaskCardCreationRequest`: add optional `LocalDate dueDate`
- [x] 2.2 `TaskCardService.createCard`: pass `request.dueDate()` into the new `TaskCard`
- [x] 2.3 `TaskCardResponse`: add `dueDate` field and populate it in `from(...)`

## 3. Backend: due date edit endpoint

- [x] 3.1 `UpdateTaskCardDueDateRequest(LocalDate dueDate)` DTO (nullable field, no `@NotNull` — null clears the due date)
- [x] 3.2 `TaskCardService.updateDueDate(cardId, actingUserId, UpdateTaskCardDueDateRequest)`: requires `TASKS` `MANAGE` on the card's site (reuse `requireManage`/`requireCard`), calls `card.updateDueDate(...)`, saves
- [x] 3.3 `TaskCardController`: `PATCH /api/task-cards/{cardId}/due-date`

## 4. Backend tests

- [x] 4.1 `TaskCardServiceTest`: creating a card with a due date persists it; `updateDueDate` sets a new date, clears it with `null`, and rejects a caller without `TASKS` `MANAGE`

## 5. pantheon-web: composable

- [x] 5.1 `TaskCard` interface in `useTaskCards.ts`: add `dueDate: string | null`
- [x] 5.2 `createCard(...)`: accept and send an optional `dueDate`
- [x] 5.3 New `updateDueDate(cardId: string, dueDate: string | null): Promise<TaskCard>` calling `PATCH /api/task-cards/{cardId}/due-date`

## 6. pantheon-web: card face (due date + labels)

- [x] 6.1 New-card form (`TasksBoardPanel.vue`): add a due-date input, pass it through `onCreateCard`
- [x] 6.2 Card face: replace the color-sliver label rendering with named pills (label name + `colorHex` background), matching the modal's pill style at a smaller size
- [x] 6.3 Card face: show the due date (formatted, e.g. `dd/MM/yyyy`) when set

## 7. pantheon-web: inline expandable comments

- [x] 7.1 Add `expandedCardId: Ref<string | null>` and `commentsByCard: Ref<Record<string, TaskComment[]>>` to `TasksBoardPanel.vue`
- [x] 7.2 Card face: collapsed-by-default footer control showing the comment count (fetch lazily just to get a count, or show a generic toggle if a count-only fetch isn't worth adding — reuse `listComments` and cache the result); clicking toggles `expandedCardId` for that card without triggering `openCard` (`@click.stop`)
- [x] 7.3 Expanded section: list `commentsByCard[card.id]`, a form to add a comment via `addComment` + `listComments` refresh, scoped to that card, fetched lazily on first expand and cached afterward
- [x] 7.4 Detail modal: remove the comments section (now redundant); keep label management; add a due-date input (calls `updateDueDate`, refreshes the board)

## 8. i18n

- [x] 8.1 `pt-BR.json` `tasks.*`: add `dueDate`, `dueDatePlaceholder`, `noDueDate`/clear-date affordance, comment-count/expand-toggle strings if needed (reuse existing `comments`/`noComments`/`newCommentPlaceholder`/`addComment` for the inline panel)

## 9. Verification

- [x] 9.1 `pantheon-service`: `mvn -q -o compile`, `mvn -q -o test-compile`, and `mvn -q -o test` all pass
- [x] 9.2 `pantheon-web`: `vue-tsc -b --force` and `vite build` both pass clean
- [x] 9.3 Manual QA in a live browser: create a card with a due date and confirm it shows on the face; edit and clear the due date from the modal; attach two labels and confirm both render as named pills on the card face; expand a card's comment section, post a comment, confirm it appears without the modal opening, collapse it, and confirm state persists across a board reload
- [x] 9.4 Update this file's checkboxes to reflect actual completion as work proceeds
- [x] 9.5 Run `openspec archive` once implemented and verified, updating `openspec/specs/` accordingly
