## MODIFIED Requirements

### Requirement: Offline support for Diário de Obra, Pedido de Compra, and Orçamentos
The mobile app SHALL let a user keep using Diário de Obra, Pedido de Compra (viewing and creating), and Orçamentos (viewing) while offline: a GET request that fails for connectivity reasons SHALL fall back to the last successfully cached response for that exact request, and a qualifying write SHALL be saved to a local outbox and sent automatically the next time connectivity is available, instead of failing. A qualifying write is creating a new Diário de Obra report, creating a new Pedido de Compra, or uploading a Diário de Obra photo or video — the cases where nothing else could already be showing the affected record. Every other mutation on an existing Diário de Obra report or Pedido de Compra (updating core fields, adding/removing a workforce entry, equipment usage, or activity, updating a media caption, uploading a document attachment, adding/removing a purchase-request item, submitting for approval, approving, rejecting, concluding, deleting) requires connectivity and fails outright when offline instead of queueing, since it mutates a record other screens may already be showing and queueing it risks the UI looking out of sync with the server.

Before saving a qualifying action to the outbox, the app SHALL show a confirmation dialog explaining that the action will be stored locally and sent once online, and let the user cancel. If connectivity drops between that confirmation and the actual request (so the action is queued despite the app believing it was online), the app SHALL instead show an acknowledgement dialog after the fact. A persistent, dismissible indicator SHALL be visible on every authenticated screen showing offline status and/or the number of pending outbox entries, linking to a sync screen that lists pending entries (with any last error), offers a manual "sync now" action, and lets the user discard a stuck entry.

Projetos, the Permissões configuration screen, and the Tasks board (viewing and mutating) SHALL be excluded from this offline support entirely — no response from these SHALL be cached, and no mutation from these SHALL be queued; each SHALL fail with a clear "requires connectivity" indication when offline instead.

#### Scenario: Diário de Obra list shows cached data offline
- **WHEN** a user who previously viewed an obra's Diário de Obra list opens it again while offline
- **THEN** the app shows the last-known list instead of an error

#### Scenario: Creating a report queues offline
- **WHEN** a user creates a new Diário de Obra report while offline (after confirming)
- **THEN** the app saves the creation to the outbox and sends it automatically once connectivity returns

#### Scenario: Photo upload queued offline
- **WHEN** a user attaches a photo to a draft Diário de Obra report while offline (after confirming)
- **THEN** the app saves the photo's local file path to the outbox and uploads it automatically once connectivity returns

#### Scenario: Editing an existing report offline requires connectivity
- **WHEN** a user tries to change an existing draft Diário de Obra report's core fields, or add/remove a workforce entry, equipment usage, or activity, while offline
- **THEN** the app blocks the action with a "requires connectivity" message rather than queueing it

#### Scenario: Submitting or deciding on a report offline requires connectivity
- **WHEN** a user tries to submit a Diário de Obra report for approval, or approve/reject a pending step on a Diário de Obra report or a Pedido de Compra, while offline
- **THEN** the app blocks the action with a "requires connectivity" message rather than queueing it

#### Scenario: Outbox drains automatically on reconnect
- **WHEN** connectivity returns after one or more actions were queued
- **THEN** the app automatically attempts to send every queued entry, removing each on success

#### Scenario: Tasks board does not work offline
- **WHEN** a user opens the Tasks board while offline, having never cached it
- **THEN** the app shows a load error rather than falling back to any cached data, since Tasks is excluded from offline support

#### Scenario: A stuck outbox entry can be discarded
- **WHEN** a user opens the sync screen and taps discard on a pending entry
- **THEN** the app removes it from the outbox without attempting it again

## ADDED Requirements

### Requirement: Screen data reflects the current session and does not go stale across navigation
Every screen's data-loading state SHALL be discarded when the user navigates away from that screen, so returning to it always triggers a fresh fetch (subject to the existing cache-fallback-when-offline behavior) rather than reusing a previous visit's result or error. Screen data-loading state SHALL also be reset on every successful login and on every logout, so that a fetch attempted before authentication completed, or a previous user's session on a shared device, can never be shown after a different login.

This requirement does not apply to app-wide session state that must persist for the whole authenticated session regardless of which screen is active (the outbox/sync status, the authenticated user's identity, the local offline database connection).

#### Scenario: Returning to a list after editing an item elsewhere shows the update
- **WHEN** a user opens a Diário de Obra report from its list, submits it for approval, and navigates back to the list
- **THEN** the list shows the report's new status, without requiring a manual pull-to-refresh

#### Scenario: Dashboard loads data right after login
- **WHEN** a user logs in successfully, whether or not any screen attempted to load data before the login completed
- **THEN** the post-login dashboard fetches and shows the current data rather than an empty or error state left over from before login

#### Scenario: A second user's login on the same device never shows the first user's data
- **WHEN** one user logs out on a shared device and a different user logs in
- **THEN** no screen shows data belonging to the previous user's session, even momentarily
