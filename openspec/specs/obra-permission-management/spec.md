# obra-permission-management Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`, `TEAM_MANAGE`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`, and `TEAM_MANAGE`, and `VIEW` on `EQUIPMENT`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT`, `DAILY_REPORT`, and `TASKS`, and `VIEW` on `DOCUMENT_PROJECTS`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, and `TEAM_MANAGE`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `TASKS`, and `TEAM_MANAGE`, with no manage access on any material/orçamento capability; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT`, `TASKS`, and `TEAM_MANAGE` only. None of these defaults is ever `HIDDEN` — a member only ends up hidden from a capability via an explicit function- or member-level override. (Authority to act on a specific Orçamento approval step is a separate, structural concern — see `orcamento-approval-workflow` — not one of these capabilities.)

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client has no material access by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `PURCHASE_REQUEST`
- **THEN** `pantheon-service` reports that the client cannot create purchase-request items

#### Scenario: Service provider can view but not manage tasks by default
- **WHEN** a construction site with no permission overrides is queried for a service provider's access to `TASKS`
- **THEN** `pantheon-service` reports `VIEW`, allowing the service provider to see the task board without creating or moving cards

### Requirement: Function-level override
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to set a function-level override, to `VIEW`, `MANAGE`, or `HIDDEN`, replacing the default access level for every `SiteMembership` of that function on that site.

#### Scenario: Admin restricts architects to view-only
- **WHEN** a company staff member sets the `DOCUMENT_PROJECTS` access level for function `ARCHITECT` on a site to `VIEW`
- **THEN** `pantheon-service` applies `VIEW` access to every architect on that site who has no member-specific override

#### Scenario: Admin hides a capability from a function
- **WHEN** a company staff member sets a capability's access level for a function on a site to `HIDDEN`
- **THEN** `pantheon-service` applies `HIDDEN` access to every member of that function on that site who has no member-specific override, and `pantheon-web` no longer shows that capability's tab to them

### Requirement: Member-level override
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to set an override, to `VIEW`, `MANAGE`, or `HIDDEN`, for one specific `SiteMembership`, which SHALL take precedence over that site's function-level default or override for that member alone.

#### Scenario: One engineer granted an exception
- **WHEN** a company staff member sets a member-specific `DAILY_REPORT` override to `MANAGE` for one client on a site where clients default to `VIEW`
- **THEN** `pantheon-service` grants that specific client `MANAGE` access while other clients on the site remain at `VIEW`

### Requirement: Permission enforcement on document projects and daily reports
`pantheon-service` SHALL reject a create/edit action on a construction site's document projects or daily reports from a member whose resolved access level for the corresponding capability is `VIEW` or `HIDDEN`, while still allowing a `VIEW` member to view existing records. A member whose resolved access level is `HIDDEN` SHALL be rejected from reading those records too.

#### Scenario: View-only member blocked from creating
- **WHEN** a construction site member whose resolved `DAILY_REPORT` access is `VIEW` attempts to create a daily report
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: View-only member can still read
- **WHEN** a construction site member whose resolved `DAILY_REPORT` access is `VIEW` requests the site's daily report list
- **THEN** `pantheon-service` returns the list

#### Scenario: Hidden member cannot read
- **WHEN** a construction site member whose resolved `DAILY_REPORT` (or `DOCUMENT_PROJECTS`) access is `HIDDEN` requests that capability's records
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Permission configuration view
`pantheon-web` SHALL provide, within a construction site's menu, a permission-configuration view (visible only to company staff) listing every capability — including `TEAM_MANAGE` — with the current function-level defaults and any member-level overrides, editable in place between `VIEW`, `MANAGE`, and `HIDDEN`.

#### Scenario: Admin edits a function default from the UI
- **WHEN** a company staff member changes a function's access level for a capability in the permission-configuration view
- **THEN** `pantheon-web` submits the change to `pantheon-service` and reflects the updated default immediately

#### Scenario: Admin sets a member override from the UI
- **WHEN** a company staff member selects a specific team member and sets an override for one capability
- **THEN** `pantheon-web` submits the override to `pantheon-service` and shows it distinctly from the function-level default

### Requirement: Permission enforcement on purchase requests and orçamento management
`pantheon-service` SHALL reject creating or converting a `PurchaseRequestItem` from a member without `MANAGE` `PURCHASE_REQUEST` access, and reject creating, editing, or submitting an `Orcamento` from a member without `MANAGE` `ORCAMENTO_MANAGE` access, regardless of their function. A member whose resolved access to either capability is `HIDDEN` SHALL also be rejected from reading that capability's records.

#### Scenario: Member without purchase-request access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a purchase-request item
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Member without orçamento-management access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create or submit an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Hidden member cannot read purchase requests or orçamentos
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` or `ORCAMENTO_MANAGE` access is `HIDDEN` requests that capability's records
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Permission enforcement on task board actions
`pantheon-service` SHALL reject a create-card, move-card, create-label, attach-label, or add-comment action on an obra's task board from a member whose resolved `TASKS` access level is `VIEW` or `HIDDEN`, while still allowing a `VIEW` member to view the board's columns, cards, labels, and comments. A member whose resolved `TASKS` access is `HIDDEN` SHALL be rejected from viewing the board too.

#### Scenario: View-only member blocked from creating a card
- **WHEN** a construction site member whose resolved `TASKS` access is `VIEW` attempts to create a task card
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: View-only member can still view the board
- **WHEN** a construction site member whose resolved `TASKS` access is `VIEW` requests the obra's task board
- **THEN** `pantheon-service` returns the columns, cards, labels, and comments

#### Scenario: Hidden member cannot view the board
- **WHEN** a construction site member whose resolved `TASKS` access is `HIDDEN` requests the obra's task board
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Hidden capability's menu is not shown
`pantheon-service` SHALL expose an endpoint for a site member to learn their own resolved access level for every capability on a construction site. `pantheon-web` SHALL use it to omit a capability's tab from the obra menu entirely when that member's resolved access to it is `HIDDEN`, and SHALL default to showing the first tab that is not hidden rather than a fixed one.

#### Scenario: Member's own resolved access is retrievable
- **WHEN** an active member of a construction site requests their own resolved access levels
- **THEN** `pantheon-service` returns the resolved `AccessLevel` for every capability, reflecting any function- or member-level overrides

#### Scenario: Hidden capability's tab is not shown
- **WHEN** a construction site member whose resolved access to a capability is `HIDDEN` opens that obra
- **THEN** `pantheon-web` does not show that capability's tab among the obra's menu

#### Scenario: Company staff are never hidden from anything
- **WHEN** company staff (any role) opens any obra of their company
- **THEN** `pantheon-web` shows every capability's tab, regardless of any function- or member-level `HIDDEN` override configured for site-team functions
