## Context

`pantheon-web`'s obra detail page (`SiteDetailView.vue`) already reserves a "Cronograma" tab (disabled) and the dashboard already renders a progress bar per obra card — but that bar reads `PLACEHOLDER_PROGRESS`, a status→% constant, not real data. There is no existing chart/Gantt library in `pantheon-web`'s `package.json`, no date-range picker, and no schedule-shaped entity anywhere in `pantheon-service`. The closest existing structure is the Kanban "Tasks" board (`TaskColumn`/`TaskCard`), which is company-scoped (columns are shared across all obras) and has only a single optional `dueDate` — a deliberately different, lighter-weight tool for ad-hoc work items, not a project schedule. It stays untouched.

Market reference: Vobi's cronograma (https://www.vobi.com.br/funcionalidades/cronograma-de-projetos) uses a two-level Etapas→Tarefas hierarchy, a day-grid Gantt with colored bars per stage, inline "NN%" progress on each bar, a today marker, click-to-edit modals, and simple dependency arrows between tasks. This design borrows that shape, scoped down for a first version.

## Goals / Non-Goals

**Goals:**
- Give every obra a real, editable schedule: phases ("Etapas") containing dated tasks ("Tarefas"), each with an owner and a completion percentage.
- Render that schedule as an intuitive, on-brand Gantt chart — day columns grouped by month, colored bars per stage, a today marker, and a way to see/set simple task dependencies.
- Compute the obra's overall progress from that schedule and surface it wherever progress is already shown (currently just the dashboard card), with zero regression for obras that haven't set up a schedule yet.
- Reuse the app's existing permission model (`PermissionCapability`/`AccessLevel`) so schedule visibility and edit rights follow the same rules as every other obra section.

**Non-Goals (v1 — explicitly deferred, not forgotten):**
- Dragging/resizing bars directly on the chart to change dates — v1 only edits dates through a form/modal.
- Automatic cascade rescheduling when a predecessor task's dates change (dependencies are drawn, not enforced).
- Exporting the cronograma (PDF/image).
- Multiple zoom levels (week/quarter) — v1 is a single day-grid view with prev/next month navigation.
- Any mobile (`pantheon-mobile`) work — web only, per explicit request.
- Sub-tasks nested more than one level deep (Vobi shows a 3rd level in one screenshot; v1 stays at Etapa → Tarefa, two levels, which already covers "what phase, what task, who, when, how done").

## Decisions

### Data model: two real tables, no redundant percent on stages
`schedule_stage` (id, construction_site_id, name, color, start_date, end_date, sort_order, created_at, updated_at) and `schedule_task` (id, stage_id, title, start_date, end_date, responsible_site_membership_id nullable, percent_complete 0–100, sort_order, created_at, updated_at). A stage's own progress is **derived** (average of its tasks' `percent_complete`, or `0` if it has no tasks yet) rather than stored — a stored `percent_complete` on `schedule_stage` would be a second source of truth that can drift from its tasks. A stage's `start_date`/`end_date` are still stored directly (not derived from tasks) since an empty stage still needs a plannable date range on the chart before any tasks exist under it.

`schedule_task_dependency` (id, predecessor_task_id, successor_task_id, created_at) is a thin join table purely for drawing an arrow between two bars — see Non-Goals for why it doesn't drive rescheduling. A `UNIQUE(predecessor_task_id, successor_task_id)` constraint prevents duplicate links; a self-link (`predecessor_task_id = successor_task_id`) is rejected at the service layer.

### Progress computation: backend-owned, embedded in the existing site response, nullable
The obra's overall `schedulePercentComplete` = the unweighted average of `percent_complete` across every `schedule_task` on that site (flattening stages — a straight task average is simpler to reason about than a stage-weighted average and avoids picking an arbitrary weighting scheme like "by stage duration" for v1). It is computed by a new `ScheduleService.computeProgress(siteId)` and embedded as a **nullable** field directly on the response `ConstructionSiteService` already builds for the dashboard's site list (not a separate `GET .../schedule-progress` endpoint) — the dashboard already fetches that list on every load, so embedding avoids an extra N+1 round trip per card. It is `null` when the site has zero schedule tasks, and `DashboardView.vue`'s existing `PLACEHOLDER_PROGRESS` lookup becomes the fallback used only when `schedulePercentComplete` is null, so an obra that hasn't touched its cronograma yet renders exactly as it does today.

### Permission capability: `SCHEDULE`, defaults mirror `TASKS` exactly
A construction schedule is close in spirit to the Tasks board (planning/tracking work, not financial or contractual data), so `SCHEDULE` reuses `TASKS`'s exact default matrix: company staff `MANAGE`; `ENGINEER`/`ARCHITECT`/`SITE_FOREMAN` `MANAGE`; `CLIENT`/`SERVICE_PROVIDER` `VIEW`. `VIEW` can see the Gantt read-only; `MANAGE` can create/edit/delete stages, tasks, and dependencies. `HIDDEN` (via override) removes the tab entirely, same as every other capability.

### Gantt rendering: hand-built CSS Grid + inline SVG, no new dependency
`pantheon-web` has zero chart/Gantt libraries today. Pulling one in (e.g. `dhtmlx-gantt`, `frappe-gantt`) would fight the app's existing design tokens (`blueprint-*`/`steel-*`/`ink-*`, Manrope) and add real bundle weight for a feature whose v1 interaction surface (view + click-to-edit, no drag-resize) doesn't need a full Gantt engine. Instead: a `CSS Grid` with one column per visible day (current month, prev/next navigation) for the timeline header and row backgrounds, and stage/task bars as absolutely-positioned `<div>`s spanning `grid-column: start / end` computed from each bar's date range clamped to the visible month. Dependency links are a single `<svg>` overlay (`position: absolute; inset: 0; pointer-events: none`) drawing a path between each dependency's two bar midpoints, recomputed on every render/resize via bar `getBoundingClientRect()`. This keeps the whole feature dependency-free and fully on-brand.

### Editing UX: click bar → side panel, not inline drag
Clicking a stage or task bar (or its row in the left list) opens a `modal-panel`-style edit form (matching the confirm-popover pattern already used across the app) with name/dates/color (stage) or title/dates/responsible/percent/dependencies (task). A `+` control per stage adds a task; a top-level `+` adds a stage. A checkbox next to each task in the left list sets `percent_complete` to `100` (or back to a prior value — last non-100 value is not preserved, unchecking just sets it to `0`) as a fast "mark done" shortcut, mirroring Vobi's checkbox.

## Risks / Trade-offs

- **[Risk] Hand-built Gantt is more work than wiring a library, and CSS Grid date-math (day→column index across month boundaries) is a common source of off-by-one bugs.** → Mitigation: keep the visible range to a single calendar month at a time (no multi-month continuous scroll in v1), which bounds the date math to "clamp this bar's range to [monthStart, monthEnd], then index by day-of-month" — much simpler than a continuous virtualized timeline.
- **[Risk] Unweighted task-average progress can look "wrong" for obras with very uneven task sizes (e.g. one 3-month structural task at 10% pulls the average down as much as five 1-day finishing tasks at 100%).** → Mitigation: documented explicitly as the v1 approach; a future duration-weighted average is a pure backend formula change behind the same nullable field, no schema or API shape change needed.
- **[Risk] Dependency arrows with no cascade logic could mislead a user into thinking dates auto-adjust.** → Mitigation: the dependency UI copy explicitly states it's a visual link only ("apenas indica ordem, não reagenda automaticamente"); full cascade scheduling is called out as future work in the proposal.
- **[Risk] Deleting a stage cascade-deletes its tasks (and any dependency rows referencing them), which is destructive.** → Mitigation: same confirm-before-delete pattern already used for Pedido de Compra/Diário de Obra (a `modal-panel`/`window.confirm` step naming how many tasks will be removed).

## Migration Plan

1. Backend: Flyway `V58__create_schedule_tables.sql` (three tables + FKs + indexes on `construction_site_id`/`stage_id`), new entities/repositories/service/controller/DTOs/exceptions, `PermissionCapability.SCHEDULE`, `SitePermissionService.DEFAULTS` update, `schedulePercentComplete` added to the site list/detail response. Backend test suite (`./mvnw test`) green.
2. Frontend: composable + `SchedulePanel.vue` + Gantt sub-components, wire the existing disabled Cronograma tab, update `DashboardView.vue`'s progress fallback logic, add pt-BR strings. `vue-tsc --noEmit` and `npm run build` green.
3. No data backfill needed — every obra simply has zero schedule rows until someone adds a stage, which is exactly the "keep behaving like today" fallback state.
4. Rollback: the feature is purely additive (new tables, new nullable response field, new gated tab) — reverting the change (or leaving the migration applied but the frontend un-deployed) leaves existing obras and their dashboard cards unaffected.

## Open Questions

- Should `schedule_stage.color` be a free hex string or a fixed palette (matching how the app already avoids one-off hex values per `obra-visual-design`'s shared-tokens requirement)? Leaning toward a small fixed set of swatches (reusing existing `blueprint`/`emerald`/`amber`/`safety`/etc. tokens) picked in the stage form, rather than a free color picker — keeps every cronograma visually consistent with the rest of the app and avoids clashing/illegible custom colors. Resolved during implementation, not blocking the proposal.
