## Context

`SiteDocumentProject.taskCardId` already establishes the pattern for linking a non-Tasks-board item to a `TaskCard`: a nullable FK, a `linkTask(taskCardId, actingUserId, now)` domain method, and a service method requiring the two relevant capabilities. That precedent only *links* to an already-existing card (`SiteDocumentProjectService.setFolderTaskLink`); Cronograma needs to *create* one, since a schedule task rarely already has a matching card.

## Goals / Non-Goals

**Goals:**
- One click on a Cronograma task creates a real Tasks-board card carrying that task's title and due date, and remembers the link.
- Reuse `TaskCardService.createCard` as-is (same permission check, same SSE event, same card shape) rather than duplicating card-creation logic in `ScheduleService`.

**Non-Goals:**
- No card→schedule-task link stays in sync afterward (e.g. moving the card doesn't touch the schedule task, editing the schedule task doesn't touch the card) — this is a one-time "create and remember", not a live sync, matching `SiteDocumentProject`'s own link (also one-directional, never cascaded).
- No column picker — the card goes into the company's first task column (by `sortOrder`); reassigning it afterward is an ordinary Tasks-board drag, already supported.
- No unlinking or re-linking to a different card in this pass — once linked, the action becomes "vinculada" chrome, not a picker.
- No mobile UI (Cronograma itself is web-only).

## Decisions

**Reuse `TaskCardService.createCard`, don't reimplement it in `ScheduleService`.** `ScheduleService.createLinkedTask` calls it directly with a `TaskCardCreationRequest(firstColumnId, task.getTitle(), null, task.getEndDate())`. This means the new action automatically inherits the exact same `TASKS` `MANAGE` check, SSE broadcast, and card shape as creating a card from the Tasks board itself — no drift risk between two creation paths.

**"First column" is the company's task columns ordered by `sortOrder`, first one.** Task columns are company-wide (shared across every obra), already fetched via `TaskColumnRepository.findByCompanyIdOrderBySortOrderAsc`. If a company has configured zero columns (only possible for a brand-new company that never touched Tasks), the action fails with a clear error rather than silently doing nothing.

**One link per task, enforced server-side.** `createLinkedTask` rejects (409) if `task.getTaskCardId()` is already set — the UI hides the "Criar task" action once linked anyway, but the server is the actual guard.

## Risks / Trade-offs

- **[Risk] The linked card can be deleted from the Tasks board independently, leaving `schedule_task.task_card_id` pointing at nothing.** → Mitigation: same trade-off `SiteDocumentProject.taskCardId` already accepts (that link is also never cascaded/cleared on card deletion, per its own doc comment) — consistent with the existing precedent rather than a new gap. The response simply stops including a `taskCardTitle` once the card is gone (a lookup miss), and the UI falls back to offering "Criar task" again.

## Migration Plan

1. Backend: `V59__add_task_card_link_to_schedule_task.sql`, `ScheduleTask.taskCardId`/`linkTask`, `ScheduleService.createLinkedTask`, controller endpoint, `ScheduleTaskResponse` fields; `./mvnw test`.
2. Frontend: composable + `SchedulePanel.vue` UI; `vue-tsc`/`npm run build`.
3. No data backfill — every existing schedule task simply has `task_card_id = null` until linked.
