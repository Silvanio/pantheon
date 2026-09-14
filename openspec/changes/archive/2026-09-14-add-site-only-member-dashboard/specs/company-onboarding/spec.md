## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: User's own registration data editable from the UI
`pantheon-web` SHALL provide a page, reachable from "Editar dados do cadastro" in the header profile menu, where an authenticated user can view and edit their own CNPJ/CPF, legal name, address, and CEP, pre-filled from their existing profile when one exists and blank otherwise.

#### Scenario: User edits their existing profile
- **WHEN** a user with an existing profile opens "Editar dados do cadastro"
- **THEN** `pantheon-web` shows the form pre-filled with their current CNPJ/CPF, legal name, address, and CEP, and submitting it persists the changes

#### Scenario: User without a profile yet fills it in for the first time
- **WHEN** a user with no existing profile opens "Editar dados do cadastro"
- **THEN** `pantheon-web` shows a blank form, and submitting it creates their profile
