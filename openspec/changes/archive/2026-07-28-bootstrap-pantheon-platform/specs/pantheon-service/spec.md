## ADDED Requirements

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
