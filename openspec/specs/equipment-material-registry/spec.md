# equipment-material-registry Specification

## Purpose

Equipment and material catalogs scoped to a `ConstructionSite`: registration, status tracking for equipment, and listing, managed by a project's administrator or a member with construction function `SITE_FOREMAN`.
## Requirements
### Requirement: Equipment registration
`pantheon-service` SHALL allow a company administrator or a construction site member with `MANAGE` access to that site's `EQUIPMENT` capability (by default, `SITE_FOREMAN`) to register `Equipment` under that construction site, recording a name, an optional type, and a status.

#### Scenario: Admin registers equipment
- **WHEN** a company administrator submits a name and status for a new piece of equipment on one of its construction sites
- **THEN** `pantheon-service` persists a new `Equipment` record linked to that site

#### Scenario: Site Foreman registers equipment
- **WHEN** a construction site member with function `SITE_FOREMAN` submits a name and status for a new piece of equipment on a site they belong to
- **THEN** `pantheon-service` persists a new `Equipment` record linked to that site

#### Scenario: Other members cannot register equipment
- **WHEN** a construction site member with `VIEW`-only `EQUIPMENT` access attempts to register equipment
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Equipment status update
`pantheon-service` SHALL allow a company administrator or a member with `MANAGE` access to a site's `EQUIPMENT` capability to update an `Equipment` record's status among `AVAILABLE`, `IN_USE`, `MAINTENANCE`, and `UNAVAILABLE`.

#### Scenario: Status updated
- **WHEN** a company administrator or a member with `EQUIPMENT` management access submits a new status for an existing piece of equipment
- **THEN** `pantheon-service` updates the equipment's status to the submitted value

### Requirement: List equipment of a construction site
`pantheon-service` SHALL allow any member of a construction site to list the `Equipment` registered for it — paginated, sorted by creation date descending (newest first).

#### Scenario: Member lists equipment
- **WHEN** an authenticated member of a construction site requests a page of its equipment
- **THEN** `pantheon-service` returns that page of `Equipment` records linked to that site, including each one's current status, ordered by creation date descending, along with the total element and page counts

#### Scenario: Newest equipment appears first
- **WHEN** a construction site has multiple equipment records and a member requests the first page
- **THEN** the most recently created `Equipment` record is the first item returned

### Requirement: Equipment views
`pantheon-web` SHALL provide, on its own construction-site tab independent of any material or budget feature, a paginated view (fetching one page on demand, not the full list, with a page-size selector offering 1, 5, or 10 results per page) for listing equipment, a view for registering equipment, and a view for updating an equipment's status, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: User manages equipment from the UI
- **WHEN** a user with permission to manage equipment submits the equipment registration form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new equipment in the site's equipment list, on the "Equipamentos" tab

#### Scenario: User pages through the equipment list
- **WHEN** a user on the "Equipamentos" tab has more equipment than fits on one page
- **THEN** `pantheon-web` shows prev/next controls that fetch the adjacent page on demand

#### Scenario: User changes the equipment list's page size
- **WHEN** a user selects a different page-size option (1, 5, or 10) on the equipment list
- **THEN** `pantheon-web` reloads the list from the first page using the newly selected size

