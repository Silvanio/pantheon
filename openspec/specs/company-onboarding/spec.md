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
`pantheon-web` SHALL gate access to the dashboard and other protected routes on the onboarding status of the companies the user administers or belongs to, redirecting in order: company creation (no company yet), plan selection (`PLAN_PENDING`), company profile completion (`PROFILE_PENDING`), then the dashboard (`COMPLETE`). A user with no company membership at all (a site-only member — client, architect, engineer, site foreman, or service provider) SHALL NOT be routed through company onboarding; if they have exactly one obra, they SHALL be sent directly into it; if they have more than one, they SHALL be sent to the dashboard instead of an arbitrary one.

#### Scenario: User with no company redirected to creation
- **WHEN** a user with no company membership and no site membership attempts to reach a protected route
- **THEN** `pantheon-web` redirects to the company creation form

#### Scenario: Fully onboarded user reaches the dashboard
- **WHEN** an authenticated user whose company's onboarding status is `COMPLETE` navigates to a protected route
- **THEN** `pantheon-web` renders the requested route without redirecting to any onboarding step

#### Scenario: Site-only member with one obra enters it directly
- **WHEN** a user with no company membership and exactly one active site membership attempts to reach the dashboard
- **THEN** `pantheon-web` redirects directly into that construction site instead of showing the dashboard

#### Scenario: Site-only member with multiple obras sees the dashboard
- **WHEN** a user with no company membership and more than one active site membership attempts to reach the dashboard
- **THEN** `pantheon-web` renders the dashboard, listing every obra that user has access to

### Requirement: Dashboard obra cards
`pantheon-web` SHALL render the dashboard as a grid of cards, one per construction site (obra) the user has access to, each showing the site's photo, name, and start date. For an administrator or company staff member, the obras belong to their one active company. For a site-only member with access to more than one obra, the obras are gathered across every company they have site membership in, and each card additionally shows a badge with that obra's own company's logo and name.

#### Scenario: Dashboard lists the company's obras
- **WHEN** a fully onboarded user opens the dashboard
- **THEN** `pantheon-web` displays one card per construction site belonging to their company, showing its photo, name, and start date

#### Scenario: Obra card opens the site
- **WHEN** a user clicks an obra card on the dashboard
- **THEN** `pantheon-web` navigates into that construction site's menu (team, diário de obra, projetos, materiais, cronograma)

#### Scenario: Site-only member's dashboard spans multiple companies
- **WHEN** a site-only member with obras in more than one company opens the dashboard
- **THEN** `pantheon-web` shows one card per obra across all of those companies, each labeled with a badge naming its own company

### Requirement: Header profile menu
`pantheon-web` SHALL provide a header menu, accessible from any authenticated view, offering access to change the company's plan and to edit the company's profile data. For a site-only member (no company membership), the menu instead offers only to sign out and to edit their own registration data — the plan and company-profile items, which don't apply to them, are not shown.

#### Scenario: Admin changes plan from the header
- **WHEN** the administrator opens the header profile menu and selects "Alterar plano"
- **THEN** `pantheon-web` presents the plan-selection screen pre-filled with the current plan

#### Scenario: Admin edits company data from the header
- **WHEN** the administrator opens the header profile menu and selects "Editar dados da empresa"
- **THEN** `pantheon-web` presents the company profile form pre-filled with its current data for editing

#### Scenario: Site-only member sees a reduced menu
- **WHEN** a site-only member (no company membership) opens the header profile menu
- **THEN** `pantheon-web` offers only "Sair" and "Editar dados do cadastro", with no plan or company-profile options

### Requirement: User's own registration data editable from the UI
`pantheon-web` SHALL provide a page, reachable from "Editar dados do cadastro" in the header profile menu, where an authenticated user can view and edit their own CNPJ/CPF, legal name, address, and CEP, pre-filled from their existing profile when one exists and blank otherwise.

#### Scenario: User edits their existing profile
- **WHEN** a user with an existing profile opens "Editar dados do cadastro"
- **THEN** `pantheon-web` shows the form pre-filled with their current CNPJ/CPF, legal name, address, and CEP, and submitting it persists the changes

#### Scenario: User without a profile yet fills it in for the first time
- **WHEN** a user with no existing profile opens "Editar dados do cadastro"
- **THEN** `pantheon-web` shows a blank form, and submitting it creates their profile

### Requirement: Dashboard header branding
`pantheon-web` SHALL show the active company's own logo and name in the dashboard header once a company is active, positioned flush against the header's left edge; before any company is active (e.g. during onboarding), it SHALL fall back to the generic Pantheon brand mark. The theme toggle SHALL be the rightmost control in the header, positioned after the profile menu.

#### Scenario: Company branding shown once a company is active
- **WHEN** a user with an active company opens the dashboard
- **THEN** `pantheon-web` shows that company's logo and name in the header instead of the generic Pantheon brand

#### Scenario: Generic brand shown before any company is active
- **WHEN** a user with no active company (e.g. mid-onboarding) views a header
- **THEN** `pantheon-web` shows the generic Pantheon brand mark

