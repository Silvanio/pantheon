## Why

Pantheon's `Project` currently has no notion of the physical construction site(s) it manages, no architectural project records, and its `ProjectMembership` only distinguishes platform-level `ADMIN`/`MEMBER` access — it has no way to represent who a person actually is on the job (client, engineer, architect, site foreman, service provider). Every subsequent construction-management feature (daily reports, equipment, materials) needs to attach to a specific construction site and needs to know a member's on-site function, so this foundational layer must exist first. This change also establishes two project-wide engineering conventions that every later change follows: backend code (entities, tables, columns, enums, comments) is written in English regardless of the business domain being Brazilian construction, and all user-facing text in `pantheon-web` is externalized into locale resource files instead of hardcoded, so the Portuguese UI can be maintained independently of the code.

## What Changes

- Add `ConstructionSite`: belongs to a `Project`, one `Project` can have multiple sites. Fields: name, address, status (`PLANNING`, `IN_PROGRESS`, `PAUSED`, `COMPLETED`), start date, expected end date.
- Admins can create, list, and update the status of a project's `ConstructionSite`s.
- Add `ArchitecturalProject`: belongs to a `Project`, optionally linked to one `ConstructionSite`. Represents a design/blueprint record (name, description), not a file-heavy document store yet.
- Extend `ProjectMembership` with a construction **function** (`CLIENT`, `ENGINEER`, `ARCHITECT`, `SITE_FOREMAN`, `SERVICE_PROVIDER`, `OTHER`), set when a member is added, independent of the existing `ADMIN`/`MEMBER` permission role. When function is `SERVICE_PROVIDER`, a free-text `specialty` (e.g., Painter, Bricklayer, Laborer, Electrician) is also captured.
- `pantheon-web`: views to create/list `ConstructionSite`s under a project, a simple form to register `ArchitecturalProject`s, and a function/specialty selector added to the "add member" flow — all user-facing text sourced from a `pt-BR` locale resource file via an i18n library, not hardcoded.
- Establish the project-wide conventions: all backend code (classes, tables, columns, enum values, code comments) is in English; all `pantheon-web` user-facing strings go through i18n resource files with `pt-BR` as the default/only locale for now.

## Capabilities

### New Capabilities
- `construction-site-management`: ConstructionSite and ArchitecturalProject entities under a Project, and construction-function classification of project members.

### Modified Capabilities
- `pantheon-web`: adds the requirement that all user-facing text is sourced from i18n locale resource files rather than hardcoded in components.

## Impact

- **Affected code**: `pantheon-service` (new `ConstructionSite`, `ArchitecturalProject` entities/repositories/services/controllers/DTOs; `ProjectMembership` gains `function`/`specialty` columns; new Flyway migrations), `pantheon-web` (new "Construction sites" list/create views, architectural-project form, member-function selector, i18n setup with a `pt-BR` locale resource file).
- **New REST surface**: `POST /api/projects/{projectId}/construction-sites`, `GET /api/projects/{projectId}/construction-sites`, `PATCH /api/construction-sites/{id}/status`, `POST /api/projects/{projectId}/architectural-projects`, `GET /api/projects/{projectId}/architectural-projects`, extended `POST /api/projects/{id}/members` payload, and `GET /api/projects/{id}/members` (list a project's members with role/function/specialty — needed to render the member list this change's own UI requirement depends on; the only prior member-listing endpoint, `GET /api/projects/me`, lists the caller's own memberships across projects, not a given project's members).
- **Not affected**: `pantheon-message`, existing auth/plan/trial flows.
- **Non-goals**: per-site team scoping (every project member can see every construction site of that project in this phase), file attachments on `ArchitecturalProject`, editing/removing a member's function after creation, supporting more than one UI locale (the i18n mechanism is introduced now so it's structurally ready, but only `pt-BR` content is written).
