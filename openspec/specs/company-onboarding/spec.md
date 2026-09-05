# company-onboarding Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Company creation is name-only
`pantheon-web` SHALL, immediately after a user with no company completes signup (email/password or Google), present a form collecting only the company's name and submit it to `pantheon-service` to create the `Company` with the user as its `ADMIN`.

#### Scenario: New user creates a company
- **WHEN** a user with no company membership completes signup and submits a company name
- **THEN** `pantheon-web` creates the company via `pantheon-service` and proceeds to mandatory plan selection

### Requirement: Mandatory plan selection
`pantheon-web` SHALL present the plan-selection modal (Basic, Profissional, Ilimitado) immediately after company creation and SHALL NOT allow navigation to the company profile form or the dashboard until a plan has been selected.

#### Scenario: Plan modal blocks navigation
- **WHEN** an administrator of a company with no plan selected attempts to navigate away from the plan-selection modal without choosing a plan
- **THEN** `pantheon-web` keeps the modal open and does not navigate to the profile form or dashboard

#### Scenario: Plan chosen proceeds to profile
- **WHEN** an administrator selects a plan in the modal and confirms
- **THEN** `pantheon-web` submits the selection to `pantheon-service` and navigates to the company profile completion form

### Requirement: Company profile completion
`pantheon-service` SHALL allow the administrator of a company with a plan selected but an incomplete profile to submit its Razão Social (legal name), Nome Fantasia (trade name), CNPJ, address, and logo image, storing the logo in object storage. `pantheon-web` SHALL present this as a required step before the dashboard is reachable for the first time.

#### Scenario: Admin completes the company profile
- **WHEN** the administrator of a company with a plan but no complete profile submits legal name, trade name, CNPJ, address, and a logo image
- **THEN** `pantheon-service` persists the company's profile fields and stores the logo in object storage, and `pantheon-web` navigates to the dashboard

#### Scenario: Incomplete profile blocks the dashboard
- **WHEN** the administrator of a company with a plan but a missing profile field attempts to reach the dashboard
- **THEN** `pantheon-web` redirects to the company profile completion form instead

### Requirement: Onboarding status computation
`pantheon-service` SHALL expose an authenticated endpoint reporting, for each company the current user administers, whether it is `PLAN_PENDING` (no plan selected), `PROFILE_PENDING` (plan selected but profile incomplete), or `COMPLETE`.

#### Scenario: Plan pending reported
- **WHEN** an authenticated user administers a company with no plan selected
- **THEN** `pantheon-service` reports that company's onboarding status as `PLAN_PENDING`

#### Scenario: Profile pending reported
- **WHEN** an authenticated user administers a company with a plan selected but missing profile fields
- **THEN** `pantheon-service` reports that company's onboarding status as `PROFILE_PENDING`

#### Scenario: Complete reported
- **WHEN** an authenticated user administers a company with a plan selected and a complete profile
- **THEN** `pantheon-service` reports that company's onboarding status as `COMPLETE`

### Requirement: Post-login routing guard order
`pantheon-web` SHALL gate access to the dashboard and other protected routes on the onboarding status of the companies the user administers or belongs to, redirecting in order: company creation (no company yet), plan selection (`PLAN_PENDING`), company profile completion (`PROFILE_PENDING`), then the dashboard (`COMPLETE`).

#### Scenario: User with no company redirected to creation
- **WHEN** a user with no company membership and no pending site invitation attempts to reach a protected route
- **THEN** `pantheon-web` redirects to the company creation form

#### Scenario: Fully onboarded user reaches the dashboard
- **WHEN** an authenticated user whose company's onboarding status is `COMPLETE` navigates to a protected route
- **THEN** `pantheon-web` renders the requested route without redirecting to any onboarding step

### Requirement: Dashboard obra cards
`pantheon-web` SHALL render the dashboard as a grid of cards, one per construction site (obra) belonging to the user's company, each showing the site's photo, name, and start date.

#### Scenario: Dashboard lists the company's obras
- **WHEN** a fully onboarded user opens the dashboard
- **THEN** `pantheon-web` displays one card per construction site belonging to their company, showing its photo, name, and start date

#### Scenario: Obra card opens the site
- **WHEN** a user clicks an obra card on the dashboard
- **THEN** `pantheon-web` navigates into that construction site's menu (team, diário de obra, projetos, materiais, cronograma)

### Requirement: Header profile menu
`pantheon-web` SHALL provide a header menu, accessible from any authenticated view, offering access to change the company's plan and to edit the company's profile data.

#### Scenario: Admin changes plan from the header
- **WHEN** the administrator opens the header profile menu and selects "Alterar plano"
- **THEN** `pantheon-web` presents the plan-selection screen pre-filled with the current plan

#### Scenario: Admin edits company data from the header
- **WHEN** the administrator opens the header profile menu and selects "Editar dados da empresa"
- **THEN** `pantheon-web` presents the company profile form pre-filled with its current data for editing

