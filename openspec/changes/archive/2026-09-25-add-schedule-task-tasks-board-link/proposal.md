## Why

Cronograma tasks and Tasks-board cards are two separate ways of tracking obra work today, with no bridge between them. A schedule task that represents real day-to-day work (e.g. "Escavação") often deserves a corresponding card on the Tasks board so it shows up where the team already tracks execution — today that means manually re-typing it. There's already a precedent for linking a schedule-adjacent item to a Tasks-board card (`SiteDocumentProject.taskCardId`, used by "Projetos" folders), but that only links to an *existing* card; a Cronograma task should be able to *create* one directly.

## What Changes

- A Cronograma task's edit panel gains a "Criar task" action that creates a new Tasks-board card (title = the schedule task's title, due date = the schedule task's end date, placed in the company's first task column) and links it to that schedule task.
- Once linked, the schedule task shows the linked card's title and a way to jump to the Tasks board; a task can only be linked to one card (no re-creating once linked).
- Gated by `MANAGE` on both `SCHEDULE` (to edit the schedule task) and `TASKS` (to create a card) — reuses `TaskCardService.createCard`'s own permission check rather than re-implementing it.

## Capabilities

### Modified Capabilities
- `construction-schedule`: a schedule task gains an optional link to a Tasks-board card, creatable from the task's own edit panel.

## Impact

- **Backend (`pantheon-service`)**: migration adding a nullable `task_card_id` column to `schedule_task`; `ScheduleTask` gains a `taskCardId` field and a `linkTask` method (mirrors `SiteDocumentProject.linkTask`); `ScheduleService` gains `createLinkedTask`, delegating card creation to the existing `TaskCardService.createCard`; new `POST /api/schedule-tasks/{id}/task-card` endpoint; `ScheduleTaskResponse` gains `taskCardId`/`taskCardTitle`.
- **Frontend (`pantheon-web`)**: `useConstructionSchedule.ts` gains the new field/call; `SchedulePanel.vue`'s task edit modal gains the "Criar task" action and linked-card display; `SiteDetailView.vue` passes a `canManageTasks` prop through and handles the "jump to Tasks tab" navigation.
- No mobile changes (Cronograma is web-only, per its own original scoping).
