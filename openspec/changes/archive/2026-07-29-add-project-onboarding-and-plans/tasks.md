## 1. Database migrations (pantheon-service)

- [x] 1.1 Add `V2__create_user_profile_table.sql` (id, user_id FK app_user unique, cnpj_cpf, legal_name, address, postal_code, created_at, updated_at)
- [x] 1.2 Add `V3__create_project_table.sql` (id, name, plan nullable, trial_started_at, trial_expires_at, plan_confirmed_at nullable, plan_valid_until nullable, created_by FK app_user, created_at, updated_at)
- [x] 1.3 Add `V4__create_project_membership_table.sql` (id, project_id FK, user_id FK app_user, role, created_at, unique constraint on (project_id, user_id))

## 2. Domain model (pantheon-service)

- [x] 2.1 Create `UserProfile` JPA entity (cnpj_cpf, legal_name, address, postal_code — one per user) and `UserProfileRepository`
- [x] 2.2 Create `UserProfileService.upsert(...)` so registration data is attached to the user, created or updated in place on each project creation
- [x] 2.3 Create `Project` JPA entity (id, name, plan, trial/plan timestamps, created_by) and `ProjectRepository` — no company data on `Project` itself
- [x] 2.4 Create `ProjectMembership` JPA entity (with `ProjectRole` enum: `ADMIN`, `MEMBER`) and `ProjectMembershipRepository`
- [x] 2.5 Create `Plan` enum (`BASIC` limit 2, `PRO` limit 10, `UNLIMITED` no limit)
- [x] 2.6 Implement project active/expired derivation logic (trial window or confirmed-plan window) as an `isActive(Instant)` method on `Project`, used by both the status endpoint and route protection

## 3. Project service and endpoints (pantheon-service)

- [x] 3.1 Implement `ProjectService.create(userId, registrationData)`: upserts the creator's `UserProfile` (CNPJ/CPF, legal name, address, CEP), persists a `Project` (name only) in trial status, creates a `ProjectMembership` with role `ADMIN`, enforcing the plan project-count limit against the creator's existing projects
- [x] 3.2 Implement `POST /api/projects` controller endpoint (authenticated) using `ProjectService.create`
- [x] 3.3 Implement `GET /api/projects/me`: list the authenticated user's projects with their role on each
- [x] 3.4 Implement `GET /api/onboarding/status`: returns `hasProject`, `activeProject` (id + role) or null, `needsPlanSelection`, `expiredProjectId`
- [x] 3.5 Implement `POST /api/projects/{id}/plan`: admin-only, sets `plan`, `plan_confirmed_at = now()`, `plan_valid_until = now() + 1 year`; reject with 403 if caller is not that project's admin
- [x] 3.6 Implement `POST /api/projects/{id}/members`: admin-only, creates a `ProjectMembership` with role `MEMBER` for the given (by email) registered user; reject with 403 if caller is not that project's admin
- [x] 3.7 Add request/response DTOs and validation for all endpoints above (project name, CNPJ/CPF, legal name, address, CEP required on create; plan must be one of the 3 valid values on confirm)

## 4. Backend tests (pantheon-service)

- [x] 4.1 Unit test: project creation sets trial window correctly and creates an `ADMIN` membership
- [x] 4.2 Unit test: project creation blocked when admin's same-plan project count would exceed the plan limit
- [x] 4.3 Unit test: onboarding status reports no-project, active-project, and needs-plan-selection cases correctly
- [x] 4.4 Unit test: plan confirmation sets a 1-year validity window and is rejected for non-admins
- [x] 4.5 Unit test: adding a member creates a `MEMBER` membership and is rejected for non-admins
- [x] 4.6 Unit test: `UserProfileService.upsert` creates a profile when none exists and updates the existing one in place otherwise

## 5. API client (pantheon-web)

- [x] 5.1 Add `useProjects` (or extend `lib/config.ts` client) with calls for create project, list my projects, get onboarding status, confirm plan, add member
- [x] 5.2 Create `useProjectOnboarding` composable caching the onboarding status for the session, with a method to invalidate/refetch after project creation or plan confirmation

## 6. Onboarding UI (pantheon-web)

- [x] 6.1 Build onboarding popup component (choices: "Create a new project" / "Join a project")
- [x] 6.2 Build "no project yet" message view/state for the join-project choice
- [x] 6.3 Build `ProjectRegistrationView` form (Nome do Projeto, CNPJ/CPF, Nome/Razão Social, Endereço, CEP) that submits to `POST /api/projects` and routes to dashboard on success
- [x] 6.4 Build `PlanSelectionView` (Basic / Pro / Ilimitado cards, "Concluído" button) that submits to `POST /api/projects/{id}/plan` and routes to dashboard on success

## 7. Routing and guard (pantheon-web)

- [x] 7.1 Add routes for the onboarding popup/message, `ProjectRegistrationView`, and `PlanSelectionView`
- [x] 7.2 Add a `router.beforeEach` guard that, for authenticated navigations to protected routes, checks `useProjectOnboarding` status and redirects to the correct onboarding step when the user has no active project
- [x] 7.3 Wire the guard to run right after login/OAuth callback so the popup appears before the dashboard is ever rendered

## 8. Frontend verification

- [x] 8.1 Manually verify: fresh user login → onboarding popup → create project → lands on dashboard
- [x] 8.2 Manually verify: fresh user login → onboarding popup → join project (no projects) → sees "no project yet" message
- [x] 8.3 Manually verify: user with an expired trial project logs in → plan selection screen → confirm plan → lands on dashboard
- [x] 8.4 Manually verify: user with an active project logs in → skips onboarding entirely → lands on dashboard
