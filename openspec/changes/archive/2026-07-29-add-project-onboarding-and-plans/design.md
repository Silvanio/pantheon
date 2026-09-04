## Context

`pantheon-service` currently has one domain entity (`AppUser`) and unified JWT auth (OAuth2 Google + email/password). `pantheon-web` has a login flow (`LoginView`, `OAuthCallbackView`) that lands directly on `DashboardView` after `useAuth().setToken()`. Neither service has any concept of a project, membership, role, or subscription. This change adds a project/tenant model with a trial-then-plan lifecycle, gated by a post-login onboarding sequence in the SPA.

## Goals / Non-Goals

**Goals:**
- Model `Project` (the tenant, carrying company registration data) and `ProjectMembership` (user↔project with a role) in `pantheon-service`.
- Drive a deterministic post-login sequence in `pantheon-web`: onboarding popup (0 projects) → registration or "no project" message, or plan-selection screen (trial/plan expired) → dashboard.
- Implement the 3-day trial and 1-year confirmed-plan validity windows as plain timestamp checks (no scheduler/cron needed — expiration is evaluated lazily on login/status-check).
- Enforce plan project-count limits at project-creation time.

**Non-Goals:**
- Real payment/billing (no gateway integration; "confirm plan" is a direct write).
- Self-service join-by-code/link.
- Promoting a `MEMBER` to `ADMIN`, multiple admins per project, mid-cycle plan changes/downgrades.
- A multi-project switcher UI — if a user has multiple projects, this change only needs *any one* non-expired project to unlock the dashboard (see Open Questions).

## Decisions

### Entity model
CNPJ/CPF, legal name (Nome/Razão Social), address, and CEP describe the **user**, not the project — they are aggregated registration data collected once (as `UserProfile`, 1:1 with `AppUser`) and reused across every project that user creates. `Project` itself carries only a `name` plus its trial/plan lifecycle fields.
```
UserProfile
  id           UUID (PK)
  user_id      UUID (FK app_user.id, UNIQUE)
  cnpj_cpf     VARCHAR
  legal_name   VARCHAR   -- Nome/Razão Social
  address      VARCHAR
  postal_code  VARCHAR   -- CEP
  created_at / updated_at

Project
  id                UUID (PK)
  name               VARCHAR
  plan               VARCHAR NULL  -- BASIC | PRO | UNLIMITED, null while in trial
  trial_started_at   TIMESTAMPTZ
  trial_expires_at   TIMESTAMPTZ   -- trial_started_at + 3 days
  plan_confirmed_at  TIMESTAMPTZ NULL
  plan_valid_until   TIMESTAMPTZ NULL  -- plan_confirmed_at + 1 year
  created_by         UUID (FK app_user.id)
  created_at / updated_at

ProjectMembership
  id          UUID (PK)
  project_id  UUID (FK project.id)
  user_id     UUID (FK app_user.id)
  role        VARCHAR  -- ADMIN | MEMBER
  created_at
  UNIQUE (project_id, user_id)
```
The project registration form collects both the project's name and the user's CNPJ/CPF/legal name/address/CEP in one submission: `ProjectService.create` upserts the caller's `UserProfile` (creating it on first use, overwriting it on later project creations — see Open Questions) and creates the `Project` with only the name. A project is "active" (unlocks the dashboard) when `now() < trial_expires_at` (still in trial) OR (`plan IS NOT NULL AND now() < plan_valid_until`). Both trial and confirmed-plan expiry reuse the same "needs plan selection" screen, so no separate status enum is needed beyond the nullable `plan` + two timestamp pairs.
*Alternative considered*: attaching CNPJ/CPF/address directly to `Project` (the original draft of this design) — rejected: those fields identify the person/company registering, not the project, and duplicating them on every project the same admin creates would drift out of sync. Modeling them on `UserProfile` keeps a single source of truth per user.
*Alternative considered*: a single `status` enum (`TRIAL`/`ACTIVE`/`EXPIRED`) recomputed on write — rejected because it requires a background job to transition state on the clock; deriving status from timestamps at read time keeps it stateless and correct without a scheduler.

### Onboarding status endpoint
`GET /api/onboarding/status` (authenticated) returns the single source of truth the SPA needs to route the user:
```json
{
  "hasProject": boolean,
  "activeProject": { "id": ..., "role": "ADMIN"|"MEMBER" } | null,
  "needsPlanSelection": boolean,      // user has a project but all owned/joined projects are expired
  "expiredProjectId": uuid | null
}
```
`pantheon-web` calls this once right after login/OAuth callback and before entering the dashboard route, rather than each view independently re-deriving state from a list of projects. This keeps the branching logic (popup vs. message vs. registration vs. plan-selection vs. dashboard) in one router guard.
*Alternative considered*: have the SPA fetch `GET /api/projects/me` and compute state client-side — rejected because expiry/plan-limit logic would then live in two places (backend enforcement + frontend routing) and could drift; the backend stays the single authority.

### Plan limits enforcement
Enforced server-side in `POST /api/projects`: count `Project` rows where `created_by = currentUser` and `plan` is one of `BASIC`/`PRO`/`UNLIMITED` (confirmed, non-expired ones — expired-plan projects still count against the limit since re-confirming reactivates them rather than freeing a slot). Compare against the limit for the *lowest* plan among the admin's projects if they have projects on different plans — simplification: since this change has no per-project independent billing, assume all of one admin's projects should be considered under the same limit check using each project's own plan (a Basic-plan project's existence doesn't cap a separate Pro-plan project). Trial projects (no plan yet) don't count against any limit, since the limit is a plan attribute.
*Alternative considered*: a single account-level plan shared across all projects — rejected because the proposal frames the limit per project ("basic: até 2 projetos"), implying the cap is evaluated per admin against the count of projects on that same plan tier they've created.

### Router guard placement (`pantheon-web`)
A navigation guard (`router.beforeEach`) runs after `isAuthenticated` is true: calls `GET /api/onboarding/status` (cached in a `useProjectOnboarding` composable for the session, invalidated after project creation/plan confirmation) and redirects to `/onboarding`, `/onboarding/no-project`, `/projects/new`, or `/plans` as needed; only lets `/dashboard` (and other protected routes) through when the status is clean.
*Alternative considered*: guard logic inline in `DashboardView.onMounted` — rejected because it would only protect that one view; a router guard protects all current and future protected routes uniformly.

## Risks / Trade-offs

- **[Risk]** Deriving trial/plan expiry from timestamps at read time means a project can be "expired" for a while before anyone notices (no proactive notification) → **Mitigation**: acceptable since the only consequence is the plan-selection screen on next login; no scheduled job needed for this change's scope.
- **[Risk]** No admin transfer / multi-admin means a departing admin leaves a project unmanageable → **Mitigation**: explicitly out of scope; documented as a known gap, not silently ignored.
- **[Risk]** `needsPlanSelection` semantics get ambiguous if a user belongs to multiple projects in different states (e.g., admin of an expired one, member of an active one) → see Open Questions; resolved by picking the first non-expired membership to unlock the dashboard, otherwise showing plan-selection scoped to the caller's own expired project(s) they administer.

## Migration Plan

Additive only — new tables (`user_profile`, `project`, `project_membership`) via new Flyway migrations (`V2__create_user_profile_table.sql`, `V3__create_project_table.sql`, `V4__create_project_membership_table.sql`), new endpoints, new frontend routes/views. No existing table or endpoint is modified. Rollback is dropping the three new tables and the new routes/controllers; `app_user` and existing auth flows are untouched.

## Open Questions

- `UserProfile.upsert` overwrites CNPJ/CPF/legal name/address/CEP in place on every project creation, so a user's most recent project-creation submission wins for all their projects (there's only one profile per user, not one per project). Confirm this matches intent, versus e.g. locking the profile after first creation or versioning it.
- If a user is `MEMBER` on one active project and `ADMIN` on a separate expired one, does the plan-selection screen still interrupt login? This design assumes **no** — any single active membership unlocks the dashboard; plan-selection only blocks when *none* of the user's memberships are active. Confirm this matches intent before implementation.
- What happens to a `MEMBER`'s access when the project they belong to expires and the admin hasn't re-confirmed a plan yet — do members also see the (read-only, non-actionable) plan-selection screen, or a distinct "ask your admin" message? This design defaults to showing members a simple "this project's plan has expired, contact your administrator" message rather than the plan-selection screen (since only the admin can act on it).
- Whether an expired-plan project that gets re-confirmed keeps its original `Project` row (this design assumes yes — `plan_confirmed_at`/`plan_valid_until` are simply overwritten) rather than creating a new plan-history record. No plan history/audit trail is in scope for this change.
