# company-task-columns Specification

## Purpose
TBD - created by syncing change add-obra-tasks-board. Update Purpose after archive.

## Requirements
### Requirement: Company-admin column management
`pantheon-service` SHALL allow only a company member with `CompanyRole.ADMIN` to create, rename, reorder, or delete a `TaskColumn` belonging to that company; every such request from a member without that role SHALL be rejected with HTTP 403.

#### Scenario: Admin creates a column
- **WHEN** a company member with role `ADMIN` creates a task column named "Em andamento"
- **THEN** `pantheon-service` persists the column for that company and returns it

#### Scenario: Non-admin blocked from creating a column
- **WHEN** a company member with role `MEMBER` attempts to create a task column
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Shared column set across obras
`pantheon-service` SHALL expose the same ordered set of `TaskColumn`s to every construction site belonging to a company; no construction site SHALL have its own independent column set.

#### Scenario: Two obras see the same columns
- **WHEN** two different construction sites under the same company request their task board's columns
- **THEN** `pantheon-service` returns the identical ordered list of columns for both

### Requirement: Column deletion blocked while in use
`pantheon-service` SHALL reject deleting a `TaskColumn` that has at least one `TaskCard` referencing it, in any construction site of that company, and SHALL indicate the column is in use.

#### Scenario: Delete blocked when cards exist
- **WHEN** an admin attempts to delete a column that still has at least one card placed in it
- **THEN** `pantheon-service` rejects the request and reports the column is in use

#### Scenario: Delete succeeds when column is empty
- **WHEN** an admin deletes a column that has no cards in any obra
- **THEN** `pantheon-service` removes the column

### Requirement: Column configuration view
`pantheon-web` SHALL provide a column-configuration panel within company settings (outside any construction site's menu), reachable from the top-right company/profile menu, visible and usable only to company members with role `ADMIN`, for creating, renaming, reordering, and deleting columns.

#### Scenario: Admin manages columns from company settings
- **WHEN** a company member with role `ADMIN` opens company settings
- **THEN** `pantheon-web` shows the task-column configuration panel with create, rename, reorder, and delete controls

#### Scenario: Non-admin does not see the panel
- **WHEN** a company member with role `MEMBER` opens company settings
- **THEN** `pantheon-web` does not show the task-column configuration panel
