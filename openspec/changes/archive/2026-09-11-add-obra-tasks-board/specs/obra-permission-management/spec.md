## MODIFIED Requirements

### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, and `TASKS`, and `VIEW` on `EQUIPMENT`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT`, `DAILY_REPORT`, and `TASKS`, and `VIEW` on `DOCUMENT_PROJECTS`, `PURCHASE_REQUEST`, and `ORCAMENTO_MANAGE`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, and `TASKS`, with no manage access on any material/orçamento capability; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT` and `TASKS` only. (Authority to act on a specific Orçamento approval step is a separate, structural concern — see `orcamento-approval-workflow` — not one of these capabilities.)

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client has no material access by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `PURCHASE_REQUEST`
- **THEN** `pantheon-service` reports that the client cannot create purchase-request items

#### Scenario: Service provider can view but not manage tasks by default
- **WHEN** a construction site with no permission overrides is queried for a service provider's access to `TASKS`
- **THEN** `pantheon-service` reports `VIEW`, allowing the service provider to see the task board without creating or moving cards

## ADDED Requirements

### Requirement: Permission enforcement on task board actions
`pantheon-service` SHALL reject a create-card, move-card, create-label, attach-label, or add-comment action on an obra's task board from a member whose resolved `TASKS` access level is `VIEW`, while still allowing that member to view the board's columns, cards, labels, and comments.

#### Scenario: View-only member blocked from creating a card
- **WHEN** a construction site member whose resolved `TASKS` access is `VIEW` attempts to create a task card
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: View-only member can still view the board
- **WHEN** a construction site member whose resolved `TASKS` access is `VIEW` requests the obra's task board
- **THEN** `pantheon-service` returns the columns, cards, labels, and comments
