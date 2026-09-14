## MODIFIED Requirements

### Requirement: Obra team management views
`pantheon-web` SHALL provide, within a construction site's menu, a team view listing clients, service providers, architects, engineers, and site foremen separately, with a single add-member form (name required for every function, email required except for the accountless service-provider case, CPF and phone always optional), showing a "Convite" indicator for unaccepted invitations. The form's CPF and email fields SHALL offer autocomplete suggestions from the company's own already-known people (see "People search"); selecting a suggestion SHALL pre-fill the form and label the submit action "Enviar convite" instead of "Adicionar". The submit action SHALL be disabled, with an explanatory message, when the typed email is already an active member of this exact obra, or already belongs to a person with a membership in a different company.

#### Scenario: Admin adds an engineer from the UI
- **WHEN** a company staff member submits the add-engineer form with a name and an email
- **THEN** `pantheon-web` submits it to `pantheon-service` and shows the engineer in the site's team list with a "Convite" badge until accepted

#### Scenario: Admin adds a service provider from the UI
- **WHEN** a company staff member submits the add-service-provider form with a name and trade, leaving the email field blank
- **THEN** `pantheon-web` submits it to `pantheon-service` and shows the service provider in the site's team list with no invitation badge

#### Scenario: Selecting a known person pre-fills the form
- **WHEN** a company staff member picks a suggestion from the CPF or email autocomplete
- **THEN** `pantheon-web` fills the name, email, CPF, and phone fields from that suggestion and labels the submit button "Enviar convite"

#### Scenario: Typing a new person's details behaves as before
- **WHEN** a company staff member types a CPF or email that matches no suggestion and fills in the rest of the form
- **THEN** `pantheon-web` submits it as a new team member exactly as it does today, unaffected by the autocomplete

#### Scenario: Submit disabled for someone already active on this obra
- **WHEN** the typed email already belongs to a member with `ACTIVE` status on the obra currently being managed
- **THEN** `pantheon-web` disables the submit action and explains that this person is already on the team

#### Scenario: Pending invitation on this obra is not treated as a duplicate
- **WHEN** the typed email already belongs to a member of this obra whose invitation is still `INVITED` (not yet accepted)
- **THEN** `pantheon-web` does not disable the submit action; submitting resends that invitation as it already does today

#### Scenario: Reusing a known person from another obra in the same company still works
- **WHEN** a company staff member selects a suggestion for a person already active on a different obra of the same company and submits
- **THEN** `pantheon-web` submits it successfully, inviting that person to this obra

#### Scenario: Submit disabled for someone belonging to another company
- **WHEN** the typed email already belongs to an account with any membership (company staff or any obra's team) in a company other than the one being managed
- **THEN** `pantheon-web` disables the submit action and explains that this email belongs to a different company

## ADDED Requirements

### Requirement: Duplicate and cross-company membership rejected server-side
`pantheon-service` SHALL reject, as the authoritative check regardless of what the UI already prevented, an add-member request whose email is already an `ACTIVE` member of the target construction site, or whose email already belongs to an `AppUser` with any membership in a company other than the target site's company. Neither check applies to the accountless service-provider path, which has no email to check.

#### Scenario: Backend rejects a duplicate active member
- **WHEN** an add-member request's email is already an `ACTIVE` `SiteMembership` on the target construction site
- **THEN** `pantheon-service` rejects the request and creates no new membership

#### Scenario: Backend rejects a person from another company
- **WHEN** an add-member request's email belongs to an `AppUser` with a membership (company staff or any obra's team) in a company other than the target site's company
- **THEN** `pantheon-service` rejects the request and creates no new membership

#### Scenario: Backend allows reusing a person already known to the same company
- **WHEN** an add-member request's email belongs to an `AppUser` whose only existing memberships are within the target site's own company
- **THEN** `pantheon-service` proceeds with the normal invite flow, reusing that account
