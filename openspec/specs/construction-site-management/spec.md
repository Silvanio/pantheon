# construction-site-management Specification

## Purpose

Construction-domain foundation for Pantheon: physical construction sites (`ConstructionSite`) and architectural design records (`ArchitecturalProject`) under a `Project`, plus construction-function classification (Client, Engineer, Architect, Site Foreman, Service Provider) of project members, independent of their platform permission role (ADMIN/MEMBER).

## Requirements

### Requirement: Construction site registration
`pantheon-service` SHALL allow an administrator of a `Project` to create a `ConstructionSite` under that project, recording a name, address, start date, and optional expected end date.

#### Scenario: Admin creates a construction site
- **WHEN** the administrator of a project submits a name, address, and start date for a new construction site
- **THEN** `pantheon-service` persists a new `ConstructionSite` linked to that project, with status `PLANNING`

#### Scenario: Non-admin cannot create a construction site
- **WHEN** a project member who is not the project's administrator attempts to create a construction site
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Construction site status lifecycle
Each `ConstructionSite` SHALL have a status of `PLANNING`, `IN_PROGRESS`, `PAUSED`, or `COMPLETED`, updatable by the project's administrator.

#### Scenario: Admin updates site status
- **WHEN** the administrator of a project's construction site submits a new status
- **THEN** `pantheon-service` updates the site's status to the submitted value

### Requirement: List construction sites of a project
`pantheon-service` SHALL allow any member of a project to list the project's `ConstructionSite`s.

#### Scenario: Member lists construction sites
- **WHEN** an authenticated member of a project requests the list of the project's construction sites
- **THEN** `pantheon-service` returns every construction site belonging to that project, including its status

### Requirement: Architectural project registration
`pantheon-service` SHALL allow a project member to register an `ArchitecturalProject` under a `Project`, with a name, an optional description, and an optional link to one of the project's `ConstructionSite`s.

#### Scenario: Member registers an architectural project
- **WHEN** a project member submits a name for a new architectural project, optionally linking it to one of the project's construction sites
- **THEN** `pantheon-service` persists a new `ArchitecturalProject` associated with the project (and the site, if given)

#### Scenario: List architectural projects
- **WHEN** a project member requests the list of the project's architectural projects
- **THEN** `pantheon-service` returns every `ArchitecturalProject` belonging to that project

### Requirement: Member construction function classification
When adding a member to a project, `pantheon-service` SHALL accept an optional construction function (`CLIENT`, `ENGINEER`, `ARCHITECT`, `SITE_FOREMAN`, `SERVICE_PROVIDER`, or `OTHER`) for that membership, and an optional specialty text when the function is `SERVICE_PROVIDER`.

#### Scenario: Admin adds a member with a function
- **WHEN** the administrator of a project adds a member and specifies a construction function
- **THEN** `pantheon-service` records that function on the resulting `ProjectMembership`

#### Scenario: Admin adds a service-provider member with a specialty
- **WHEN** the administrator of a project adds a member with function `SERVICE_PROVIDER` and a specialty (e.g., "Painter")
- **THEN** `pantheon-service` records both the function and the specialty on the resulting `ProjectMembership`

#### Scenario: Function omitted defaults gracefully
- **WHEN** the administrator of a project adds a member without specifying a construction function
- **THEN** `pantheon-service` records the membership with function `OTHER`

### Requirement: List project members
`pantheon-service` SHALL allow any member of a project to list that project's members, including each member's role, construction function, and specialty.

#### Scenario: Member lists project members
- **WHEN** an authenticated member of a project requests the project's member list
- **THEN** `pantheon-service` returns every member of the project with their email, role, construction function, and specialty

#### Scenario: Non-member cannot list project members
- **WHEN** a user who is not a member of the project requests its member list
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Construction site management views
`pantheon-web` SHALL provide views for listing and creating `ConstructionSite`s under a project, a form for registering `ArchitecturalProject`s, a construction-function/specialty selector in the add-member flow, and a project member list showing each member's role, function, and specialty.

#### Scenario: Admin creates a construction site from the UI
- **WHEN** a project administrator submits the construction site creation form with a valid name, address, and start date
- **THEN** `pantheon-web` submits the data to `pantheon-service` and, on success, shows the new site in the project's site list

#### Scenario: Admin adds a member with a function from the UI
- **WHEN** a project administrator submits the add-member form with a selected construction function (and specialty, if Service Provider)
- **THEN** `pantheon-web` submits the function (and specialty) alongside the existing add-member data
