## MODIFIED Requirements

### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`, `SCHEDULE`, `TEAM_MANAGE`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`, `SCHEDULE`, and `TEAM_MANAGE`, and `VIEW` on `EQUIPMENT`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT`, `DAILY_REPORT`, `TASKS`, and `SCHEDULE`, and `VIEW` on `DOCUMENT_PROJECTS`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, and `TEAM_MANAGE`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `TASKS`, `SCHEDULE`, `TEAM_MANAGE`, and `ORCAMENTO_MANAGE`, and `VIEW_AND_APPROVE` on `PURCHASE_REQUEST`; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT`, `TASKS`, `SCHEDULE`, `TEAM_MANAGE`, and `PURCHASE_REQUEST` only. None of these defaults is ever `HIDDEN` — a member only ends up hidden from a capability via an explicit function- or member-level override. `VIEW_AND_APPROVE` is a meaningful default only for `PURCHASE_REQUEST`; see `purchase-request-approval-workflow` for how it governs both visibility and approval authority for that capability.

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client gets view-and-approve access to purchase requests by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `PURCHASE_REQUEST`
- **THEN** `pantheon-service` reports `VIEW_AND_APPROVE`, which cannot create or submit a Pedido de Compra but can approve or reject a step matching the client's function

#### Scenario: Service provider can view but not manage tasks by default
- **WHEN** a construction site with no permission overrides is queried for a service provider's access to `TASKS`
- **THEN** `pantheon-service` reports `VIEW`, allowing the service provider to see the task board without creating or moving cards

#### Scenario: Site foreman can manage the schedule by default
- **WHEN** a construction site with no permission overrides is queried for a site foreman's access to `SCHEDULE`
- **THEN** `pantheon-service` reports `MANAGE`, allowing the foreman to create and edit stages and tasks on the Cronograma tab

#### Scenario: Client can view but not manage the schedule by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `SCHEDULE`
- **THEN** `pantheon-service` reports `VIEW`, allowing the client to see the Cronograma tab read-only
