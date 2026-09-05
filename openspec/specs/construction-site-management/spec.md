# construction-site-management Specification

## Purpose

Construction-domain foundation for Pantheon: physical construction sites (`ConstructionSite`) and architectural design records (`ArchitecturalProject`) under a `Project`, plus construction-function classification (Client, Engineer, Architect, Site Foreman, Service Provider) of project members, independent of their platform permission role (ADMIN/MEMBER).
## Requirements
### Requirement: Construction site registration
`pantheon-service` SHALL allow an administrator of a `Company` to create a `ConstructionSite` under that company, recording a name, address, start date, optional expected end date, and an optional photo, subject to the company's plan active-site limit (see `company-plan-catalog`).

#### Scenario: Admin creates a construction site
- **WHEN** the administrator of a company submits a name, address, and start date for a new construction site
- **THEN** `pantheon-service` persists a new `ConstructionSite` linked to that company, with status `PLANNING`

#### Scenario: Non-admin cannot create a construction site
- **WHEN** a company staff member who is not the company's administrator attempts to create a construction site
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Construction site status lifecycle
Each `ConstructionSite` SHALL have a status of `PLANNING`, `IN_PROGRESS`, `PAUSED`, or `COMPLETED`, updatable by the company's administrator.

#### Scenario: Admin updates site status
- **WHEN** the administrator of a company updates one of its construction sites' status
- **THEN** `pantheon-service` updates the site's status to the submitted value

### Requirement: List construction sites of a project
`pantheon-service` SHALL allow any staff member of a company to list the company's `ConstructionSite`s.

#### Scenario: Member lists construction sites
- **WHEN** an authenticated staff member of a company requests the list of the company's construction sites
- **THEN** `pantheon-service` returns every construction site belonging to that company, including its status

### Requirement: Construction site management views
`pantheon-web` SHALL provide views for listing and creating `ConstructionSite`s under a company, and a construction site status editor, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Admin creates a construction site from the UI
- **WHEN** a company administrator submits the construction site creation form with a valid name, address, and start date
- **THEN** `pantheon-web` submits the data to `pantheon-service` and, on success, shows the new site as a card on the dashboard

#### Scenario: Admin adds a member with a function from the UI
- **WHEN** a company administrator submits a construction site's team add-member form with a selected construction function (and specialty, if Service Provider)
- **THEN** `pantheon-web` submits the function (and specialty) alongside the existing add-member data, scoped to that construction site rather than the company as a whole

#### Scenario: Invited member shows a Convite badge
- **WHEN** a construction site's team list contains a member whose invitation has not been accepted
- **THEN** `pantheon-web` displays that member with a "Convite" badge next to their name, and removes the badge once the invitation is accepted

#### Scenario: Admin invites an email with no account
- **WHEN** a company administrator submits a construction site's team add-member form with an email that is not yet registered
- **THEN** `pantheon-web` submits it successfully and shows the person in the site's team list with a "Convite" badge

