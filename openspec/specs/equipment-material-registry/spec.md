# equipment-material-registry Specification

## Purpose

Equipment and material catalogs scoped to a `ConstructionSite`: registration, status tracking for equipment, and listing, managed by a project's administrator or a member with construction function `SITE_FOREMAN`.
## Requirements
### Requirement: Equipment registration
`pantheon-service` SHALL allow a company administrator or a construction site member with `MANAGE` access to that site's `EQUIPMENT_MATERIAL` capability (by default, `SITE_FOREMAN`) to register `Equipment` under that construction site, recording a name, an optional type, and a status.

#### Scenario: Admin registers equipment
- **WHEN** a company administrator submits a name and status for a new piece of equipment on one of its construction sites
- **THEN** `pantheon-service` persists a new `Equipment` record linked to that site

#### Scenario: Site Foreman registers equipment
- **WHEN** a construction site member with function `SITE_FOREMAN` submits a name and status for a new piece of equipment on a site they belong to
- **THEN** `pantheon-service` persists a new `Equipment` record linked to that site

#### Scenario: Other members cannot register equipment
- **WHEN** a construction site member with `VIEW`-only `EQUIPMENT_MATERIAL` access attempts to register equipment
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Equipment status update
`pantheon-service` SHALL allow a company administrator or a member with `MANAGE` access to a site's `EQUIPMENT_MATERIAL` capability to update an `Equipment` record's status among `AVAILABLE`, `IN_USE`, `MAINTENANCE`, and `UNAVAILABLE`.

#### Scenario: Status updated
- **WHEN** a company administrator or a member with `EQUIPMENT_MATERIAL` management access submits a new status for an existing piece of equipment
- **THEN** `pantheon-service` updates the equipment's status to the submitted value

### Requirement: List equipment of a construction site
`pantheon-service` SHALL allow any member of a construction site to list the `Equipment` registered for it.

#### Scenario: Member lists equipment
- **WHEN** an authenticated member of a construction site requests the list of its equipment
- **THEN** `pantheon-service` returns every `Equipment` record linked to that site, including its current status

### Requirement: Material catalog registration
`pantheon-service` SHALL allow a company administrator or a construction site member with `MANAGE` access to that site's `EQUIPMENT_MATERIAL` capability to register a `Material` catalog entry under that construction site, recording a name and a unit of measure.

#### Scenario: Admin registers a material
- **WHEN** a company administrator submits a name and unit of measure for a new material on one of its construction sites
- **THEN** `pantheon-service` persists a new `Material` record linked to that site

#### Scenario: Other members cannot register a material
- **WHEN** a construction site member with `VIEW`-only `EQUIPMENT_MATERIAL` access attempts to register a material
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: List material catalog of a construction site
`pantheon-service` SHALL allow any member of a construction site to list the `Material` catalog entries registered for it.

#### Scenario: Member lists materials
- **WHEN** an authenticated member of a construction site requests the list of its materials
- **THEN** `pantheon-service` returns every `Material` catalog entry linked to that site

### Requirement: Equipment and material catalog views
`pantheon-web` SHALL provide views, scoped to a construction site, for registering and listing equipment and material catalog entries, and for updating an equipment's status, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: User manages equipment from the UI
- **WHEN** a user with permission to manage equipment submits the equipment registration form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new equipment in the site's equipment list

#### Scenario: User manages materials from the UI
- **WHEN** a user with permission to manage materials submits the material registration form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new material in the site's material catalog list

