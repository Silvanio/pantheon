## 1. Frontend: i18n foundation

- [x] 1.1 Add `vue-i18n` dependency to `pantheon-web`
- [x] 1.2 Create `src/locales/pt-BR.json` and wire up the i18n plugin with `pt-BR` as default/fallback locale
- [x] 1.3 Establish the convention (lint rule or code review checklist) that no user-facing string literal is written directly in a `.vue` template/script — enforced by convention/review for this change (no lint rule added yet); verified via 7.4

## 2. Backend: ConstructionSite entity and endpoints

- [x] 2.1 Create `construction_site` table via Flyway migration (project_id, name, address, status, start_date, expected_end_date, created_by, timestamps)
- [x] 2.2 Add `ConstructionSite` entity, repository, `ConstructionSiteService`, DTOs (`ConstructionSiteResponse`, `ConstructionSiteRegistrationRequest`, `ConstructionSiteStatusUpdateRequest`)
- [x] 2.3 Add `ConstructionSiteController`: `POST /api/projects/{projectId}/construction-sites`, `GET /api/projects/{projectId}/construction-sites`, `PATCH /api/construction-sites/{id}/status`
- [x] 2.4 Enforce admin-only creation/status-update, member-readable listing
- [x] 2.5 Unit tests for `ConstructionSiteService` (creation, status transitions, permission checks)

## 3. Backend: ArchitecturalProject entity and endpoints

- [x] 3.1 Create `architectural_project` table via Flyway migration (project_id, construction_site_id nullable, name, description, created_by, timestamps)
- [x] 3.2 Add `ArchitecturalProject` entity, repository, service, DTOs
- [x] 3.3 Add controller endpoints: `POST /api/projects/{projectId}/architectural-projects`, `GET /api/projects/{projectId}/architectural-projects`
- [x] 3.4 Unit tests for creation and listing, including the optional construction site link

## 4. Backend: Member construction function

- [x] 4.1 Add nullable `function` and `specialty` columns to `project_membership` via Flyway migration
- [x] 4.2 Extend `AddMemberRequest`/`ProjectMembershipResponse` DTOs with `function`/`specialty`
- [x] 4.3 Update `ProjectService`'s add-member flow to persist function/specialty, defaulting function to `OTHER` when omitted
- [x] 4.4 Unit tests covering function set, specialty set only with `SERVICE_PROVIDER`, and default-to-`OTHER`
- [x] 4.5 (added during apply) `GET /api/projects/{id}/members` — the "project member list" assumed by tasks 6.2/6.2 didn't exist yet as an endpoint (only `GET /api/projects/me`, which lists *the caller's own* memberships across projects); added `ProjectMemberResponse` DTO, `ProjectService.listMembers`, and the controller endpoint so the UI has something real to render

## 5. Frontend: Construction site and architectural project views

- [x] 5.1 Add "Construction sites" list view under a project (Portuguese copy: "Obras"), with create form and status update control (admin only), all copy sourced from `pt-BR.json`
- [x] 5.2 Add architectural project registration form and list view, copy sourced from `pt-BR.json`
- [x] 5.3 Add API client functions for the new endpoints

## 6. Frontend: Member function selector

- [x] 6.1 Add construction function dropdown (and conditional specialty text field) to the add-member form, option labels sourced from `pt-BR.json`
- [x] 6.2 Show function/specialty in the project member list

## 7. Verification

- [x] 7.1 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes — 15/15 (ProjectServiceTest) + 6/6 (ConstructionSiteServiceTest) + 4/4 (ArchitecturalProjectServiceTest), full suite green
- [x] 7.2 Build `pantheon-web` (`npm run build`) and confirm it passes — `vue-tsc -b && vite build` clean, 0 type errors
- [x] 7.3 Manually exercise: create construction site → update status, register architectural project (with and without site link), add member with each function value — exercised end-to-end against a live `pantheon-service` (real Postgres + RabbitMQ via `infra/docker-compose.yml`): create/list/PATCH-status obra, arch. project with and without site link, add member with `SERVICE_PROVIDER`+specialty and with no function (defaults to `OTHER`), 403 confirmed for non-admin create, 200 confirmed for member read
- [x] 7.4 Grep `pantheon-web/src` for obviously hardcoded Portuguese string literals outside `src/locales/` and confirm none were introduced by this change — only pre-existing hardcoded strings in `DashboardView.vue` ("Pantheon", "Painel"), predating this change and out of its scope; the three new panel components are 100% i18n-sourced
