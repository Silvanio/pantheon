## ADDED Requirements

### Requirement: Pedido de Compra header creation
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to create a `PurchaseRequest` (Pedido de Compra) for that site consisting of one or more `PurchaseRequestItem`s submitted together, each with a free-text name, an optional type, a quantity, and an optional unit of measure, all starting in `PENDING` status. `pantheon-service` SHALL generate the `PurchaseRequest`'s name as `"Pedido " + <creation date as dd/MM/yyyy> + " #" + <sequence>`, where `<sequence>` is one more than the number of `PurchaseRequest`s already created for that site on the same calendar day.

#### Scenario: First Pedido de Compra of the day
- **WHEN** a construction site member with `PURCHASE_REQUEST` access creates a Pedido de Compra with two items, and no other Pedido de Compra exists for that site that day
- **THEN** `pantheon-service` persists a new `PurchaseRequest` named "Pedido \<today's date> #1" with both items in `PENDING` status

#### Scenario: Second Pedido de Compra of the same day
- **WHEN** a construction site member creates a second Pedido de Compra on a site that already has one Pedido de Compra created that same day
- **THEN** `pantheon-service` names the new `PurchaseRequest` "Pedido \<today's date> #2"

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Pedido de Compra listing and filtering
`pantheon-service` SHALL allow any member of a construction site to list its `PurchaseRequest` headers, optionally filtered to those created on a given calendar date, and to list the `PurchaseRequestItem`s of a specific header, optionally filtered by status (`PENDING` or `CONVERTED`).

#### Scenario: Member lists Pedidos de Compra for a given date
- **WHEN** an authenticated member of a construction site requests its Pedidos de Compra filtered to a specific date
- **THEN** `pantheon-service` returns only the `PurchaseRequest` headers created on that site on that date

#### Scenario: Member lists a header's pending items
- **WHEN** an authenticated member requests a specific Pedido de Compra's items filtered to `PENDING`
- **THEN** `pantheon-service` returns every item of that header that has not yet been converted into an Orçamento

## REMOVED Requirements

### Requirement: Purchase-request item creation
**Reason**: Items are no longer created standalone — they are always created together as one `PurchaseRequest` header. See this capability's new "Pedido de Compra header creation" requirement.
**Migration**: Replace any standalone item-creation call with a Pedido de Compra creation call carrying one or more items.

### Requirement: Purchase-request item listing
**Reason**: Replaced by header-scoped listing plus a date filter. See this capability's new "Pedido de Compra listing and filtering" requirement.
**Migration**: List `PurchaseRequest` headers first, then list a given header's items.

## MODIFIED Requirements

### Requirement: Converting purchase-request items into an Orçamento
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to select one or more `PENDING` `PurchaseRequestItem`s belonging to the same `PurchaseRequest` header and create a new `Orcamento` whose line items are copied from the selected items' name, type, and quantity and whose `sourcePurchaseRequestId` is set to that header's id. Converted items SHALL transition to `CONVERTED` status and record which Orçamento they were converted into; items of that header not selected SHALL remain `PENDING` and available for a later conversion into a different Orçamento.

#### Scenario: Selected items become a new Orçamento
- **WHEN** a construction site member selects two `PENDING` items from the same Pedido de Compra and chooses "Criar orçamento"
- **THEN** `pantheon-service` creates a new `Orcamento` in `DRAFT` status linked to that Pedido de Compra, with two line items copied from the selected items, and marks both items `CONVERTED`

#### Scenario: Same Pedido de Compra spawns a second Orçamento later
- **WHEN** a construction site member converts a first subset of a Pedido de Compra's items into an Orçamento, then later selects the remaining `PENDING` items of that same Pedido de Compra and converts them
- **THEN** `pantheon-service` creates a second `Orcamento`, also linked to that same Pedido de Compra, distinct from the first

#### Scenario: Cannot select items from more than one Pedido de Compra at once
- **WHEN** a construction site member attempts to convert a selection of `PurchaseRequestItem`s spanning more than one `PurchaseRequest` header in a single action
- **THEN** `pantheon-service` rejects the request

#### Scenario: Already-converted items cannot be converted again
- **WHEN** a construction site member attempts to convert a `PurchaseRequestItem` that is already `CONVERTED`
- **THEN** `pantheon-service` rejects the request

### Requirement: Purchase-request views
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a view to list Pedidos de Compra (filterable by date), open one to see its pending and converted items, add new items as a new Pedido de Compra, select one or more of a header's pending items, and convert the selection into a new Orçamento, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member creates a Pedido de Compra from the UI
- **WHEN** a construction site member fills in one or more items in the Pedido de Compra creation form and submits it
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new, auto-named Pedido de Compra in the site's list

#### Scenario: Member filters Pedidos de Compra by date
- **WHEN** a construction site member selects a date in the Pedido de Compra list's date filter
- **THEN** `pantheon-web` shows only the Pedidos de Compra created on that site on that date

#### Scenario: Member converts a selection from the UI
- **WHEN** a construction site member opens a Pedido de Compra, checks one or more of its pending items, and clicks "Criar orçamento"
- **THEN** `pantheon-web` submits the selection to `pantheon-service`, navigates to the newly created Orçamento, and removes the converted items from the pending list while leaving the Pedido de Compra open for any remaining pending items
