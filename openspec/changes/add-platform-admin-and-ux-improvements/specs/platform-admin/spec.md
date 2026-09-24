## ADDED Requirements

### Requirement: Superadmin bypasses every company and site membership gate
`pantheon-service` SHALL treat a user whose `AppUser.superAdmin` flag is set as having full access to every company and every construction site, without requiring any `CompanyMembership` or `SiteMembership` row — identical in effect to company staff wherever `SiteAccessContext.companyStaff()` already grants `MANAGE` on every `PermissionCapability`. This applies to every existing membership gate (`CompanyService.requireMembership`/`requireAdmin`, `ConstructionSiteService.requireMembership`/`requireAdmin`, `SiteAccessService.resolve`) without any of those gates being individually reimplemented for superadmin.

#### Scenario: Superadmin views a company they have no membership in
- **WHEN** a superadmin requests a company's details, its staff list, or its obras, and holds no `CompanyMembership` for that company
- **THEN** `pantheon-service` returns the requested data instead of rejecting the request

#### Scenario: Superadmin has full access inside any obra
- **WHEN** a superadmin requests any capability-gated resource of a construction site (Diário de Obra, Equipamentos, Pedido de Compra, Orçamentos, Tasks, Cronograma) and holds no `SiteMembership` on that site
- **THEN** `pantheon-service` resolves their access as `MANAGE` on every capability, the same as company staff

#### Scenario: Non-superadmin is unaffected
- **WHEN** a regular user (superAdmin flag unset or false) without a matching membership requests a company or a construction site's resources
- **THEN** `pantheon-service` rejects the request exactly as it did before this capability existed

### Requirement: Platform-wide, paginated, searchable company listing
`pantheon-service` SHALL allow a superadmin (only) to list every company on the platform, paginated and optionally filtered by a case-insensitive substring match on the company's name, via `GET /api/admin/companies`. `pantheon-service` SHALL reject the request with HTTP 403 for a non-superadmin.

#### Scenario: Superadmin lists all companies
- **WHEN** a superadmin requests the company list with no search filter
- **THEN** `pantheon-service` returns a page of every company on the platform, regardless of the superadmin's own membership in any of them

#### Scenario: Superadmin searches companies by name
- **WHEN** a superadmin requests the company list with a search term matching part of some companies' names
- **THEN** `pantheon-service` returns only the companies whose name contains that term, case-insensitively

#### Scenario: Non-superadmin cannot list all companies
- **WHEN** a regular user requests `GET /api/admin/companies`
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Superadmin company picker in the UI
`pantheon-web` SHALL provide a dedicated view (`/admin`) for a superadmin, reached automatically in place of the normal dashboard immediately after login. The view SHALL present a filter control matching the Global Tasks Board's obra-filter pattern: a button (showing "Selecionar empresa" or the selected company's name) opening a popover with a search input and up to 10 matching companies, querying `pantheon-service`'s paginated/searchable company listing as the superadmin types (3 or more characters triggers a name-filtered query; fewer than 3 shows the first 10 companies). Selecting a company SHALL show that company's obras as cards using the same visual design as the normal dashboard's obra cards (photo, start date, progress bar, status badge), each opening the normal obra detail view on click, plus a link to that company's settings screen. This view SHALL replace, not supplement, the normal company-onboarding flow for a superadmin, who belongs to no company of their own.

#### Scenario: Superadmin lands on the company picker after login
- **WHEN** a superadmin logs in
- **THEN** `pantheon-web` shows the company picker instead of the normal dashboard or company-onboarding flow

#### Scenario: Selecting a company shows its obras as dashboard-style cards and a settings link
- **WHEN** a superadmin selects a company from the filter popover
- **THEN** `pantheon-web` shows that company's obras as cards matching the dashboard's obra-card design, each opening the normal obra detail view, and a link to that company's settings screen

#### Scenario: Superadmin searches for a company
- **WHEN** a superadmin types 3 or more characters of a company's name into the filter's search field
- **THEN** `pantheon-web` queries `pantheon-service` for companies whose name contains that text and shows up to 10 matches

#### Scenario: Typing fewer than 3 characters does not filter
- **WHEN** a superadmin types 1 or 2 characters into the filter's search field
- **THEN** `pantheon-web` continues showing the first 10 companies unfiltered
