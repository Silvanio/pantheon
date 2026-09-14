## MODIFIED Requirements

### Requirement: Server-Sent Events client
`pantheon-web` SHALL connect to `pantheon-service`'s SSE endpoint and reflect received events in the UI in real time, reconnecting automatically if the connection drops. `pantheon-web` SHALL support reacting to a specific event with application logic that updates local state in place, not only logging the event for display.

#### Scenario: Real-time event received
- **WHEN** `pantheon-service` pushes an event over the SSE stream while a user has `pantheon-web` open
- **THEN** `pantheon-web` receives the event and updates the relevant UI state without a manual page refresh

#### Scenario: Connection drop triggers reconnect
- **WHEN** the SSE connection between `pantheon-web` and `pantheon-service` is interrupted
- **THEN** `pantheon-web` automatically attempts to reconnect rather than leaving the stream permanently closed

#### Scenario: Task board reflects a card moved elsewhere
- **WHEN** `pantheon-web` has an obra's Tasks board open and receives a `task-card-moved` event for that same obra
- **THEN** `pantheon-web` moves the affected card to its new column and position in the board's local state, without refetching the whole board

#### Scenario: Task board ignores a move from a different obra
- **WHEN** `pantheon-web` has one obra's Tasks board open and receives a `task-card-moved` event for a different obra
- **THEN** `pantheon-web` does not modify the currently displayed board
