## MODIFIED Requirements

### Requirement: Default permissions by function
`pantheon-service` SHALL apply, for any construction site with no explicit permission configuration, a default access level per `SiteMembership` function for each capability (`DOCUMENT_PROJECTS`, `DAILY_REPORT`, `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`, `TEAM_MANAGE`): company staff have `MANAGE` on every capability; `ENGINEER` and `ARCHITECT` have `MANAGE` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, `TASKS`, and `TEAM_MANAGE`, and `VIEW` on `EQUIPMENT`; `SITE_FOREMAN` has `MANAGE` on `EQUIPMENT`, `DAILY_REPORT`, and `TASKS`, and `VIEW` on `DOCUMENT_PROJECTS`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`, and `TEAM_MANAGE`; `CLIENT` has `VIEW` on `DOCUMENT_PROJECTS`, `DAILY_REPORT`, `TASKS`, `TEAM_MANAGE`, and `ORCAMENTO_MANAGE`, and `VIEW_AND_APPROVE` on `PURCHASE_REQUEST`; `SERVICE_PROVIDER` has `VIEW` on `DAILY_REPORT`, `TASKS`, `TEAM_MANAGE`, and `PURCHASE_REQUEST` only. None of these defaults is ever `HIDDEN` — a member only ends up hidden from a capability via an explicit function- or member-level override. `VIEW_AND_APPROVE` is a meaningful default only for `PURCHASE_REQUEST`; see `purchase-request-approval-workflow` for how it governs both visibility and approval authority for that capability.

#### Scenario: Fresh site uses defaults
- **WHEN** a construction site with no permission overrides is queried for an engineer's access to `DAILY_REPORT`
- **THEN** `pantheon-service` reports `MANAGE`, matching the default for `ENGINEER`

#### Scenario: Client gets view-and-approve access to purchase requests by default
- **WHEN** a construction site with no permission overrides is queried for a client's access to `PURCHASE_REQUEST`
- **THEN** `pantheon-service` reports `VIEW_AND_APPROVE`, which cannot create or submit a Pedido de Compra but can approve or reject a step matching the client's function

#### Scenario: Service provider can view but not manage tasks by default
- **WHEN** a construction site with no permission overrides is queried for a service provider's access to `TASKS`
- **THEN** `pantheon-service` reports `VIEW`, allowing the service provider to see the task board without creating or moving cards

### Requirement: Permission enforcement on purchase requests and orçamento management
`pantheon-service` SHALL reject creating or converting a `PurchaseRequestItem`, or submitting a Pedido de Compra for approval, from a member whose resolved `PURCHASE_REQUEST` access is not `MANAGE`, and reject creating, editing, or submitting an `Orcamento` from a member without `MANAGE` `ORCAMENTO_MANAGE` access, regardless of their function. A member whose resolved access to either capability is `HIDDEN` SHALL also be rejected from reading that capability's records. A member whose resolved `PURCHASE_REQUEST` access is `VIEW` SHALL always be able to read every Pedido de Compra on the site; a member whose resolved `PURCHASE_REQUEST` access is `VIEW_AND_APPROVE` SHALL only be able to read a Pedido de Compra that is visible to them per `purchase-request-approval-workflow`'s dynamic-visibility rule.

#### Scenario: Member without purchase-request access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a purchase-request item
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: View-and-approve member cannot create a purchase request
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `VIEW_AND_APPROVE` attempts to create a purchase-request item or submit a Pedido de Compra for approval
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Member without orçamento-management access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create or submit an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: Hidden member cannot read purchase requests or orçamentos
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` or `ORCAMENTO_MANAGE` access is `HIDDEN` requests that capability's records
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: View member sees every purchase request unconditionally
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is `VIEW` requests the site's Pedido de Compra list
- **THEN** `pantheon-service` returns every Pedido de Compra on the site, regardless of the member's involvement in any approval step
