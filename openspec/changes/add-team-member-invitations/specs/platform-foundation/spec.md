## MODIFIED Requirements

### Requirement: Local development orchestration
The repository SHALL provide a local orchestration setup (e.g., docker-compose) that runs RabbitMQ, the required databases, an SMTP server for outbound email, and all three services together for end-to-end local development and testing.

#### Scenario: Full stack starts locally
- **WHEN** a developer runs the local orchestration setup
- **THEN** RabbitMQ, the databases, the SMTP server, `pantheon-service`, `pantheon-message`, and `pantheon-web` all start and can communicate with each other

#### Scenario: Outbound email captured locally
- **WHEN** the local stack is running and a service sends an email
- **THEN** the SMTP server accepts it and the message is inspectable by the developer (e.g., via a local mail UI) rather than being delivered to a real inbox
