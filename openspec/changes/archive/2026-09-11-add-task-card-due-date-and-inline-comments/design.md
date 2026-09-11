## Context

`TaskCard` (pantheon-service) and `TasksBoardPanel.vue` (pantheon-web) already exist from the just-shipped `obra-tasks-board` feature. Cards currently have no due date. Labels are attached/detached only through a detail modal, and the card face renders them as unlabeled 6x2px color bars. Comments are listed/posted only inside that same modal (`TasksBoardPanel.vue:254-266`). `TaskCardService.moveCard` is the only card mutation beyond creation — there is no generic "update card" endpoint yet.

## Goals / Non-Goals

**Goals:**
- Add an optional due date to `TaskCard`, settable at creation and editable later, visible on the card face.
- Make labels on the card face legible (name + color, not just a color sliver).
- Let a user read and post comments directly from the board, without opening the modal, via a per-card expandable section anchored at the bottom of the card.

**Non-Goals:**
- No due-date reminders, overdue highlighting/sorting, or notifications — display only.
- No change to who can do what: comment-add still requires `TASKS` `MANAGE`, comment-read still only requires `VIEW` (per existing `obra-tasks-board` spec) — same for the new due-date edit (`MANAGE`).
- No change to label create/attach/detach UX — stays in the modal.
- No card title/description editing (still out of scope, same as the original feature).

## Decisions

- **Due date is a separate PATCH endpoint, not folded into `moveCard`.** `PATCH /api/task-cards/{cardId}/due-date` with `UpdateTaskCardDueDateRequest(LocalDate dueDate)` (nullable, to allow clearing it). Keeps `MoveTaskCardRequest` focused on drag-and-drop and avoids widening it for an unrelated field. Mirrors the existing one-concern-per-endpoint pattern (`/move`, `/labels/{labelId}`, `/comments`).
- **`dueDate` is a plain nullable `LocalDate` column**, consistent with how the rest of the codebase models "day-granularity, no timezone" business dates (e.g. `PurchaseRequest` date filtering). No time-of-day component.
- **Comments move out of the modal into a per-card inline expander; the modal is not removed.** The modal still owns label management (create/attach/detach — unchanged) and now also owns due-date editing (a date input next to the title). Splitting "labels can only be managed in the modal" from "comments live on the card" avoids restructuring the modal's label UI in this change while still meeting the request that comments be reachable from the card itself.
- **Expansion state and each expanded card's comments are local component state in `TasksBoardPanel.vue`** (`expandedCardId: Ref<string | null>` — one card open at a time, matching the existing single-`selectedCard` modal pattern already in this file — plus `commentsByCard: Ref<Record<string, TaskComment[]>>` populated lazily on first expand). No new composable state; `useTaskCards().listComments`/`addComment` are reused as-is.
- **Label pills on the card face reuse the same visual as the modal's label buttons** (rounded pill, `colorHex` background, white text, label name) rather than inventing a second style, just sized for the card (smaller padding/text).

## Risks / Trade-offs

- [Two places can now mutate a card concurrently — the inline comment panel and the modal] → Both act through the same `load()`-triggered refetch of the whole board after a mutation, matching the existing pattern (labels already do this today), so there's no new consistency risk beyond what the feature already has.
- [Expanding a card the first time incurs a comment fetch, same as opening the modal did] → Acceptable; comments are cached in `commentsByCard` for the session so re-expanding the same card doesn't refetch.

## Open Questions

None — scope confirmed with the user: due date display + edit, visible named labels, inline expandable comments.
