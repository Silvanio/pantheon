## ADDED Requirements

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
