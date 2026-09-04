## ADDED Requirements

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
