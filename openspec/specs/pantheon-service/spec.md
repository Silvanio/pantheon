# pantheon-service Specification

## Purpose

Core Spring Boot service for the Pantheon platform. Provides the REST API, JPA-backed persistence with Flyway migrations, OAuth2 login (Google) and email/password login unified behind a single session token format, RabbitMQ message publishing to `pantheon-message`, and an authenticated Server-Sent Events (SSE) endpoint for pushing real-time events to `pantheon-web`.

## Requirements

### Requirement: REST API
`pantheon-service` SHALL expose its capabilities as a REST API built with Spring Boot, returning JSON payloads and standard HTTP status codes.

#### Scenario: Health check reachable
- **WHEN** a client sends `GET /actuator/health` to `pantheon-service`
- **THEN** the service responds with HTTP 200 and a JSON body indicating status `UP`

### Requirement: JPA-backed persistence with Flyway migrations
`pantheon-service` SHALL persist its domain data (including user accounts) through Spring Data JPA against a relational database, with schema changes applied via versioned Flyway migrations.

#### Scenario: User record persisted on registration
- **WHEN** a new user completes registration (via OAuth2 or email/password)
- **THEN** a corresponding user record is persisted in the database and is retrievable on subsequent requests

#### Scenario: Flyway migrations applied on startup
- **WHEN** `pantheon-service` starts up against a database missing one or more migrations
- **THEN** Flyway applies the pending versioned migrations before the application finishes starting

### Requirement: OAuth2 login with Google
`pantheon-service` SHALL allow a user to authenticate using their Google account via OAuth2.

#### Scenario: Successful Google login
- **WHEN** a user completes the Google OAuth2 consent flow with valid credentials
- **THEN** `pantheon-service` creates or matches a local user record and issues a session token to the client

#### Scenario: Google login denied
- **WHEN** the OAuth2 provider returns an error or the user cancels consent
- **THEN** `pantheon-service` responds with an authentication failure and does not issue a token

### Requirement: Email and password login
`pantheon-service` SHALL allow a user to authenticate with an email address and password registered directly with the service.

#### Scenario: Successful email/password login
- **WHEN** a user submits a registered email and matching password to the login endpoint
- **THEN** `pantheon-service` issues a session token to the client

#### Scenario: Invalid credentials rejected
- **WHEN** a user submits an email/password pair that does not match a registered account
- **THEN** `pantheon-service` responds with HTTP 401 and does not issue a token

### Requirement: Unified session token issuance
Regardless of login method (Google OAuth2 or email/password), `pantheon-service` SHALL issue a single, self-contained session token format that authenticates subsequent REST and SSE requests.

#### Scenario: Token authenticates a protected REST endpoint
- **WHEN** a client presents a valid session token issued by either login method on a protected REST request
- **THEN** `pantheon-service` authorizes the request as the associated user

#### Scenario: Expired or invalid token rejected
- **WHEN** a client presents an expired or malformed session token
- **THEN** `pantheon-service` responds with HTTP 401

### Requirement: RabbitMQ message publishing
`pantheon-service` SHALL publish domain events to a RabbitMQ exchange so that `pantheon-message` can consume and process them asynchronously.

#### Scenario: Event published on qualifying action
- **WHEN** an action in `pantheon-service` that produces a domain event occurs
- **THEN** a message describing the event is published to the configured RabbitMQ exchange with a routing key identifying the event type

#### Scenario: Broker unavailable
- **WHEN** `pantheon-service` attempts to publish a message while RabbitMQ is unreachable
- **THEN** the service logs the failure and does not silently lose the triggering request's success response semantics (the failure is surfaced, not swallowed)

### Requirement: Server-Sent Events stream
`pantheon-service` SHALL expose an authenticated Server-Sent Events (SSE) endpoint that pushes real-time events to connected clients.

#### Scenario: Client subscribes to the event stream
- **WHEN** an authenticated client opens a connection to the SSE endpoint
- **THEN** `pantheon-service` keeps the connection open and pushes subsequent events to that client as they occur

#### Scenario: Unauthenticated subscription rejected
- **WHEN** a client attempts to open the SSE endpoint without a valid session token
- **THEN** `pantheon-service` responds with HTTP 401 and does not open the stream

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

### Requirement: Java package and build coordinate convention
`pantheon-service` SHALL use the `com.pantheon` Maven groupId and `com.pantheon.service` Java package prefix, and SHALL organize its Java source into layered packages (`controller`, `service`, `repository`, `entity`, `dto`, `exception`) rather than per-feature packages. Cross-cutting infrastructure (`security`, `messaging`, `sse`) is exempt from this layering and may remain organized by concern.

#### Scenario: Package prefix
- **WHEN** inspecting the package declaration of any Java source file under `pantheon-service`'s application code
- **THEN** the declaration starts with `com.pantheon.service`

#### Scenario: Layered organization
- **WHEN** inspecting the top-level packages under `com.pantheon.service`
- **THEN** Spring MVC controllers live in `controller`, business logic in `service`, Spring Data repositories in `repository`, JPA-mapped types (entities and their enums) in `entity`, request/response payload types in `dto`, and exception types together with their `@RestControllerAdvice` handlers in `exception`
