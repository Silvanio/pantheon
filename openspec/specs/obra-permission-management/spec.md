# obra-permission-management Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT_MATERIAL`, `MATERIAL_REQUEST`, `MATERIAL_APPROVAL`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS` and `DAILY_REPORT` and may submit material requests, with approval additionally granted to `ENGINEER`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT_MATERIAL` and `DAILY_REPORT` and `VIEW` on `DOCUMENT_PROJECTS`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS` and `DAILY_REPORT` with no material actions; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT` only.

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client has no material access by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `MATERIAL_REQUEST`
- **THEN** `pantheon-service` reports that the client cannot submit material requests

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

### Requirement: Permission enforcement on material requests and approval
`pantheon-service` SHALL reject a material/budget request from a member with no `MATERIAL_REQUEST` access, and reject an approval or rejection decision from a member with no `MATERIAL_APPROVAL` access, regardless of their function.

#### Scenario: Member without request access blocked
- **WHEN** a construction site member with no `MATERIAL_REQUEST` access attempts to create a material request
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Member without approval access blocked
- **WHEN** a construction site member with no `MATERIAL_APPROVAL` access attempts to approve or reject a pending material request
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Permission configuration view
`pantheon-web` SHALL provide, within a construction site's menu, a permission-configuration view (visible only to company staff) listing each capability with the current function-level defaults and any member-level overrides, editable in place.

#### Scenario: Admin edits a function default from the UI
- **WHEN** a company staff member changes a function's access level for a capability in the permission-configuration view
- **THEN** `pantheon-web` submits the change to `pantheon-service` and reflects the updated default immediately

#### Scenario: Admin sets a member override from the UI
- **WHEN** a company staff member selects a specific team member and sets an override for one capability
- **THEN** `pantheon-web` submits the override to `pantheon-service` and shows it distinctly from the function-level default

