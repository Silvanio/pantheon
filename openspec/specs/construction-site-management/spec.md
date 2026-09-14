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

### Requirement: Collapsible obra cover photo
`pantheon-web` SHALL allow the obra detail page's cover photo to be collapsed and expanded via a toggle control. When collapsed, the photo SHALL be hidden and the obra's name SHALL appear in the page header instead, alongside an expand control; when expanded, the photo SHALL show as before, with a collapse control on it. This preference SHALL be a single global choice, not remembered separately per obra, persisted so it is remembered on the next visit to any obra. The obra detail page's header SHALL also offer the light/dark theme toggle.

#### Scenario: Member collapses the cover photo
- **WHEN** a member on an obra's detail page collapses the cover photo
- **THEN** `pantheon-web` hides the photo and shows the obra's name in the page header along with an expand control

#### Scenario: Member expands the cover photo
- **WHEN** a member on an obra's detail page with the cover photo collapsed uses the expand control
- **THEN** `pantheon-web` shows the cover photo again as before

#### Scenario: Collapse preference carries over to other obras
- **WHEN** a member collapses the cover photo on one obra and then opens a different obra
- **THEN** `pantheon-web` shows that other obra's cover photo collapsed too, without the member repeating the action

#### Scenario: Theme toggle available on the obra detail page
- **WHEN** a member is on an obra's detail page
- **THEN** `pantheon-web` offers the light/dark theme toggle in that page's header

