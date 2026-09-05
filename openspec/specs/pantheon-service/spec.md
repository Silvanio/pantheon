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
`pantheon-service` SHALL allow an authenticated user with no company to create a `Company` by supplying a company name. The creating user SHALL be recorded as that company's `ADMIN`. The company SHALL start with no plan assigned; plan selection and the company's commercial profile (Razão Social, Nome Fantasia, CNPJ, address, logo) are handled separately (see `company-onboarding`, `company-plan-catalog`).

#### Scenario: Project created successfully
- **WHEN** an authenticated user with no company submits a company name
- **THEN** `pantheon-service` persists a new `Company` (identified by the given name) with no plan and no profile fields set, and creates a `CompanyMembership` linking the creator to the company with role `ADMIN`

#### Scenario: Project count limit blocks creation
- **WHEN** an authenticated user creates a company
- **THEN** `pantheon-service` does not limit how many companies a user may create or administer — the plan-tied limit instead applies to how many active construction sites a company may have (see `company-plan-catalog`'s "Active construction-site limit enforcement")

#### Scenario: User with an existing company is not offered creation again
- **WHEN** an authenticated user who already administers or belongs to a company attempts to create another company
- **THEN** `pantheon-service` still allows it (a user may administer more than one company), recording the new company independently with its own plan and profile state

### Requirement: List administered and joined projects
`pantheon-service` SHALL allow an authenticated user to retrieve the list of companies they belong to, along with their role on each and each company's onboarding status.

#### Scenario: User lists their projects
- **WHEN** an authenticated user requests their list of companies
- **THEN** `pantheon-service` returns every company for which the user has a `CompanyMembership`, including their role (`ADMIN` or `MEMBER`) and onboarding status on each

### Requirement: Java package and build coordinate convention
`pantheon-service` SHALL use the `com.pantheon` Maven groupId and `com.pantheon.service` Java package prefix, and SHALL organize its Java source into layered packages (`controller`, `service`, `repository`, `entity`, `dto`, `exception`) rather than per-feature packages. Cross-cutting infrastructure (`security`, `messaging`, `sse`) is exempt from this layering and may remain organized by concern.

#### Scenario: Package prefix
- **WHEN** inspecting the package declaration of any Java source file under `pantheon-service`'s application code
- **THEN** the declaration starts with `com.pantheon.service`

#### Scenario: Layered organization
- **WHEN** inspecting the top-level packages under `com.pantheon.service`
- **THEN** Spring MVC controllers live in `controller`, business logic in `service`, Spring Data repositories in `repository`, JPA-mapped types (entities and their enums) in `entity`, request/response payload types in `dto`, and exception types together with their `@RestControllerAdvice` handlers in `exception`

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

