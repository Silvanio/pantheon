## MODIFIED Requirements

### Requirement: Purchase-request list, detail, and approval actions
The mobile app SHALL list an obra's purchase requests with status and stage, show a request's detail (items, approval steps, linked orçamentos), and allow creating a new purchase request, submitting it for approval, approving/rejecting the current pending step, and concluding an approved request — each gated by the same resolved-permission and status preconditions as `pantheon-web` (create/submit: `MANAGE`; approve/reject: the acting user's site function must match the current cycle's lowest-step-order `PENDING` step's approver function, or — only when the user holds no `SiteMembership` on that site at all — `MANAGE` specifically; conclude: `MANAGE` or `VIEW_AND_APPROVE`). The app SHALL only hide actions the current user's resolved permissions do not allow, not attempt its own authorization logic beyond this client-side gating; the backend remains the authority. Submit/approve/reject/conclude SHALL require an active connection (see the offline-support requirement's exclusions) — viewing and creating a purchase request SHALL work offline.

#### Scenario: Approving a pending step
- **WHEN** a user whose site function matches the current cycle's lowest-step-order pending step, and whose resolved access is `MANAGE` or `VIEW_AND_APPROVE`, taps "Aprovar"
- **THEN** the app calls the existing approve-step endpoint and refreshes the request's status

#### Scenario: Multiple steps pending at once resolves to the actionable one
- **WHEN** a purchase request's current approval cycle has more than one step in `PENDING` status simultaneously
- **THEN** the app treats the lowest step-order `PENDING` step within that cycle as the actionable one for showing/hiding approve/reject, not any other pending step

#### Scenario: Creating a purchase request offline
- **WHEN** a user with `MANAGE` access creates a new purchase request while offline
- **THEN** the app saves it to the local outbox (after the offline-confirmation dialog) and sends it automatically once connectivity returns

#### Scenario: Approving while offline is blocked
- **WHEN** a user attempts to approve, reject, submit, or conclude a purchase request while offline
- **THEN** the app blocks the action with an explanatory dialog instead of queueing it

## ADDED Requirements

### Requirement: Permission-gated UI matches pantheon-web exactly
Every permission-gated screen, entry point, and action in the mobile app SHALL match `pantheon-web`'s resolved-access-level and role-based gating exactly — the same information SHALL be visible, and the same actions SHALL be available, to a given user on both platforms. In particular, the "Permissões" obra-entry card SHALL be gated by the caller's company-admin role for that site's company (not shown otherwise), matching `pantheon-web`'s `isCompanyAdmin` gate on its equivalent tab.

#### Scenario: Non-admin does not see Permissões
- **WHEN** a user who is not an admin of the obra's company opens the obra home screen
- **THEN** the "Permissões" entry card is not shown, matching `pantheon-web`'s behavior for the same user

#### Scenario: Admin sees Permissões
- **WHEN** a user who is an admin of the obra's company opens the obra home screen
- **THEN** the "Permissões" entry card is shown

### Requirement: Offline support for Diário de Obra, Pedido de Compra, and Orçamentos
The mobile app SHALL let a user keep using Diário de Obra, Pedido de Compra (viewing and creating), and Orçamentos (viewing) while offline: a GET request that fails for connectivity reasons SHALL fall back to the last successfully cached response for that exact request, and a qualifying write (create/submit a Diário de Obra report, upload a report photo, create a Pedido de Compra) SHALL be saved to a local outbox and sent automatically the next time connectivity is available, instead of failing.

Before saving a qualifying action to the outbox, the app SHALL show a confirmation dialog explaining that the action will be stored locally and sent once online, and let the user cancel. If connectivity drops between that confirmation and the actual request (so the action is queued despite the app believing it was online), the app SHALL instead show an acknowledgement dialog after the fact. A persistent, dismissible indicator SHALL be visible on every authenticated screen showing offline status and/or the number of pending outbox entries, linking to a sync screen that lists pending entries (with any last error), offers a manual "sync now" action, and lets the user discard a stuck entry.

Projetos, the Permissões configuration screen, the Tasks board (viewing and mutating), and the Pedido de Compra approval workflow (submit/approve/reject/conclude) SHALL be excluded from this offline support entirely — no response from these SHALL be cached, and no mutation from these SHALL be queued; each SHALL fail with a clear "requires connectivity" indication when offline instead.

#### Scenario: Diário de Obra list shows cached data offline
- **WHEN** a user who previously viewed an obra's Diário de Obra list opens it again while offline
- **THEN** the app shows the last-known list instead of an error

#### Scenario: Photo upload queued offline
- **WHEN** a user attaches a photo to a draft Diário de Obra report while offline (after confirming)
- **THEN** the app saves the photo's local file path to the outbox and uploads it automatically once connectivity returns

#### Scenario: Outbox drains automatically on reconnect
- **WHEN** connectivity returns after one or more actions were queued
- **THEN** the app automatically attempts to send every queued entry, removing each on success

#### Scenario: Tasks board does not work offline
- **WHEN** a user opens the Tasks board while offline, having never cached it
- **THEN** the app shows a load error rather than falling back to any cached data, since Tasks is excluded from offline support

#### Scenario: A stuck outbox entry can be discarded
- **WHEN** a user opens the sync screen and taps discard on a pending entry
- **THEN** the app removes it from the outbox without attempting it again
