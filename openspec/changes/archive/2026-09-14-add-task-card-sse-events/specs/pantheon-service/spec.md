## MODIFIED Requirements

### Requirement: Server-Sent Events stream
`pantheon-service` SHALL expose an authenticated Server-Sent Events (SSE) endpoint that pushes real-time events to connected clients. Company-scoped events SHALL only be delivered to a connected client belonging to that company. The stream's behavior SHALL be identical regardless of how many `pantheon-service` instances are running or which instance produced a given event, achieved by fanning every SSE-worthy event out to all instances via RabbitMQ before local delivery.

#### Scenario: Client subscribes to the event stream
- **WHEN** an authenticated client opens a connection to the SSE endpoint
- **THEN** `pantheon-service` keeps the connection open and pushes subsequent events to that client as they occur

#### Scenario: Unauthenticated subscription rejected
- **WHEN** a client attempts to open the SSE endpoint without a valid session token
- **THEN** `pantheon-service` responds with HTTP 401 and does not open the stream

#### Scenario: Company-scoped event reaches only that company's clients
- **WHEN** `pantheon-service` emits an event scoped to a given company
- **THEN** only clients whose connected user belongs to that company receive the event; clients belonging only to other companies do not

#### Scenario: Event reaches a client connected to a different instance
- **WHEN** an action on one `pantheon-service` instance produces an event, and the intended recipient's SSE connection is open on a different instance
- **THEN** the recipient still receives the event in real time

#### Scenario: Idle connection kept alive
- **WHEN** an SSE connection has had no application event to deliver for an extended period
- **THEN** `pantheon-service` periodically sends a keep-alive frame on that connection so intermediary proxies or load balancers do not close it as idle
