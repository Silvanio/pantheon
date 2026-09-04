## Why

This is the core value proposition of the product: a daily construction log (Relatório Diário de Obra / RDO), matching the feature the reference competitor (diariodeobras.net) built its whole product around. Without it, a `ConstructionSite` is just a record with a status and nothing else — this change gives each site an actual day-to-day operating log: weather, work hours, workforce, equipment usage, activity progress, occurrences, materials received, and comments.

## What Changes

- Add `DailyReport`: one report per `ConstructionSite` per calendar date, sequentially numbered per site.
- Each report carries, as sub-sections of the same report:
  - **Weather** (condition + whether planned tasks were executable)
  - **Work hours** (hours logged for the day)
  - **Workforce** (who was present — own staff or service provider, function/specialty, headcount, optionally linked to a registered `ProjectMembership`)
  - **Equipment usage** (equipment used that day, referencing the site's `Equipment` registry, with a status note)
  - **Activities** (activities in progress/completed that day, with a progress note)
  - **Occurrences** (free-text occurrences/incidents)
  - **Materials received** (materials received that day, referencing the site's `Material` catalog, with quantity) — a simple log entry, distinct from the formal request/approval/verification workflow of `add-material-request-workflow`
  - **Comments** (free-text comments)
- Any project member can create/edit their site's daily report for a given date while it is in `DRAFT`; once submitted (`SUBMITTED`) it becomes read-only content-wise (media/signatures, added in a later change, remain possible).
- `pantheon-web`: a daily report creation/edit view with all sections, plus a per-site report history/list view — all copy sourced from the `pt-BR` locale resource file.

## Capabilities

### New Capabilities
- `daily-construction-report`: the DailyReport entity and its eight logging sections (weather, hours, workforce, equipment usage, activities, occurrences, materials received, comments), plus creation/listing/detail views.

### Modified Capabilities
(none)

## Impact

- **Affected code**: `pantheon-service` (new `DailyReport` entity and one child entity per repeatable section, repositories, service, controller, DTOs; new Flyway migrations), `pantheon-web` (new daily report form/detail/list views).
- **Depends on** `add-construction-site-and-team` (ConstructionSite, member function) and `add-equipment-and-material-registry` (Equipment, Material references).
- **New REST surface**: `POST /api/construction-sites/{siteId}/daily-reports`, `GET /api/construction-sites/{siteId}/daily-reports`, `GET /api/daily-reports/{id}`, `PATCH /api/daily-reports/{id}` (weather/hours/comments while draft), `POST /api/daily-reports/{id}/submit`, plus sub-resource endpoints per repeatable section (`workforce-entries`, `equipment-usage`, `activities`, `occurrences`, `materials-received`).
- **Non-goals (deferred to `add-daily-report-media-and-signoff`)**: photos/videos, file attachments, signatures, PDF export.
- **Non-goals (this change)**: editing a report after submission, more than one report per site per date, automatic weather data (manual entry only).
