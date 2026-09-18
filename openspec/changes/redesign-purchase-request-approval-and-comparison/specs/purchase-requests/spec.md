## MODIFIED Requirements

### Requirement: Pedido de Compra header creation
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to create a `PurchaseRequest` (Pedido de Compra) for that site consisting of one or more `PurchaseRequestItem`s submitted together, each with a free-text name, an optional type, a quantity, and an optional unit of measure, all starting in `PENDING` status. `pantheon-service` SHALL generate the `PurchaseRequest`'s name as `"Pedido " + <creation date as dd/MM/yyyy> + " #" + <sequence>`, where `<sequence>` is one more than the number of `PurchaseRequest`s already created for that site on the same calendar day. Every new `PurchaseRequest` SHALL start in `INICIADO` status.

#### Scenario: First Pedido de Compra of the day
- **WHEN** a construction site member with `PURCHASE_REQUEST` access creates a Pedido de Compra with two items, and no other Pedido de Compra exists for that site that day
- **THEN** `pantheon-service` persists a new `PurchaseRequest` named "Pedido \<today's date> #1", in `INICIADO` status, with both items in `PENDING` status

#### Scenario: Second Pedido de Compra of the same day
- **WHEN** a construction site member creates a second Pedido de Compra on a site that already has one Pedido de Compra created that same day
- **THEN** `pantheon-service` names the new `PurchaseRequest` "Pedido \<today's date> #2"

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Pedido de Compra listing and filtering
`pantheon-service` SHALL allow any member of a construction site to list its `PurchaseRequest` headers — paginated, and optionally filtered to those created on a given calendar date and/or in a given `PurchaseRequestStatus` — and to list the `PurchaseRequestItem`s of a specific header, optionally filtered by status (`PENDING` or `CONVERTED`), each item annotated with its current `selectedOrcamentoLineItemId` (if any).

#### Scenario: Member paginates and filters Pedidos de Compra
- **WHEN** an authenticated member of a construction site requests its Pedidos de Compra with a page number, page size, and a status filter of `ORCADO`
- **THEN** `pantheon-service` returns the matching page of `PurchaseRequest` headers in `ORCADO` status for that site, along with the total count

#### Scenario: Member lists Pedidos de Compra for a given date
- **WHEN** an authenticated member of a construction site requests its Pedidos de Compra filtered to a specific date
- **THEN** `pantheon-service` returns only the `PurchaseRequest` headers created on that site on that date

#### Scenario: Member lists a header's pending items
- **WHEN** an authenticated member requests a specific Pedido de Compra's items filtered to `PENDING`
- **THEN** `pantheon-service` returns every item of that header that has not yet been converted into an Orçamento

### Requirement: Purchase-request views
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a header separating the "Filtrar" action (opening a filter panel for status and date) from the "Novo pedido" creation action; a paginated card list where each card shows the Pedido de Compra's name, its status badge (Iniciado/Orçado/Conferido/Concluído), its item count, and the count of Orçamentos linked to it; and, on the detail view, the header's items (each showing its current selection when one exists), a link to every linked Orçamento, the comparison table (once at least one Orçamento is linked) with a per-supplier "Imprimir PDF" action, and — once submitted — the approval timeline and post-conclusion materials. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member creates a Pedido de Compra from the UI
- **WHEN** a construction site member opens "Novo pedido", fills in one or more items, and submits the form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new, auto-named Pedido de Compra, in `Iniciado` status, in the site's list

#### Scenario: Member filters and paginates the list from the UI
- **WHEN** a construction site member opens the "Filtrar" panel, selects a status, and navigates to a later page of results
- **THEN** `pantheon-web` shows only the matching Pedidos de Compra for that page, without disturbing the separate "Novo pedido" action

#### Scenario: Member converts a selection from the UI
- **WHEN** a construction site member opens a Pedido de Compra, checks one or more of its pending items, and clicks "Criar orçamento"
- **THEN** `pantheon-web` submits the selection to `pantheon-service`, navigates to the newly created Orçamento, and removes the converted items from the pending list while leaving the Pedido de Compra open for any remaining pending items

#### Scenario: Detail view lists every linked Orçamento
- **WHEN** a construction site member opens a Pedido de Compra that has two linked Orçamentos
- **THEN** `pantheon-web` shows both as links that navigate to their respective Orçamento detail views

## ADDED Requirements

### Requirement: Pedido de Compra status lifecycle
`pantheon-service` SHALL track each `PurchaseRequest`'s lifecycle with `PurchaseRequestStatus{INICIADO, ORCADO, CONFERIDO, CONCLUIDO}`. A header SHALL start `INICIADO`. It SHALL transition to `ORCADO` automatically the first time an `Orcamento` is created with that header as its `sourcePurchaseRequestId`. Transitions to `CONFERIDO` and `CONCLUIDO`, and back to `ORCADO` on rejection, are governed by the approval workflow (see `purchase-request-approval-workflow`).

#### Scenario: New header starts Iniciado
- **WHEN** a construction site member creates a new Pedido de Compra
- **THEN** `pantheon-service` persists it in `INICIADO` status

#### Scenario: First linked Orçamento moves it to Orçado
- **WHEN** a construction site member converts items of an `INICIADO` Pedido de Compra into its first Orçamento
- **THEN** `pantheon-service` transitions that header to `ORCADO`

#### Scenario: Later Orçamentos don't change an already-Orçado status
- **WHEN** a construction site member converts more items of an already-`ORCADO` Pedido de Compra into a second Orçamento
- **THEN** `pantheon-service` leaves the header's status as `ORCADO`

### Requirement: Per-item supplier selection
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to set or clear a `PurchaseRequestItem`'s `selectedOrcamentoLineItemId`, identifying which `OrcamentoLineItem` (and therefore which supplier's quote) will fulfill that item, only while the header is `ORCADO`. `pantheon-service` SHALL reject a selection whose target `OrcamentoLineItem` belongs to an Orçamento not linked to that same header, or whose target `OrcamentoLineItem` has a `sourcePurchaseRequestItemId` set to a different item.

#### Scenario: Selecting a quoted line item for an item
- **WHEN** a construction site member selects, for a Pedido de Compra item, an `OrcamentoLineItem` from a linked Orçamento that was quoted against that same item
- **THEN** `pantheon-service` records that line item as the item's `selectedOrcamentoLineItemId`

#### Scenario: Mixed selection across two suppliers
- **WHEN** a construction site member selects one item's fulfillment from Orçamento A and a different item's fulfillment from Orçamento B, both linked to the same Pedido de Compra
- **THEN** `pantheon-service` records both selections independently, allowing the header's eventual purchase to combine both suppliers

#### Scenario: Cannot select a line item from an unrelated Orçamento
- **WHEN** a construction site member attempts to select an `OrcamentoLineItem` belonging to an Orçamento whose `sourcePurchaseRequestId` does not match the item's header
- **THEN** `pantheon-service` rejects the request

#### Scenario: Cannot select after the mix is locked in
- **WHEN** a construction site member attempts to change a selection on a Pedido de Compra that is `CONFERIDO` or `CONCLUIDO`
- **THEN** `pantheon-service` rejects the request

#### Scenario: Clearing a selection
- **WHEN** a construction site member clears a previously set selection on an `ORCADO` Pedido de Compra's item
- **THEN** `pantheon-service` sets that item's `selectedOrcamentoLineItemId` to null

### Requirement: Pedido de Compra comparison table
`pantheon-service` SHALL provide, for a given Pedido de Compra, a comparison view listing every one of its `PurchaseRequestItem`s as a row and every Orçamento linked to it (identified by supplier) as a column, populating each cell with the price and quantity of that Orçamento's `OrcamentoLineItem` quoted against that row's item, when one exists, and flagging the cell currently selected for that row.

#### Scenario: Comparison table with two suppliers quoting the same product
- **WHEN** a construction site member requests the comparison view for a Pedido de Compra with two linked Orçamentos that both quoted the same item at different prices
- **THEN** `pantheon-service` returns one row for that item with both suppliers' prices populated, and flags whichever is currently selected

#### Scenario: Unquoted cell left empty
- **WHEN** a linked Orçamento did not quote a price for a particular item
- **THEN** `pantheon-service` returns that row's cell for that Orçamento's column as empty

### Requirement: Per-supplier Pedido de Compra PDF
`pantheon-service` SHALL generate, for a given Pedido de Compra and one of its linked Orçamentos, a PDF document listing that Orçamento's line items that are the currently selected fulfillment for their Pedido de Compra item, each with quantity, unit price, and line total, alongside the Pedido de Compra's name and the supplier's snapshot data, and a grand total.

#### Scenario: Member downloads a supplier-scoped PDF
- **WHEN** a construction site member requests the PDF for one of a Pedido de Compra's linked Orçamentos
- **THEN** `pantheon-service` returns a PDF containing only that Orçamento's currently-selected items, their prices, and a grand total

#### Scenario: Non-selected items excluded from the PDF
- **WHEN** a linked Orçamento has line items that are not the currently selected fulfillment for their item
- **THEN** `pantheon-service` excludes those line items from that Orçamento's PDF
