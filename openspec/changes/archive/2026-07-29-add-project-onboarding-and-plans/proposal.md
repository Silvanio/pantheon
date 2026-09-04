## Why

Pantheon currently authenticates users (Google OAuth2 and email/password) but has no notion of a project, an administrator profile, or a subscription plan. Every logged-in user lands directly on the dashboard with nothing to work against. Before any real product functionality can be built, users need a way to either create a project (becoming its administrator) or be added to one, and the platform needs a trial/plan model to gate access once the free trial period ends.

## What Changes

- On login, if the user has zero project memberships (first access), `pantheon-web` shows an onboarding popup asking whether they want to create a new project or join an existing one.
- Choosing "join a project" while having zero memberships shows a message explaining the user has no project yet (self-service join is not supported — an administrator must add them).
- Choosing "create a project" shows a registration form (Nome do Projeto, CNPJ/CPF, Nome/Razão Social, Endereço, CEP). On submit, `pantheon-service` upserts the creator's `UserProfile` (CNPJ/CPF, legal name, address, CEP — aggregated data of the *user*, not the project), creates a `Project` (name only) in `TRIAL` status, creates a `ProjectMembership` with role `ADMIN` for the creator, and the user is taken to the dashboard.
- Trial period is 3 days from project creation. If a user logs in and their project's trial has expired with no plan confirmed, `pantheon-web` shows a plan-selection screen (Basic — up to 2 projects, Pro — up to 10 projects, Ilimitado — unlimited projects) before the dashboard is reachable. Confirming a plan (no real payment) sets the project's plan and a 1-year validity period starting at confirmation, then proceeds to the dashboard.
- When a confirmed plan's 1-year validity expires, the same plan-selection screen is shown again on next login (same handling as trial expiration).
- Plan project-count limits (Basic=2, Pro=10, Ilimitado=unbounded) are enforced per administrator when creating a new project, counted against projects that administrator owns with a confirmed plan.
- The project creator becomes that project's `ADMIN`. Admins can view/manage all projects they created and add other users to a project as `MEMBER` (no admin privileges).
- **New endpoints** on `pantheon-service`: create project, list current user's projects, get onboarding/trial/plan status, confirm a plan, add a member to a project (admin-only).

## Capabilities

### New Capabilities
(none — this extends the two existing umbrella service specs below, following this repo's convention of one spec per service)

### Modified Capabilities
- `pantheon-service`: Adds `UserProfile` (per-user CNPJ/CPF, legal name, address, CEP), `Project` (name-only, per-project trial/plan lifecycle) and `ProjectMembership` persistence, trial/plan lifecycle (3-day trial, 1-year confirmed-plan validity), plan-based project-count limits, and REST endpoints for project creation, membership, onboarding status, and plan confirmation.
- `pantheon-web`: Adds a post-login onboarding popup, a "no project yet" message state, a project creation (registration) view, a plan-selection view, and a router guard that sequences these before the dashboard based on the user's onboarding/trial/plan status.

## Impact

- **Affected code**: `pantheon-service` (new `project` package: `Project`/`ProjectMembership` entities, repositories, service, controllers; `UserProfile` entity/repository/service added to the existing `user` package; new Flyway migrations for `user_profile`, `project`, and `project_membership` tables), `pantheon-web` (new onboarding modal, `ProjectRegistrationView`, `PlanSelectionView`, router guard, `useProjectOnboarding` composable, API client additions).
- **No changes** to `pantheon-message` or `platform-foundation`.
- **New REST surface**: `POST /api/projects`, `GET /api/projects/me`, `GET /api/onboarding/status`, `POST /api/projects/{id}/plan`, `POST /api/projects/{id}/members`.
- **Non-goals**: real payment/billing integration, self-service join-by-code/link, promoting a member to admin, multiple administrators per project, mid-cycle plan changes/downgrades, and a multi-project switcher UI beyond what onboarding requires.
