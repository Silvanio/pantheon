## ADDED Requirements

### Requirement: Adding a member creates an invitation
When the administrator of a project adds a member by email, `pantheon-service` SHALL create the `ProjectMembership` in an `INVITED` state and SHALL NOT grant that person access to the project until the invitation is accepted. This applies whether or not the email already belongs to a registered account.

#### Scenario: Invitation created for an existing account
- **WHEN** the administrator of a project adds a member whose email belongs to an existing active account
- **THEN** `pantheon-service` creates a `ProjectMembership` with role `MEMBER` and status `INVITED`, creates a `MembershipInvitation` record, and publishes a `team-invitation-created` event

#### Scenario: Invitation created with pre-registration for an unknown email
- **WHEN** the administrator of a project adds a member whose email does not belong to any account
- **THEN** `pantheon-service` creates a pre-registration `AppUser` for that email with registration status `PENDING_REGISTRATION` and no credentials, creates an `INVITED` `ProjectMembership` linked to it, creates a `MembershipInvitation` marked as requiring registration, and publishes a `team-invitation-created` event

#### Scenario: Invited person has no project access yet
- **WHEN** a user whose only link to a project is an `INVITED` membership attempts to read or act on that project
- **THEN** `pantheon-service` rejects the request as if the user were not a member

#### Scenario: Re-adding an already-invited email
- **WHEN** the administrator adds an email that already has an `INVITED` membership on that project
- **THEN** `pantheon-service` does not create a duplicate membership and re-issues the invitation for the existing one

#### Scenario: Re-adding an already-active member
- **WHEN** the administrator adds an email that already has an `ACTIVE` membership on that project
- **THEN** `pantheon-service` rejects the request with HTTP 409

### Requirement: Invitation token and lookup
`pantheon-service` SHALL identify each invitation by a single-use secret token delivered only in the invitation email, storing only a hash of the token, and SHALL expose an unauthenticated endpoint to retrieve an invitation's public details by token.

#### Scenario: Valid token returns invitation details
- **WHEN** a client requests `GET /api/invitations/{token}` with a valid, unexpired token
- **THEN** `pantheon-service` returns the project name, the inviter's name, the invited email, whether registration is required, and the invitation status

#### Scenario: Unknown or expired token rejected
- **WHEN** a client requests `GET /api/invitations/{token}` with a token that does not match any invitation or whose invitation has expired
- **THEN** `pantheon-service` responds with HTTP 404

### Requirement: Completing pre-registration from an invitation
`pantheon-service` SHALL allow the holder of an invitation token that requires registration to complete the pre-registration account by setting a password and display name, after which that account is a normal, log-in-capable account. Completing registration SHALL NOT by itself add the person to the project.

#### Scenario: Pending user completes registration
- **WHEN** the holder of a registration-required invitation token submits a password and display name to `POST /api/invitations/{token}/complete-registration`
- **THEN** `pantheon-service` sets the credentials on the pre-registration account, changes its registration status to `ACTIVE`, and returns a session token authenticating that user

#### Scenario: Registration not applicable
- **WHEN** the holder of an invitation token whose invited email already had an active account calls `POST /api/invitations/{token}/complete-registration`
- **THEN** `pantheon-service` rejects the request with HTTP 409

#### Scenario: Membership still pending after registration
- **WHEN** a pending user has completed registration from their invitation but has not accepted it
- **THEN** the corresponding `ProjectMembership` remains `INVITED` and confers no project access

### Requirement: Accepting an invitation
`pantheon-service` SHALL require the invited person to be authenticated as the invited account before accepting an invitation, and upon acceptance SHALL change the corresponding `ProjectMembership` from `INVITED` to `ACTIVE`.

#### Scenario: Invited user accepts
- **WHEN** a user authenticated as the invitation's account calls `POST /api/invitations/{token}/accept` for a valid, unexpired, not-yet-accepted invitation
- **THEN** `pantheon-service` sets the membership status to `ACTIVE`, records the acceptance time, and returns the project the user has joined

#### Scenario: Acceptance requires the matching account
- **WHEN** a request to accept an invitation is made without authentication, or authenticated as a user other than the invited account
- **THEN** `pantheon-service` rejects the request with HTTP 401 or HTTP 403 and the membership stays `INVITED`

#### Scenario: Accepting an already-accepted invitation
- **WHEN** the invited user calls accept on an invitation whose membership is already `ACTIVE`
- **THEN** `pantheon-service` responds successfully without changing anything further

#### Scenario: Existing account must still accept
- **WHEN** an email that already had an active account was invited and that user logs in
- **THEN** the user is not part of the project until they call the accept endpoint for their invitation

### Requirement: Invitation email delivery
`pantheon-service` SHALL publish a `team-invitation-created` event carrying the raw invitation token, the invited email, the project name, the inviter's name, and whether registration is required, so that `pantheon-message` can send the invitation email containing a link to the invitation.

#### Scenario: Event published on invitation
- **WHEN** an invitation is created for a project member
- **THEN** `pantheon-service` publishes a `team-invitation-created` event to RabbitMQ with the invited email, raw token, project name, inviter name, and registration-required flag

#### Scenario: Email links to the invitation
- **WHEN** `pantheon-message` consumes a `team-invitation-created` event
- **THEN** it sends an email to the invited address containing a link to the web invitation page for that token
