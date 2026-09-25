## 1. Backend: data model

- [x] 1.1 Migration `V59__add_task_card_link_to_schedule_task.sql`: `ALTER TABLE schedule_task ADD COLUMN task_card_id UUID REFERENCES task_card (id);`.
- [x] 1.2 `ScheduleTask.java`: add `taskCardId` field + getter, and a `linkTask(UUID taskCardId, Instant now)` method (mirrors `SiteDocumentProject.linkTask` — sets the field, touches `updatedAt`; no-op guard against overwriting an existing link is the service's job, not the entity's).

## 2. Backend: service and endpoint

- [x] 2.1 Add `NoTaskColumnsAvailableException` and `ScheduleTaskAlreadyLinkedException`; map both in `ScheduleExceptionHandler` (400 and 409 respectively).
- [x] 2.2 `ScheduleService.createLinkedTask(UUID scheduleTaskId, UUID actingUserId)`: `requireTask`, `requireStage`, `requireManage(SCHEDULE)`; reject if `task.getTaskCardId() != null` (`ScheduleTaskAlreadyLinkedException`); resolve the site's company via `siteRepository`, look up `taskColumnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)`, take the first (else `NoTaskColumnsAvailableException`); call `taskCardService.createCard(siteId, actingUserId, new TaskCardCreationRequest(firstColumn.getId(), task.getTitle(), null, task.getEndDate()))` (this call already enforces `TASKS` `MANAGE`); if the schedule task has a `responsibleSiteMembershipId`, call `taskCardService.assign(card.getId(), actingUserId, responsibleSiteMembershipId)` to carry the responsible over as the card's assignee; `task.linkTask(createdCard.getId(), Instant.now())`, save, return the updated `ScheduleTaskResponse`.
- [x] 2.3 `ScheduleTaskResponse`: add `taskCardId` and `taskCardTitle` (nullable) fields; `ScheduleService`'s existing `toTaskResponse`/task-mapping helpers look up the linked card's title (when `taskCardId` is present) via `taskCardRepository.findById(...)`, tolerating a missing card (deleted independently) by leaving `taskCardTitle` null.
- [x] 2.4 `ScheduleController.java`: `POST /api/schedule-tasks/{id}/task-card` → `createLinkedTask`, returns the updated `ScheduleTaskResponse`.

## 3. Backend tests

- [x] 3.1 `ScheduleServiceTest.java`: creating a linked card sets `taskCardId`/`taskCardTitle` and uses the first company column by `sortOrder`, title/due-date carried over from the schedule task; a second attempt on an already-linked task is rejected; a member without `TASKS` `MANAGE` is rejected (via the delegated `TaskCardService.createCard` call); a company with zero task columns is rejected clearly.
- [x] 3.2 Run `./mvnw test` from `pantheon-service/` and confirm the full suite passes.

## 4. Frontend

- [x] 4.1 `useConstructionSchedule.ts`: add `taskCardId`/`taskCardTitle` to the `ScheduleTask` interface; add `createLinkedTaskCard(taskId): Promise<ScheduleTask>` (`POST /api/schedule-tasks/{id}/task-card`).
- [x] 4.2 `SchedulePanel.vue`'s task edit modal: when `editingTask.taskCardId` is set, show its title (as a clickable link emitting `open-tab: 'tasks'`) instead of the create action; otherwise, when `canManageTasks` (new prop), show a "Criar task" button calling `createLinkedTaskCard` then reloading.
- [x] 4.3 `SiteDetailView.vue`: pass `:can-manage-tasks="myPermissions?.TASKS === 'MANAGE'"` to `SchedulePanel`, alongside the existing `open-tab` handler already wired for the summary panel (reuse `activeTab = $event`).
- [x] 4.4 Add the new i18n strings (`schedule.task.createLinkedTask`, `schedule.task.linkedTaskLabel`, `schedule.task.linkError`, `schedule.task.noColumnsError`) to `pt-BR.json`.

## 5. Verification

- [x] 5.1 Backend: `./mvnw test` green.
- [x] 5.2 Frontend: `npx vue-tsc --noEmit` and `npm run build` green.
- [x] 5.3 Static/live check: confirm the "Criar task" action only shows for a `MANAGE`+`MANAGE` viewer on an unlinked task, confirm the linked-card title replaces it after creating one, confirm clicking it switches to the Tasks tab.
