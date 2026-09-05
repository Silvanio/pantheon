## MODIFIED Requirements

### Requirement: Adding a member creates an invitation
When a company staff member with sufficient access adds someone to a company's staff or a construction site's team by email, `pantheon-service` SHALL create the corresponding membership (`CompanyMembership` or `SiteMembership`) in an `INVITED` state and SHALL NOT grant that person access until the invitation is accepted. This applies whether or not the email already belongs to a registered account, and regardless of which membership type is being invited to.

#### Scenario: Invitation created for an existing account
- **WHEN** an administrator adds a company staff member or a construction site's team member whose email belongs to an existing active account
- **THEN** `pantheon-service` creates the membership with status `INVITED`, creates a `MembershipInvitation` record tagged with the correct membership type, and publishes a `team-invitation-created` event

#### Scenario: Invitation created with pre-registration for an unknown email
- **WHEN** an administrator adds a company staff member or a construction site's team member whose email does not belong to any account
- **THEN** `pantheon-service` creates a pre-registration `AppUser` for that email with registration status `PENDING_REGISTRATION` and no credentials, creates an `INVITED` membership linked to it, creates a `MembershipInvitation` marked as requiring registration, and publishes a `team-invitation-created` event

#### Scenario: Invited person has no project access yet
- **WHEN** a user whose only link to a company or construction site is an `INVITED` membership attempts to read or act on it
- **THEN** `pantheon-service` rejects the request as if the user had no relationship to it

#### Scenario: Re-adding an already-invited email
- **WHEN** an administrator adds an email that already has an `INVITED` membership of the same type (company or a given site)
- **THEN** `pantheon-service` does not create a duplicate membership and re-issues the invitation for the existing one

#### Scenario: Re-adding an already-active member
- **WHEN** an administrator adds an email that already has an `ACTIVE` membership of the same type (company or a given site)
- **THEN** `pantheon-service` rejects the request with HTTP 409

### Requirement: Invitation token and lookup
`pantheon-service` SHALL identify each invitation by a single-use secret token delivered only in the invitation email, storing only a hash of the token, and SHALL expose an unauthenticated endpoint to retrieve an invitation's public details by token, regardless of whether it targets a company or a construction site.

#### Scenario: Valid token returns invitation details
- **WHEN** a client requests `GET /api/invitations/{token}` with a valid, unexpired token
- **THEN** `pantheon-service` returns the target name (company or construction site name), the inviter's name, the invited email, whether registration is required, and the invitation status

#### Scenario: Unknown or expired token rejected
- **WHEN** a client requests `GET /api/invitations/{token}` with a token that does not match any invitation or whose invitation has expired
- **THEN** `pantheon-service` responds with HTTP 404

### Requirement: Completing pre-registration from an invitation
`pantheon-service` SHALL allow the holder of an invitation token that requires registration to complete the pre-registration account by setting a password and display name, after which that account is a normal, log-in-capable account. Completing registration SHALL NOT by itself grant access to the invitation's target.

#### Scenario: Pending user completes registration
- **WHEN** the holder of a registration-required invitation token submits a password and display name to `POST /api/invitations/{token}/complete-registration`
- **THEN** `pantheon-service` sets the credentials on the pre-registration account, changes its registration status to `ACTIVE`, and returns a session token authenticating that user

#### Scenario: Registration not applicable
- **WHEN** the holder of an invitation token whose invited email already had an active account calls `POST /api/invitations/{token}/complete-registration`
- **THEN** `pantheon-service` rejects the request with HTTP 409

#### Scenario: Membership still pending after registration
- **WHEN** a pending user has completed registration from their invitation but has not accepted it
- **THEN** the corresponding membership remains `INVITED` and confers no access

### Requirement: Accepting an invitation
`pantheon-service` SHALL require the invited person to be authenticated as the invited account before accepting an invitation, and upon acceptance SHALL change the corresponding membership (company or construction site) from `INVITED` to `ACTIVE`.

#### Scenario: Invited user accepts
- **WHEN** a user authenticated as the invitation's account calls `POST /api/invitations/{token}/accept` for a valid, unexpired, not-yet-accepted invitation
- **THEN** `pantheon-service` sets the membership status to `ACTIVE`, records the acceptance time, and returns the company or construction site the user has joined

#### Scenario: Acceptance requires the matching account
- **WHEN** a request to accept an invitation is made without authentication, or authenticated as a user other than the invited account
- **THEN** `pantheon-service` rejects the request with HTTP 401 or HTTP 403 and the membership stays `INVITED`

#### Scenario: Accepting an already-accepted invitation
- **WHEN** the invited user calls accept on an invitation whose membership is already `ACTIVE`
- **THEN** `pantheon-service` responds successfully without changing anything further

#### Scenario: Existing account must still accept
- **WHEN** an email that already had an active account was invited and that user logs in
- **THEN** the user has no access to the invitation's target until they call the accept endpoint for their invitation

### Requirement: Invitation email delivery
`pantheon-service` SHALL publish a `team-invitation-created` event carrying the raw invitation token, the invited email, the target name (company or construction site), the inviter's name, and whether registration is required, so that `pantheon-message` can send the invitation email containing a link to the invitation.

#### Scenario: Event published on invitation
- **WHEN** an invitation is created for a company staff member or a construction site's team member
- **THEN** `pantheon-service` publishes a `team-invitation-created` event to RabbitMQ with the invited email, raw token, target name, inviter name, and registration-required flag

#### Scenario: Email links to the invitation
- **WHEN** `pantheon-message` consumes a `team-invitation-created` event
- **THEN** it sends an email to the invited address containing a link to the web invitation page for that token
