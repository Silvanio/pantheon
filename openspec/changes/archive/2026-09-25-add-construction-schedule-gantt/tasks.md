## 1. Backend data model

- [x] 1.1 Create `pantheon-service/src/main/resources/db/migration/V58__create_schedule_tables.sql`: `schedule_stage` (id UUID PK, construction_site_id FK, name, color, start_date, end_date, sort_order, created_at, updated_at), `schedule_task` (id UUID PK, stage_id FK, title, start_date, end_date, responsible_site_membership_id FK nullable, percent_complete INT NOT NULL DEFAULT 0, sort_order, created_at, updated_at), `schedule_task_dependency` (id UUID PK, predecessor_task_id FK, successor_task_id FK, created_at, `UNIQUE(predecessor_task_id, successor_task_id)`). Index `schedule_stage(construction_site_id)` and `schedule_task(stage_id)`.
- [x] 1.2 Add entities `ScheduleStage`, `ScheduleTask`, `ScheduleTaskDependency` under `pantheon-service/src/main/java/com/pantheon/service/entity/` following the existing entity style (see `DailyReport.java` for constructor/update-method conventions).
- [x] 1.3 Add repositories `ScheduleStageRepository`, `ScheduleTaskRepository`, `ScheduleTaskDependencyRepository` under `.../repository/` with `findByConstructionSiteIdOrderBySortOrderAsc`, `findByStageIdOrderBySortOrderAsc`, `findByPredecessorTaskIdOrFindBySuccessorTaskId` (or a combined `findByPredecessorTaskIdInOrSuccessorTaskIdIn`) for cascade-delete lookups.

## 2. Backend permission capability

- [x] 2.1 Add `SCHEDULE` to `PermissionCapability.java`.
- [x] 2.2 Update `SitePermissionService.java`'s static `DEFAULTS` map: `ENGINEER`/`ARCHITECT`/`SITE_FOREMAN` → `MANAGE`, `CLIENT`/`SERVICE_PROVIDER` → `VIEW` (mirror `TASKS` row-for-row).
- [x] 2.3 Update `SitePermissionServiceTest` (or equivalent) with the two new default-scenario assertions from the delta spec (site foreman MANAGE, client VIEW on `SCHEDULE`).

## 3. Backend service, controller, DTOs, exceptions

- [x] 3.1 Add DTOs under `.../dto/`: `ScheduleStageCreationRequest`, `ScheduleStageUpdateRequest`, `ScheduleTaskCreationRequest`, `ScheduleTaskUpdateRequest`, `ScheduleStageResponse` (nested list of `ScheduleTaskResponse`, plus a derived `percentComplete` computed in the DTO's `from()` factory), `ScheduleTaskResponse`, `ScheduleDependencyRequest`.
- [x] 3.2 Add `ScheduleService.java`: `listStages(siteId, actingUserId)` (requireVisible SCHEDULE), `createStage`, `updateStage`, `deleteStage` (cascade: delete its tasks, then any dependency rows referencing those tasks, then the stage), `createTask`, `updateTask`, `deleteTask` (also removes dependency rows referencing it), `linkDependency` (reject self-link and duplicate link), `unlinkDependency` — every write method calls `requireManage(siteId, actingUserId, PermissionCapability.SCHEDULE)` following `DailyReportService`'s `requireManage`/`requireReport` pattern.
- [x] 3.3 Add `ScheduleService.computeProgress(UUID siteId)`: returns `Integer` (average `percent_complete` across all tasks for the site, rounded, `null` if zero tasks) — used by task 4.
- [x] 3.4 Add `ScheduleController.java`: `GET/POST /api/construction-sites/{siteId}/schedule-stages`, `PATCH/DELETE /api/schedule-stages/{id}`, `POST /api/schedule-stages/{stageId}/tasks`, `PATCH/DELETE /api/schedule-tasks/{id}`, `POST /api/schedule-tasks/{id}/dependencies`, `DELETE /api/schedule-tasks/{id}/dependencies/{depId}`.
- [x] 3.5 Add exceptions (`ScheduleStageNotFoundException`, `ScheduleTaskNotFoundException`, `SelfDependencyException`, `DuplicateDependencyException`) and a `ScheduleExceptionHandler` (`@RestControllerAdvice`) mapping them to 404/400/409 as appropriate, following `DailyReportExceptionHandler`'s style.

## 4. Backend: progress on the site response

- [x] 4.1 Find the DTO `ConstructionSiteService` uses for the site list/detail response (used by `pantheon-web`'s dashboard) and add a nullable `schedulePercentComplete` field.
- [x] 4.2 Wire `ConstructionSiteService` (or its controller) to call `ScheduleService.computeProgress(siteId)` when building that response.

## 5. Backend tests

- [x] 5.1 `ScheduleServiceTest.java` (Mockito, mirroring `DailyReportServiceTest.java`'s structure): stage/task CRUD, cascade delete removes tasks and dependency rows, self-dependency rejected, duplicate dependency rejected, `computeProgress` returns null for zero tasks and the correct average otherwise, write methods reject non-MANAGE access.
- [x] 5.2 Run `./mvnw test` from `pantheon-service/` and confirm the full suite passes.

## 6. Frontend: types and composable

- [x] 6.1 Add `'SCHEDULE'` to the capability union type in `pantheon-web/src/composables/useSitePermissions.ts`.
- [x] 6.2 Add `pantheon-web/src/composables/useConstructionSchedule.ts`: types (`ScheduleStage`, `ScheduleTask`, dependency), `listStages`, `createStage`, `updateStage`, `deleteStage`, `createTask`, `updateTask`, `deleteTask`, `linkDependency`, `unlinkDependency`, following `useDailyReports.ts`'s `authFetch` pattern.
- [x] 6.3 Add `schedulePercentComplete` to the `ConstructionSite` type in `useConstructionSites.ts`/`useMySites.ts`.

## 7. Frontend: Gantt chart component

- [x] 7.1 Add `pantheon-web/src/components/schedule/ScheduleGantt.vue`: month header + day-of-week row (CSS Grid, one column per day of the visible month), prev/next month navigation, a highlighted "today" column.
- [x] 7.2 Render stage/task bars as absolutely-positioned elements spanning `grid-column` computed from each item's date range clamped to the visible month; task bars nested under their stage's row, colored by the stage's color, showing the item's name and `NN%`.
- [x] 7.3 Render dependency links as an `<svg>` overlay connecting linked task bars' midpoints (recomputed on resize/month navigation).
- [x] 7.4 Clicking a bar emits an event with that stage/task's id (consumed by `SchedulePanel.vue` to open the edit form).

## 8. Frontend: SchedulePanel

- [x] 8.1 Add `pantheon-web/src/components/SchedulePanel.vue` (props: `siteId`), loading stages via `useConstructionSchedule`, rendering a left list of expandable stages (each row: color swatch, name, start/end, a "done" checkbox per task setting `percentComplete` to 100/0) beside `ScheduleGantt.vue`.
- [x] 8.2 "Nova etapa" form (name, color swatch picker reusing existing design tokens, start/end dates via `v-date-picker`) and, per stage, "Nova tarefa" form (title, start/end dates, responsible dropdown sourced from `useSiteMembers.ts`, percent complete).
- [x] 8.3 Edit modal/panel for an existing stage or task (same fields as creation, plus delete with a `window.confirm`-style guard naming cascade impact for a stage delete).
- [x] 8.4 "Depende de" dropdown in the task edit form listing other tasks on the site, calling `linkDependency`/`unlinkDependency`; static helper copy noting the link doesn't auto-reschedule.
- [x] 8.5 Gate every create/edit/delete control on `MANAGE` access to `SCHEDULE` (resolve via the site's `myPermissions`, same pattern as `TasksBoardPanel.vue`); read-only rendering for `VIEW`.

## 9. Frontend: tab wiring

- [x] 9.1 In `SiteDetailView.vue`: add `schedule: 'SCHEDULE'` to `TAB_CAPABILITY`, add `'schedule'` to `TAB_ORDER`, replace both disabled Cronograma buttons (desktop + mobile nav) with real ones driven by `isTabVisible('schedule')`, and mount `<SchedulePanel v-if="activeTab === 'schedule' && isTabVisible('schedule')" :site-id="siteId" />` alongside the other panels.
- [x] 9.2 Remove the now-unused `scheduleDisabled` i18n key usage (keep or remove the key itself depending on whether anything else references it).

## 10. Frontend: dashboard progress

- [x] 10.1 In `DashboardView.vue`, change the progress display to use `site.schedulePercentComplete ?? PLACEHOLDER_PROGRESS[site.status]` wherever `PLACEHOLDER_PROGRESS` is currently read (both the % text and the bar width), keeping the existing comment but noting the fallback is now live, not purely aspirational.

## 11. i18n

- [x] 11.1 Add a `schedule` section to `pantheon-web/src/locales/pt-BR.json`: tab label (reuse/replace `siteDetail.tabs.schedule`), form labels (etapa/tarefa name, dates, responsável, % concluído, cor, depende de), empty states, delete-confirmation copy, error messages — following the naming style of the `dailyReports` section.

## 12. Verification

- [x] 12.1 Backend: `./mvnw test` green.
- [x] 12.2 Frontend: `npx vue-tsc --noEmit` and `npm run build` green.
- [x] 12.3 Live check in the browser pane: create a stage and a task, confirm the bar renders in the right date position and color, mark a task done via checkbox and confirm the stage/dashboard percentages update, confirm a `VIEW`-only member sees no edit controls, confirm an obra with no schedule still shows today's placeholder percentage unchanged on the dashboard.
