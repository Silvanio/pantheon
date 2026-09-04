## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Architectural project registration
**Reason**: Generalized into a construction-site-owned document project (`SiteDocumentProject`) that supports PDF attachments and is no longer optionally linked — it always belongs to a specific site.
**Migration**: See `site-document-projects`.

### Requirement: Member construction function classification
**Reason**: Construction functions (Client, Engineer, Architect, Site Foreman, Service Provider) now describe construction-site-scoped `SiteMembership` rows, not company-wide project membership.
**Migration**: See `obra-team-management`.

### Requirement: List project members
**Reason**: Replaced by two separate listings matching the new hierarchy: a company's staff list (via `team-invitations`/company membership) and a construction site's team list.
**Migration**: See `obra-team-management`'s "List a construction site's team" requirement.
