## 1. Backend: Daily report core

- [x] 1.1 Create `daily_report` table via Flyway migration (construction_site_id, report_date, sequence_no, status, weather fields, work hour fields, comments, created_by, submitted_at, timestamps, unique (construction_site_id, report_date))
- [x] 1.2 Add `DailyReport` entity, repository, service (create, get, list, submit), DTOs
- [x] 1.3 Add `DailyReportController`: `POST /api/construction-sites/{siteId}/daily-reports`, `GET /api/construction-sites/{siteId}/daily-reports`, `GET /api/daily-reports/{id}`, `POST /api/daily-reports/{id}/submit`
- [x] 1.4 Enforce draft-only edits and SUBMITTED immutability at the service layer
- [x] 1.5 Unit tests: creation, duplicate-date rejection, submit transition, edit-after-submit rejection

## 2. Backend: Section child entities

- [x] 2.1 Create `daily_report_workforce_entry`, `daily_report_equipment_usage`, `daily_report_activity`, `daily_report_occurrence`, `daily_report_material_received` tables via Flyway migrations
- [x] 2.2 Add entities/repositories for each section
- [x] 2.3 Extend `DailyReportService`/DTOs with add/list operations for each section (weather + hours + comments live on `DailyReport` itself, already covered in 1.2)
- [x] 2.4 Extend `DailyReportController` with `PATCH` (weather/hours/comments) and `POST`/`GET` sub-resource endpoints for each repeatable section
- [x] 2.5 Pre-fill workforce entry's role description from the linked `ProjectMembership` when provided, allowing override
- [x] 2.6 Unit tests for each section: add entry, list entries, reject edits on a submitted report — 20/20 green (`DailyReportServiceTest`)

## 3. Frontend: Daily report form and detail

- [x] 3.1 Add daily report creation flow (pick site + date) and draft-edit view with all eight sections, copy sourced from `pt-BR.json` — `DailyReportDetailView.vue`; workforce entry offers both a registered-member picker (project id threaded through as a route query param from `ConstructionSitesPanel` → history → detail) and free-text role/headcount for casual labor. Along the way, fixed `ProjectMemberResponse` (from `add-construction-site-and-team`) to expose the `ProjectMembership` row's own id — it previously only exposed `userId`, which isn't what a workforce entry links to
- [x] 3.2 Add submit action and read-only rendering once `SUBMITTED`
- [x] 3.3 Add API client functions for the daily report endpoints (core + each section) — new `useDailyReports.ts` composable

## 4. Frontend: Report history

- [x] 4.1 Add per-site report history/list view, ordered by date — `DailyReportHistoryView.vue`, linked from each site's expanded section in `ConstructionSitesPanel.vue`
- [x] 4.2 Wire list entries to the read-only/detail view

## 5. Verification

- [x] 5.1 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes — full suite green (`DailyReportServiceTest` 20/20, plus every prior suite)
- [x] 5.2 Build `pantheon-web` (`npm run build`) and confirm it passes — clean, 0 type errors
- [x] 5.3 Manually exercise: create a report, fill every section, submit, confirm edits are rejected afterward, browse history — exercised end-to-end against a live `pantheon-service`: created a report, hit the duplicate-date 409, filled all 8 sections (weather/hours/comments via core PATCH, workforce, equipment usage, activity, occurrence, material received), fetched the full detail, submitted, confirmed double-submit and every edit endpoint (core PATCH, add-occurrence) now reject with 409, and confirmed history listing reflects `SUBMITTED` status
