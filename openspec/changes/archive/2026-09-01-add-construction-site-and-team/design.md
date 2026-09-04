## Context

`pantheon-service` today has `AppUser`, `UserProfile`, `Project`, `ProjectMembership` (role `ADMIN`/`MEMBER`). This change adds the first construction-domain entities on top of that: the physical site(s) a project manages (`ConstructionSite`), architectural design records (`ArchitecturalProject`), and a richer notion of what a member actually does on site. It is the foundation every later construction-management change (equipment/material registry, daily report, material request workflow) attaches to. It also sets two conventions the rest of the initiative follows: English identifiers in code regardless of the Brazilian-Portuguese business domain, and i18n-driven UI text in `pantheon-web`.

## Goals / Non-Goals

**Goals:**
- Model `ConstructionSite` as a first-class child of `Project` with a simple status lifecycle.
- Model `ArchitecturalProject` as a lightweight record (not a document management system).
- Let a `ProjectMembership` carry a construction function + optional specialty, separate from its platform permission role.
- Establish English-only code identifiers (entities, tables, columns, enum values, comments) as the project convention, even though the product's domain vocabulary and UI are Portuguese.
- Establish `pantheon-web`'s i18n mechanism (locale resource files, `pt-BR` as the shipped locale) so no user-facing string is hardcoded going forward.

**Non-Goals:**
- Per-site membership/visibility scoping — deferred; every project member sees every construction site.
- File/document attachments on `ArchitecturalProject` — covered generically once `add-daily-report-media-and-signoff` lands, or a future change.
- Changing or removing a member's function after it is set (no edit endpoint yet).
- Multiple functions per member (one function per membership record for now).
- Supporting more than one shipped locale — the i18n plumbing is introduced now, but only `pt-BR` strings are written; adding a second locale is future work.

## Decisions

### Entity model
```
ConstructionSite                         -- table: construction_site
  id                 UUID PK
  project_id         UUID FK project.id
  name               VARCHAR
  address            VARCHAR
  status             VARCHAR   -- PLANNING | IN_PROGRESS | PAUSED | COMPLETED
  start_date         DATE
  expected_end_date  DATE NULL
  created_by         UUID FK app_user.id
  created_at / updated_at

ArchitecturalProject                     -- table: architectural_project
  id                    UUID PK
  project_id            UUID FK project.id
  construction_site_id  UUID NULL FK construction_site.id
  name                  VARCHAR
  description           TEXT NULL
  created_by            UUID FK app_user.id
  created_at / updated_at

ProjectMembership (extended)
  ... existing columns ...
  function   VARCHAR NULL  -- CLIENT | ENGINEER | ARCHITECT | SITE_FOREMAN | SERVICE_PROVIDER | OTHER
  specialty  VARCHAR NULL  -- only meaningful when function = SERVICE_PROVIDER
```
`function` is nullable to keep existing memberships (created before this change) valid without a backfill; new memberships set it going forward, defaulting to `OTHER` if omitted by the client. Table/column names and enum values are English throughout — table `construction_site`, `architectural_project` — even though the domain concept ("Obra") and every UI label the user reads are Portuguese; the two are decoupled by the i18n layer (see below).

*Alternative considered*: a fixed enum of specialties for `SERVICE_PROVIDER` (Painter, Bricklayer, Laborer, Electrician...) — rejected because the list is open-ended (per the product owner, "e assim por diante") and a closed enum would need a migration every time a new trade shows up; free text keeps it simple, matching the "simples e objetivo" scope for this phase.

*Alternative considered*: modeling site team membership as its own join table (`construction_site_membership`) scoped per site — rejected for this change as unnecessary complexity; deferred to Non-Goals since every construction feature so far (daily report, equipment) only needs "is this person on the project", not "is this person on this specific site".

### Where ConstructionSite hangs off Project vs. becoming the tenant itself
`Project` remains the platform tenant/subscription unit (trial, plan, billing). `ConstructionSite` is a child resource — a `Project` can run several concurrent construction sites under one subscription, which matches the product intent ("projeto pode ter: obra, projetos arquitetônicos, membro").

*Alternative considered*: renaming `Project` to `ConstructionSite` and dropping the extra layer — rejected because it would break the existing trial/plan/membership model built in `add-project-onboarding-and-plans`, and because a `Project` legitimately needs to hold more than one construction site (and non-site records like `ArchitecturalProject`).

### English code, Portuguese product
The product's domain vocabulary (Obra, Diário de Obra, Mestre de Obra...) and every string the end user reads stay Portuguese, but the code that implements it — Java class names, JPA entity/table/column names, enum constants, REST paths, code comments — is written in English, matching standard engineering convention and keeping the codebase approachable regardless of who maintains it. This decouples the two: a `ConstructionSite` entity backs a UI screen titled "Obras"; a `SITE_FOREMAN` enum constant backs a dropdown option labeled "Mestre de Obra".

*Alternative considered*: naming entities/tables after the Portuguese domain terms (as the first draft of this change did — `Obra`, `obra` table, `MESTRE_DE_OBRA` enum) — rejected per explicit product direction; kept only as the UI-facing translation, sourced from the locale file below.

### Table naming convention
Every JPA entity's table name is the entity's PascalCase Java class name converted to `snake_case`, words separated by underscores — e.g. `ConstructionSite` → `construction_site`, `DailyReportActivity` → `daily_report_activity`, `ReceiptVerification` → `receipt_verification`. This is standard Hibernate/Spring Boot naming-strategy behavior (`SpringPhysicalNamingStrategy`) and applies uniformly across all five changes in this initiative; every entity model in this and the following changes' `design.md` documents is annotated with its exact table name.

### `pantheon-web` internationalization
Introduce an i18n library (Vue's standard is `vue-i18n`) with locale resource files under `src/locales/` (e.g. `pt-BR.json`), `pt-BR` set as the default and, for now, only locale. Every new user-facing string added by this change (and by every change after it) is added as a key in the locale file and referenced via the i18n composable/directive, never inlined as a literal in a `.vue` template or script.

*Alternative considered*: hardcoding Portuguese strings directly in components now, adding i18n later — rejected because retrofitting i18n across every view built by five changes is far more work than starting with the convention from change one; the cost of the initial `vue-i18n` setup is small and paid once here.

## Risks / Trade-offs

- **[Risk]** No per-site visibility scoping means a `SERVICE_PROVIDER` added for one site can see every other site's data under the same project → **Mitigation**: acceptable for this phase given the product owner's "simples e objetivo" directive; documented as a Non-Goal, revisit if a real multi-site customer needs isolation.
- **[Risk]** Free-text `specialty` allows inconsistent labels ("Pedreiro" vs "pedreiro" vs "Alvenaria") → **Mitigation**: normalize casing client-side; a controlled vocabulary can be introduced later without a breaking migration since the column is already a plain string.
- **[Risk]** Nullable `function` on old rows means some UI surfaces (e.g., a "team by function" view) must handle unknown function gracefully → **Mitigation**: treat null as `OTHER` in the UI.
- **[Risk]** Introducing i18n as a hard convention from change one adds a small amount of ceremony (locale key + file edit) to every UI task in every subsequent change → **Mitigation**: accepted deliberately — the alternative (retrofitting later) is strictly more expensive.

## Migration Plan

Additive only: new tables (`construction_site`, `architectural_project`) and two new nullable columns on `project_membership` (`function`, `specialty`) via new Flyway migrations. No existing table or endpoint is modified in place — `POST /api/projects/{id}/members` gains two new optional request fields. `pantheon-web` gains a new dependency (`vue-i18n`) and a `src/locales/pt-BR.json` file. Rollback: drop the two new tables and the two new columns; existing project/membership/plan flows are untouched; the i18n dependency can be removed if this change is fully reverted.

## Open Questions

- Should `ADMIN` be implicitly treated as a specific function (e.g., always allowed to act as `ENGINEER`-level approver in later changes), or is function purely descriptive with no permission weight of its own? This design assumes function is descriptive only; permission checks in later changes (e.g., material request approval) will need to decide explicitly whether to key off `role` or `function` — flagged there.
- Can a `Project` have zero `ConstructionSite`s indefinitely (e.g., during a pre-construction/design-only phase using only `ArchitecturalProject`)? This design assumes yes — site creation is not mandatory at project creation time.
