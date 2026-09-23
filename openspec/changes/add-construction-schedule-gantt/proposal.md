## Why

An obra's real physical progress (is it still in planning, mid-construction, nearly done?) has no source of truth today. The dashboard shows a `PLACEHOLDER_PROGRESS` percentage derived purely from the obra's coarse `status` enum (`PLANNING`→10%, `IN_PROGRESS`→55%, `PAUSED`→40%, `COMPLETED`→100%) — a fabricated constant, not real data, explicitly flagged in code as a stand-in ("replace once a real progress-tracking feature exists"). `SiteDetailView.vue` also already has a disabled "Cronograma" tab stub waiting to be built. Users need an actual schedule — phases and tasks with dates, owners, and completion — visualized as a Gantt chart, both to plan/track the obra and to drive a real progress percentage.

## What Changes

- New "Cronograma" section on the obra detail page (enables the existing disabled tab) showing a Gantt chart: colored phases ("Etapas"), each containing dated tasks ("Tarefas"), a today marker, and simple task-to-task dependency links (visual only, no auto-rescheduling).
- New `SCHEDULE` permission capability gating that section (read via `VIEW`+, write via `MANAGE`), with defaults mirroring `TASKS`.
- New backend entities/endpoints for stages, tasks, and task dependencies, scoped to a construction site.
- The obra's overall progress percentage is now computed from its schedule tasks' completion (once any exist) and exposed on the existing construction-site list/detail responses; the dashboard uses it when present and falls back to today's placeholder otherwise (obras with no schedule configured yet keep working exactly as they do now).
- Web only — mobile (`pantheon-mobile`) is explicitly out of scope for this change.

## Capabilities

### New Capabilities
- `construction-schedule`: Gantt-based construction schedule (phases, tasks, dependencies) per obra, and the obra-level progress percentage derived from it.

### Modified Capabilities
- `obra-permission-management`: the "Default permissions by function" requirement's capability list and per-function defaults gain `SCHEDULE`, mirroring `TASKS`'s defaults exactly (company staff `MANAGE`; `ENGINEER`/`ARCHITECT`/`SITE_FOREMAN` `MANAGE`; `CLIENT`/`SERVICE_PROVIDER` `VIEW`).

## Impact

- **Backend (`pantheon-service`)**: new tables `schedule_stage`, `schedule_task`, `schedule_task_dependency` (migration `V58`); new entities/repositories/service/controller/DTOs/exceptions; `PermissionCapability` gains `SCHEDULE`; `SitePermissionService`'s `DEFAULTS` map gains `SCHEDULE` rows; `ConstructionSiteResponse` (or equivalent list/detail DTO) gains a nullable `schedulePercentComplete` field computed by the new schedule service.
- **Frontend (`pantheon-web`)**: new `SchedulePanel.vue`, `useConstructionSchedule.ts` composable; `SiteDetailView.vue`'s disabled Cronograma tab becomes real (`TAB_ORDER`, `TAB_CAPABILITY`); `useSitePermissions.ts` capability type gains `'SCHEDULE'`; `DashboardView.vue` uses `schedulePercentComplete` when present, else keeps `PLACEHOLDER_PROGRESS`; new pt-BR locale strings.
- **No mobile changes.** No new third-party dependency — the Gantt is hand-built with CSS Grid/SVG reusing existing design tokens.
