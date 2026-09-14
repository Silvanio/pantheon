# obra-team-management Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Client membership
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's team to register a `SiteMembership` with function `CLIENT`, recording a required name and email, and an optional CPF and phone, and SHALL drive it through the same invite/accept lifecycle as `team-invitations`: an unregistered email is pre-registered (using the submitted name as its initial display name) and emailed a registration link, a registered email is emailed an invitation to accept.

#### Scenario: Client invited with an existing account
- **WHEN** a company staff member registers a client by an email that already has an account
- **THEN** `pantheon-service` creates an `INVITED` `SiteMembership` with function `CLIENT` and sends that account an invitation to accept, without changing that account's existing display name

#### Scenario: Client invited with no account
- **WHEN** a company staff member registers a client by a name and an email with no existing account
- **THEN** `pantheon-service` pre-registers that email with the submitted name as its display name and sends a registration-and-invitation email; the client must complete registration and then accept

#### Scenario: Client accepts and gains access
- **WHEN** the invited client accepts the invitation
- **THEN** `pantheon-service` flips the `SiteMembership` from `INVITED` to `ACTIVE`, granting the client visibility of the construction site per their configured permissions

### Requirement: Service provider membership without an account
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's team to register a `SiteMembership` with function `SERVICE_PROVIDER`, a free-text trade (e.g., Pedreiro, Encanador, Eletricista, Terceirizado), a required display name, and an optional contact email, CPF, and phone, with no user account and no invitation required. An account MAY optionally be attached later, at which point the membership follows the same invite/accept lifecycle as other roles.

#### Scenario: Service provider registered without an account
- **WHEN** a company staff member registers a service provider with a name, trade, and contact but no email tied to an account
- **THEN** `pantheon-service` creates an `ACTIVE` `SiteMembership` with function `SERVICE_PROVIDER` and no associated user, requiring no login

#### Scenario: Account attached later
- **WHEN** a company staff member later provides an email to give an existing accountless service-provider membership a login
- **THEN** `pantheon-service` creates an invitation for that membership following the standard invite/accept lifecycle

### Requirement: Architect, engineer, and site foreman membership
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's team to register a `SiteMembership` with function `ARCHITECT`, `ENGINEER`, or `SITE_FOREMAN`, recording a required name and email and an optional CPF and phone, always requiring the invite/accept lifecycle: an existing account must explicitly accept, and an unregistered email must first complete registration (pre-registered with the submitted name as its initial display name) and then accept.

#### Scenario: Engineer invited and must accept
- **WHEN** a company staff member registers an engineer by a name and an email with an existing account
- **THEN** `pantheon-service` creates an `INVITED` `SiteMembership` with function `ENGINEER` that confers no access until the invitee accepts

#### Scenario: Architect with no account must register then accept
- **WHEN** a company staff member registers an architect by a name and an email with no existing account
- **THEN** `pantheon-service` pre-registers the email with the submitted name as its display name, sends a registration-and-invitation email, and the architect gains access only after registering and accepting

### Requirement: List a construction site's team
`pantheon-service` SHALL allow any active member of a construction site (company staff or an `ACTIVE` `SiteMembership`) to list that site's team, including each member's function, trade (for service providers), and invitation state.

#### Scenario: Member lists the site team
- **WHEN** an active member of a construction site requests its team list
- **THEN** `pantheon-service` returns every `SiteMembership` of that site with its function, trade (if any), and whether it is still an unaccepted invitation

#### Scenario: Non-member cannot list the team
- **WHEN** a user with no active relationship to a construction site requests its team list
- **THEN** `pantheon-service` rejects the request with HTTP 403

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

### Requirement: People search for reusing known team members
`pantheon-service` SHALL expose an endpoint, gated by the same `MANAGE`-on-team access as adding a member, that searches for people already known to the acting company — its staff, and every construction site's team members, account-holding or not — by a CPF or email prefix, returning each match's name (falling back to the email's local part when no name is recorded), email, CPF, and phone. The search SHALL NOT return people from any other company.

#### Scenario: Search finds a person from another obra in the same company
- **WHEN** a company staff member searches by an email or CPF prefix belonging to a person already on a different obra's team in the same company
- **THEN** `pantheon-service` returns that person's known name, email, CPF, and phone

#### Scenario: Search never returns another company's people
- **WHEN** a company staff member searches by a CPF or email prefix that only matches a person belonging to a different company
- **THEN** `pantheon-service` does not return that person

#### Scenario: Non-staff cannot search
- **WHEN** a user without `MANAGE` access to a construction site's team requests that site's people search
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: CPF and email format validation
`pantheon-service` SHALL reject a malformed email on any team-member registration that requires one, and SHALL reject a CPF that fails Brazil's standard format and check-digit validation whenever a CPF is supplied (CPF remains optional; only a *supplied* CPF is validated).

#### Scenario: Malformed email rejected
- **WHEN** a company staff member submits an add-member request with an email that is not a valid email format
- **THEN** `pantheon-service` rejects the request with a validation error and creates no membership

#### Scenario: Malformed CPF rejected
- **WHEN** a company staff member submits an add-member request with a CPF that fails the standard check-digit validation
- **THEN** `pantheon-service` rejects the request with a validation error and creates no membership

#### Scenario: Well-formed CPF accepted
- **WHEN** a company staff member submits an add-member request with a CPF that passes the standard check-digit validation
- **THEN** `pantheon-service` accepts it and persists the CPF on the new membership

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
