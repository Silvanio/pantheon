## ADDED Requirements

### Requirement: User profile registration
`pantheon-service` SHALL allow an authenticated user to record their CNPJ/CPF, legal name (Nome/Razão Social), address, and postal code (CEP) as their own profile data, independent of any specific project. Submitting this data again SHALL update the user's existing profile rather than creating a duplicate.

#### Scenario: Profile created on first submission
- **WHEN** an authenticated user with no existing profile submits CNPJ/CPF, legal name, address, and CEP
- **THEN** `pantheon-service` persists a new profile record associated with that user

#### Scenario: Profile updated on subsequent submission
- **WHEN** an authenticated user who already has a profile submits CNPJ/CPF, legal name, address, and CEP again
- **THEN** `pantheon-service` updates the existing profile record for that user rather than creating a second one

### Requirement: Project creation
`pantheon-service` SHALL allow an authenticated user to create a `Project` by supplying a project name, together with their CNPJ/CPF, legal name, address, and postal code (recorded as that user's profile data, not as fields of the project). The creating user SHALL be recorded as that project's `ADMIN`, and the project SHALL start in trial status with no plan assigned.

#### Scenario: Project created successfully
- **WHEN** an authenticated user submits a project name along with their CNPJ/CPF, legal name, address, and CEP
- **THEN** `pantheon-service` persists a new `Project` (identified by the given name) with `plan` unset and a trial period starting now, records the submitted CNPJ/CPF, legal name, address, and CEP as the user's profile data, and creates a `ProjectMembership` linking the creator to the project with role `ADMIN`

#### Scenario: Project count limit blocks creation
- **WHEN** an authenticated user who administers one or more projects on a given confirmed plan attempts to create a new project that would exceed that plan's project limit (Basic: 2, Pro: 10)
- **THEN** `pantheon-service` rejects the request and does not create the project

### Requirement: Trial period
Each `Project` SHALL have a trial period of 3 days starting at its creation time, during which it is considered active without requiring a plan.

#### Scenario: Project active during trial
- **WHEN** the onboarding/status check for a project runs before 3 days have elapsed since its creation
- **THEN** `pantheon-service` reports the project as active and does not require plan selection

#### Scenario: Project trial expired
- **WHEN** the onboarding/status check for a project runs after 3 days have elapsed since its creation and no plan has been confirmed
- **THEN** `pantheon-service` reports the project as requiring plan selection

### Requirement: Plan confirmation
`pantheon-service` SHALL allow the administrator of a project to confirm one of three plans — Basic (up to 2 projects), Pro (up to 10 projects), or Ilimitado (unlimited projects) — for that project. Confirming a plan SHALL set the plan's validity to 1 year from the confirmation time.

#### Scenario: Admin confirms a plan
- **WHEN** the administrator of a project whose trial or previously confirmed plan has expired submits a plan choice (Basic, Pro, or Ilimitado)
- **THEN** `pantheon-service` records the chosen plan on the project and sets its validity to 1 year from the confirmation time, after which the project is reported as active again

#### Scenario: Non-admin cannot confirm a plan
- **WHEN** a user who is not the administrator of a project attempts to confirm a plan for it
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Confirmed plan expires after 1 year
- **WHEN** the onboarding/status check for a project runs after its confirmed plan's 1-year validity has elapsed
- **THEN** `pantheon-service` reports the project as requiring plan selection again

### Requirement: Onboarding status
`pantheon-service` SHALL expose an authenticated endpoint that reports whether the current user has any project membership, whether any of their memberships is on an active (non-expired trial or plan) project, and whether plan selection is required.

#### Scenario: User with no projects
- **WHEN** an authenticated user with zero project memberships requests their onboarding status
- **THEN** `pantheon-service` reports that the user has no project

#### Scenario: User with at least one active project
- **WHEN** an authenticated user has at least one project membership on an active project
- **THEN** `pantheon-service` reports the user as having an active project and does not require plan selection

#### Scenario: User whose only projects are expired
- **WHEN** an authenticated user has one or more project memberships but none of the associated projects are active
- **THEN** `pantheon-service` reports that plan selection is required, identifying the expired project(s) the user administers

### Requirement: Project membership management
`pantheon-service` SHALL allow the administrator of a project to add another registered user to that project as a `MEMBER`.

#### Scenario: Admin adds a member
- **WHEN** the administrator of a project submits a request to add a registered user to that project
- **THEN** `pantheon-service` creates a `ProjectMembership` linking that user to the project with role `MEMBER`

#### Scenario: Non-admin cannot add a member
- **WHEN** a user who is not the administrator of a project attempts to add another user to it
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: List administered and joined projects
`pantheon-service` SHALL allow an authenticated user to retrieve the list of projects they belong to, along with their role on each.

#### Scenario: User lists their projects
- **WHEN** an authenticated user requests their list of projects
- **THEN** `pantheon-service` returns every project for which the user has a `ProjectMembership`, including their role (`ADMIN` or `MEMBER`) on each
