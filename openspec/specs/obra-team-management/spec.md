# obra-team-management Specification

## Purpose
TBD - created by archiving change restructure-company-obra-hierarchy. Update Purpose after archive.
## Requirements
### Requirement: Client membership
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's team to register a `SiteMembership` with function `CLIENT`, recording an email, name, and CPF, and SHALL drive it through the same invite/accept lifecycle as `team-invitations`: an unregistered email is pre-registered and emailed a registration link, a registered email is emailed an invitation to accept.

#### Scenario: Client invited with an existing account
- **WHEN** a company staff member registers a client by an email that already has an account
- **THEN** `pantheon-service` creates an `INVITED` `SiteMembership` with function `CLIENT` and sends that account an invitation to accept

#### Scenario: Client invited with no account
- **WHEN** a company staff member registers a client by an email with no existing account
- **THEN** `pantheon-service` pre-registers that email and sends a registration-and-invitation email; the client must complete registration and then accept

#### Scenario: Client accepts and gains access
- **WHEN** the invited client accepts the invitation
- **THEN** `pantheon-service` flips the `SiteMembership` from `INVITED` to `ACTIVE`, granting the client visibility of the construction site per their configured permissions

### Requirement: Service provider membership without an account
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's team to register a `SiteMembership` with function `SERVICE_PROVIDER`, a free-text trade (e.g., Pedreiro, Encanador, Eletricista, Terceirizado), and a display name/contact, with no user account and no invitation required. An account MAY optionally be attached later, at which point the membership follows the same invite/accept lifecycle as other roles.

#### Scenario: Service provider registered without an account
- **WHEN** a company staff member registers a service provider with a name, trade, and contact but no email tied to an account
- **THEN** `pantheon-service` creates an `ACTIVE` `SiteMembership` with function `SERVICE_PROVIDER` and no associated user, requiring no login

#### Scenario: Account attached later
- **WHEN** a company staff member later provides an email to give an existing accountless service-provider membership a login
- **THEN** `pantheon-service` creates an invitation for that membership following the standard invite/accept lifecycle

### Requirement: Architect, engineer, and site foreman membership
`pantheon-service` SHALL allow a company staff member with `MANAGE` access to a construction site's team to register a `SiteMembership` with function `ARCHITECT`, `ENGINEER`, or `SITE_FOREMAN`, always requiring the invite/accept lifecycle: an existing account must explicitly accept, and an unregistered email must first complete registration and then accept.

#### Scenario: Engineer invited and must accept
- **WHEN** a company staff member registers an engineer by an email with an existing account
- **THEN** `pantheon-service` creates an `INVITED` `SiteMembership` with function `ENGINEER` that confers no access until the invitee accepts

#### Scenario: Architect with no account must register then accept
- **WHEN** a company staff member registers an architect by an email with no existing account
- **THEN** `pantheon-service` pre-registers the email, sends a registration-and-invitation email, and the architect gains access only after registering and accepting

### Requirement: List a construction site's team
`pantheon-service` SHALL allow any active member of a construction site (company staff or an `ACTIVE` `SiteMembership`) to list that site's team, including each member's function, trade (for service providers), and invitation state.

#### Scenario: Member lists the site team
- **WHEN** an active member of a construction site requests its team list
- **THEN** `pantheon-service` returns every `SiteMembership` of that site with its function, trade (if any), and whether it is still an unaccepted invitation

#### Scenario: Non-member cannot list the team
- **WHEN** a user with no active relationship to a construction site requests its team list
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Obra team management views
`pantheon-web` SHALL provide, within a construction site's menu, a team view listing clients, service providers, architects, engineers, and site foremen separately, with forms to add each (an email-based form for client/architect/engineer/site foreman, a name-and-trade form with no email required for service providers), showing a "Convite" indicator for unaccepted invitations.

#### Scenario: Admin adds an engineer from the UI
- **WHEN** a company staff member submits the add-engineer form with an email
- **THEN** `pantheon-web` submits it to `pantheon-service` and shows the engineer in the site's team list with a "Convite" badge until accepted

#### Scenario: Admin adds a service provider from the UI
- **WHEN** a company staff member submits the add-service-provider form with a name and trade, leaving the email field blank
- **THEN** `pantheon-web` submits it to `pantheon-service` and shows the service provider in the site's team list with no invitation badge

