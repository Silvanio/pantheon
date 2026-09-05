## MODIFIED Requirements

### Requirement: Project membership management
`pantheon-service` SHALL allow the administrator of a project to add another person to that project by email as a `MEMBER`. The resulting `ProjectMembership` SHALL be created in an `INVITED` state that confers no project access until the invitation is accepted. If the email does not match any existing account, `pantheon-service` SHALL create a pre-registration account for it (registration status `PENDING_REGISTRATION`, no credentials) and link the invited membership to that account.

#### Scenario: Admin adds a member
- **WHEN** the administrator of a project submits a request to add a person whose email belongs to an existing active account
- **THEN** `pantheon-service` creates a `ProjectMembership` linking that account to the project with role `MEMBER` and status `INVITED`

#### Scenario: Admin invites an unregistered email
- **WHEN** the administrator of a project submits a request to add a person whose email does not belong to any account
- **THEN** `pantheon-service` creates a `PENDING_REGISTRATION` account for that email and an `INVITED` `ProjectMembership` linking it to the project with role `MEMBER`

#### Scenario: Non-admin cannot add a member
- **WHEN** a user who is not the administrator of a project attempts to add another user to it
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Invited membership does not grant access
- **WHEN** a user whose membership on a project is `INVITED` requests that project's data or member list
- **THEN** `pantheon-service` responds with HTTP 403, the same as for a non-member

### Requirement: Onboarding status
`pantheon-service` SHALL expose an authenticated endpoint that reports whether the current user has any `ACTIVE` project membership, whether any of their `ACTIVE` memberships is on an active (non-expired trial or plan) project, and whether plan selection is required. Memberships in the `INVITED` state SHALL be ignored for this determination.

#### Scenario: User with no projects
- **WHEN** an authenticated user with zero project memberships requests their onboarding status
- **THEN** `pantheon-service` reports that the user has no project

#### Scenario: User with only invited memberships
- **WHEN** an authenticated user has one or more `ProjectMembership` records but all of them are `INVITED`
- **THEN** `pantheon-service` reports that the user has no project

#### Scenario: User with at least one active project
- **WHEN** an authenticated user has at least one `ACTIVE` project membership on an active project
- **THEN** `pantheon-service` reports the user as having an active project and does not require plan selection

#### Scenario: User whose only projects are expired
- **WHEN** an authenticated user has one or more `ACTIVE` project memberships but none of the associated projects are active
- **THEN** `pantheon-service` reports that plan selection is required, identifying the expired project(s) the user administers

## ADDED Requirements

### Requirement: Account registration lifecycle
`pantheon-service` SHALL track a registration status on each account of either `ACTIVE` or `PENDING_REGISTRATION`. A `PENDING_REGISTRATION` account has no usable credentials and SHALL NOT be able to authenticate until its registration is completed.

#### Scenario: Pending account cannot log in
- **WHEN** someone attempts to log in with the email of a `PENDING_REGISTRATION` account
- **THEN** `pantheon-service` responds with HTTP 401 and does not issue a token

#### Scenario: Normal registration completes a pending account
- **WHEN** a user registers through the normal registration endpoint using an email that currently has a `PENDING_REGISTRATION` account
- **THEN** `pantheon-service` completes that existing account with the submitted credentials and sets its registration status to `ACTIVE` rather than reporting the email as already registered

#### Scenario: Legacy accounts treated as active
- **WHEN** an account created before this lifecycle existed is loaded
- **THEN** `pantheon-service` treats its registration status as `ACTIVE`
