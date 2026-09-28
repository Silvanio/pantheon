## ADDED Requirements

### Requirement: Per-site Diário de Obra approval levels
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to configure an ordered list of Diário de Obra approval levels for that site, each specifying a step order and a required construction-site function. A site with no configured levels SHALL default to a single implicit level requiring the `ENGINEER` function.

#### Scenario: Admin configures a two-step chain
- **WHEN** a company staff member configures a site's Diário de Obra approval levels as step 1 = `SITE_FOREMAN`, step 2 = `ENGINEER`
- **THEN** `pantheon-service` persists both levels in that order for that site

#### Scenario: Unconfigured site defaults to a single level
- **WHEN** a Diário de Obra is submitted for approval on a site with no configured approval levels
- **THEN** `pantheon-service` creates a single approval step requiring the `ENGINEER` function

### Requirement: Acting on a Diário de Obra approval step
`pantheon-service` SHALL allow a user to approve or reject the current cycle's lowest-order `PENDING` `DailyReportApproval` step only when both hold: (1) either the user holds no active `SiteMembership` on that construction site (company staff acting with no site role of their own), or the user holds an active `SiteMembership` on that site whose function matches the step's function — this function match is required with no exception, even for company staff who also hold a `SiteMembership` there; and (2) the user's resolved `DAILY_REPORT` access level is `MANAGE` or `VIEW_AND_APPROVE` (never `VIEW` or `HIDDEN`). Approving the final step SHALL transition the Diário de Obra to `APPROVED`; approving a non-final step SHALL activate the next step. Rejecting SHALL require a reason, mark that step `REJECTED`, record the reason, and transition the report back to `DRAFT`. When the acting user has an active `SiteMembership` on the site, the decision SHALL be attributed to that `SiteMembership`, even if the user is also company staff.

#### Scenario: Intermediate step approved
- **WHEN** the member matching a two-step chain's first step approves it
- **THEN** `pantheon-service` marks that step `APPROVED` and the Diário de Obra remains pending approval with its second step now actionable

#### Scenario: Final step approved
- **WHEN** the member matching a chain's last remaining `PENDING` step approves it
- **THEN** `pantheon-service` marks that step `APPROVED` and transitions the Diário de Obra to `APPROVED`

#### Scenario: Step rejected returns Diário de Obra to Draft
- **WHEN** the member matching the current pending step rejects it, supplying a reason
- **THEN** `pantheon-service` marks that step `REJECTED`, records the reason, and transitions the report to `DRAFT`

#### Scenario: Non-matching member blocked
- **WHEN** a construction site member whose function does not match the current pending step's function, and who is not company staff, attempts to approve or reject it
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Company staff with a matching site role decides as that role
- **WHEN** a user who has an active `SiteMembership` with function `ENGINEER` on a site, and who is also company staff for that site's company, approves the current pending step and that step requires the `ENGINEER` function
- **THEN** `pantheon-service` allows the approval and attributes it to that user's `ENGINEER` `SiteMembership`

#### Scenario: Company staff with no site role at all retains the approval bypass
- **WHEN** a user who is company staff for a site's company, and has no active `SiteMembership` on that specific site, approves the current pending step for any function
- **THEN** `pantheon-service` allows the approval and attributes it to no specific `SiteMembership`

#### Scenario: View-only access blocks approval even on a function match
- **WHEN** a construction site member whose function matches the current pending step, but whose resolved `DAILY_REPORT` access is `VIEW`, attempts to approve or reject it
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Approval and status UI
`pantheon-web` and `pantheon-mobile` SHALL show, on a Diário de Obra whose status is `PENDING_APPROVAL`, its current approval step and an approve/reject action to members whose resolved `DAILY_REPORT` access is `MANAGE` or `VIEW_AND_APPROVE`, with a required-reason prompt on reject; `pantheon-web` SHALL additionally provide, under the construction site's permissions/settings area, a screen for configuring the site's Diário de Obra approval levels (mirroring the equivalent Pedido de Compra screen) — this configuration screen is web-only.

#### Scenario: Approver acts on a pending report from the UI
- **WHEN** a member with `VIEW_AND_APPROVE` `DAILY_REPORT` access opens a `PENDING_APPROVAL` report matching their function's step
- **THEN** the app shows an approve action and a reject action (the latter requiring a reason) for that step

#### Scenario: Admin configures approval levels from the web UI
- **WHEN** a company staff member opens a construction site's Diário de Obra approval-levels configuration screen
- **THEN** `pantheon-web` shows the current ordered levels and allows adding, reordering, or removing them
