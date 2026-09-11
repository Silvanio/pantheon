# purchase-requests Specification

## Purpose
TBD - created by archiving change restructure-materials-orcamento-flow. Update Purpose after archive.
## Requirements
### Requirement: Purchase-request item creation
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to add a `PurchaseRequestItem` to that site's Pedido de Compra list, recording a free-text name, an optional type, a quantity, and an optional unit of measure, starting in `PENDING` status. No reference to any material catalog is required or accepted.

#### Scenario: Member adds a purchase-request item
- **WHEN** a construction site member with `PURCHASE_REQUEST` access submits a name, optional type, quantity, and optional unit for a material the site needs to buy
- **THEN** `pantheon-service` persists a new `PurchaseRequestItem` in `PENDING` status linked to that site

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to add a purchase-request item
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Purchase-request item listing
`pantheon-service` SHALL allow any member of a construction site to list its `PurchaseRequestItem`s, optionally filtered by status (`PENDING` or `CONVERTED`).

#### Scenario: Member lists pending items
- **WHEN** an authenticated member of a construction site requests its purchase-request items filtered to `PENDING`
- **THEN** `pantheon-service` returns every `PurchaseRequestItem` on that site that has not yet been converted into an Orçamento

### Requirement: Converting purchase-request items into an Orçamento
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to select one or more `PENDING` `PurchaseRequestItem`s on a site and create a new `Orcamento` whose line items are copied from the selected items' name, type, and quantity. Converted items SHALL transition to `CONVERTED` status and record which Orçamento they were converted into.

#### Scenario: Selected items become a new Orçamento
- **WHEN** a construction site member selects two `PENDING` purchase-request items and chooses "Criar orçamento"
- **THEN** `pantheon-service` creates a new `Orcamento` in `DRAFT` status with two line items copied from the selected items, and marks both items `CONVERTED`

#### Scenario: Already-converted items cannot be converted again
- **WHEN** a construction site member attempts to convert a `PurchaseRequestItem` that is already `CONVERTED`
- **THEN** `pantheon-service` rejects the request

### Requirement: Purchase-request views
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a view to add purchase-request items, list pending and converted items, select one or more pending items, and convert the selection into a new Orçamento, with all copy sourced from the `pt-BR` locale resource file.

#### Scenario: Member adds an item from the UI
- **WHEN** a construction site member fills in the purchase-request item form and submits it
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new item in the site's Pedido de Compra list

#### Scenario: Member converts a selection from the UI
- **WHEN** a construction site member checks one or more pending items and clicks "Criar orçamento"
- **THEN** `pantheon-web` submits the selection to `pantheon-service`, navigates to the newly created Orçamento, and removes the converted items from the pending list

