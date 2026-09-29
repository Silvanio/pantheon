## MODIFIED Requirements

### Requirement: Approval and status UI
`pantheon-web` and `pantheon-mobile` SHALL show, on a Diário de Obra whose status is `PENDING_APPROVAL`, its current approval step and an approve/reject action to members whose resolved `DAILY_REPORT` access is `MANAGE` or `VIEW_AND_APPROVE`, with a required-reason prompt on reject; every approval step SHALL show the display name of the member who decided it, not just its status. `pantheon-web` SHALL additionally provide, under the construction site's permissions/settings area, a screen for configuring the site's Diário de Obra approval levels (mirroring the equivalent Pedido de Compra screen) — this configuration screen is web-only. This approval history is the report's closing section, replacing the removed sign-off UI (see `daily-report-media-and-signoff`).

#### Scenario: Approver acts on a pending report from the UI
- **WHEN** a member with `VIEW_AND_APPROVE` `DAILY_REPORT` access opens a `PENDING_APPROVAL` report matching their function's step
- **THEN** the app shows an approve action and a reject action (the latter requiring a reason) for that step

#### Scenario: Decided step shows who decided it
- **WHEN** an approval step has been approved or rejected
- **THEN** the app shows the display name of the `SiteMembership` that decided it, alongside the decision and timestamp

#### Scenario: Admin configures approval levels from the web UI
- **WHEN** a company staff member opens a construction site's Diário de Obra approval-levels configuration screen
- **THEN** `pantheon-web` shows the current ordered levels and allows adding, reordering, or removing them
