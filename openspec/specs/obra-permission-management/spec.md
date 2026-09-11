# obra-permission-management Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `PURCHASE_REQUEST`, and `ORCAMENTO_MANAGE`, and `VIEW` on `EQUIPMENT`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT` and `DAILY_REPORT`, and `VIEW` on `DOCUMENT_PROJECTS`, `PURCHASE_REQUEST`, and `ORCAMENTO_MANAGE`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS` and `DAILY_REPORT` with no manage access on any material/orçamento capability; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT` only. (Authority to act on a specific Orçamento approval step is a separate, structural concern — see `orcamento-approval-workflow` — not one of these capabilities.)

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client has no material access by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `PURCHASE_REQUEST`
- **THEN** `pantheon-service` reports that the client cannot create purchase-request items

### Requirement: Function-level override
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to set a function-level override, replacing the default access level for every `SiteMembership` of that function on that site.

#### Scenario: Admin restricts architects to view-only
- **WHEN** a company staff member sets the `DOCUMENT_PROJECTS` access level for function `ARCHITECT` on a site to `VIEW`
- **THEN** `pantheon-service` applies `VIEW` access to every architect on that site who has no member-specific override

### Requirement: Member-level override
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's permissions to set an override for one specific `SiteMembership`, which SHALL take precedence over that site's function-level default or override for that member alone.

#### Scenario: One engineer granted an exception
- **WHEN** a company staff member sets a member-specific `DAILY_REPORT` override to `MANAGE` for one client on a site where clients default to `VIEW`
- **THEN** `pantheon-service` grants that specific client `MANAGE` access while other clients on the site remain at `VIEW`

### Requirement: Permission enforcement on document projects and daily reports
`pantheon-service` SHALL reject a create/edit action on a construction site's document projects or daily reports from a member whose resolved access level for the corresponding capability is `VIEW`, while still allowing that member to view existing records.

#### Scenario: View-only member blocked from creating
- **WHEN** a construction site member whose resolved `DAILY_REPORT` access is `VIEW` attempts to create a daily report
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: View-only member can still read
- **WHEN** a construction site member whose resolved `DAILY_REPORT` access is `VIEW` requests the site's daily report list
- **THEN** `pantheon-service` returns the list

### Requirement: Permission configuration view
`pantheon-web` SHALL provide, within a construction site's menu, a permission-configuration view (visible only to company staff) listing each capability with the current function-level defaults and any member-level overrides, editable in place.

#### Scenario: Admin edits a function default from the UI
- **WHEN** a company staff member changes a function's access level for a capability in the permission-configuration view
- **THEN** `pantheon-web` submits the change to `pantheon-service` and reflects the updated default immediately

#### Scenario: Admin sets a member override from the UI
- **WHEN** a company staff member selects a specific team member and sets an override for one capability
- **THEN** `pantheon-web` submits the override to `pantheon-service` and shows it distinctly from the function-level default

### Requirement: Permission enforcement on purchase requests and orçamento management
`pantheon-service` SHALL reject creating or converting a `PurchaseRequestItem` from a member with no `PURCHASE_REQUEST` access, and reject creating, editing, or submitting an `Orcamento` from a member with no `ORCAMENTO_MANAGE` access, regardless of their function.

#### Scenario: Member without purchase-request access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a purchase-request item
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Member without orçamento-management access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create or submit an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

