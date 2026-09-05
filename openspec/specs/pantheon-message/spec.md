# pantheon-message Specification

## Purpose

Spring Boot messaging worker for the Pantheon platform. Provides a REST API, JPA-backed persistence, API-token authentication for inbound service-to-service calls from `pantheon-service`, and a RabbitMQ consumer that processes messages published by `pantheon-service`.
## Requirements
### Requirement: REST API
`pantheon-message` SHALL expose its capabilities as a REST API built with Spring Boot, returning JSON payloads and standard HTTP status codes.

#### Scenario: Health check reachable
- **WHEN** a client sends `GET /actuator/health` to `pantheon-message`
- **THEN** the service responds with HTTP 200 and a JSON body indicating status `UP`

### Requirement: JPA-backed persistence
`pantheon-message` SHALL persist processed message records through Spring Data JPA against a relational database, with schema changes applied via versioned migrations.

#### Scenario: Consumed message persisted
- **WHEN** `pantheon-message` successfully processes a message consumed from RabbitMQ
- **THEN** a corresponding record is persisted in the database and is retrievable via the REST API

### Requirement: API-token authentication for inbound requests
`pantheon-message` SHALL require a valid pre-shared API token on every REST request originating from `pantheon-service`, rejecting requests that omit or present an invalid token.

#### Scenario: Valid API token accepted
- **WHEN** `pantheon-service` sends a REST request to `pantheon-message` with a valid API token in the request header
- **THEN** `pantheon-message` processes the request normally

#### Scenario: Missing or invalid API token rejected
- **WHEN** a request to a protected endpoint on `pantheon-message` omits the API token header or presents an unrecognized token
- **THEN** `pantheon-message` responds with HTTP 401 and does not process the request

### Requirement: RabbitMQ message consumption
`pantheon-message` SHALL consume messages published by `pantheon-service` from its bound RabbitMQ queue and process each message according to its declared type.

#### Scenario: Message consumed and processed
- **WHEN** a message is published to a routing key that `pantheon-message`'s queue is bound to
- **THEN** `pantheon-message` receives the message, processes it, and acknowledges it to RabbitMQ on success

#### Scenario: Processing failure is not silently dropped
- **WHEN** `pantheon-message` fails to process a consumed message
- **THEN** the message is not acknowledged as successfully processed, so it can be retried or routed to a dead-letter queue rather than being lost

### Requirement: Java package and build coordinate convention
`pantheon-message` SHALL use the `com.pantheon` Maven groupId and `com.pantheon.message` Java package prefix, consistent with `pantheon-service` and the shared parent POM.

#### Scenario: Package prefix
- **WHEN** inspecting the package declaration of any Java source file under `pantheon-message`
- **THEN** the declaration starts with `com.pantheon.message`

### Requirement: Transactional email delivery
`pantheon-message` SHALL send transactional email via SMTP in response to events consumed from RabbitMQ that call for a notification, using SMTP connection settings and a sender address supplied through externalized environment variables rather than hardcoded values.

#### Scenario: Invitation email sent
- **WHEN** `pantheon-message` consumes a `team-invitation-created` event
- **THEN** it sends an email to the invited address whose body contains a link to the web invitation page built from the configured web base URL and the event's token, and it persists the processed-message record for the event

#### Scenario: SMTP configuration externalized
- **WHEN** `pantheon-message`'s runtime configuration is inspected
- **THEN** its SMTP host, port, credentials, sender address, and the web base URL used for links are supplied via environment variables, not hardcoded in the image or source

#### Scenario: Email send failure is not silently dropped
- **WHEN** `pantheon-message` fails to send the email for a consumed event
- **THEN** the message is not acknowledged as successfully processed, so it can be retried or routed to the dead-letter queue rather than being lost

