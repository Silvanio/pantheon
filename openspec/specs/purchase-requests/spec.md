# purchase-requests Specification

## Purpose
Defines the "Pedido de Compra" (purchase request) flow: grouping requested materials under a named header per construction site, its status lifecycle (`INICIADO` → `ORCADO` → `CONFERIDO` → `CONCLUIDO`), converting a header's pending items into an Orçamento, selecting each item's fulfilling supplier once multiple Orçamentos are linked, and comparing/printing the resulting supplier mix. Submission for approval, acting on approval steps, and conclusion are defined in `purchase-request-approval-workflow`.

## Requirements

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

### Requirement: Converting purchase-request items into an Orçamento
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to select one or more `PurchaseRequestItem`s belonging to the same `PurchaseRequest` header — regardless of whether any of them have already been converted before — and create a new `Orcamento` whose line items are copied from the selected items' name, type, and quantity and whose `sourcePurchaseRequestId` is set to that header's id. The first time a given item is converted, it SHALL transition to `CONVERTED` status and record which Orçamento it was first converted into; converting it again (into a further Orçamento) SHALL leave its `status` and recorded Orçamento unchanged. Items of that header not selected SHALL remain in their current status and available for a later conversion into a different Orçamento.

#### Scenario: Selected items become a new Orçamento
- **WHEN** a construction site member selects two `PENDING` items from the same Pedido de Compra and chooses "Criar orçamento"
- **THEN** `pantheon-service` creates a new `Orcamento` in `DRAFT` status linked to that Pedido de Compra, with two line items copied from the selected items, and marks both items `CONVERTED`

#### Scenario: Same Pedido de Compra spawns a second Orçamento later
- **WHEN** a construction site member converts a first subset of a Pedido de Compra's items into an Orçamento, then later selects the remaining `PENDING` items of that same Pedido de Compra and converts them
- **THEN** `pantheon-service` creates a second `Orcamento`, also linked to that same Pedido de Compra, distinct from the first

#### Scenario: Cannot select items from more than one Pedido de Compra at once
- **WHEN** a construction site member attempts to convert a selection of `PurchaseRequestItem`s spanning more than one `PurchaseRequest` header in a single action
- **THEN** `pantheon-service` rejects the request

#### Scenario: An already-converted item can be quoted by a second supplier
- **WHEN** a construction site member selects a `CONVERTED` item (already part of an earlier Orçamento) and converts it again with a different supplier
- **THEN** `pantheon-service` creates a second `Orcamento` whose line items include that item, leaves the item's `status` `CONVERTED` and its originally-recorded Orçamento unchanged, and the item is now quoted by both Orçamentos

### Requirement: Purchase-request views
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a header separating the "Filtrar" action (opening a filter panel for status and date) from the "Novo pedido" creation action; a paginated card list where each card shows the Pedido de Compra's name, its status badge (Iniciado/Orçado/Conferido/Concluído), its item count, and the count of Orçamentos linked to it; and, on the detail view, the header's items — both pending and already-converted, each selectable for a new conversion regardless of its current status — showing each item's current selection when one exists, a link to every linked Orçamento, the comparison table (once at least one Orçamento is linked) with a per-supplier "Imprimir PDF" action and a separate "Baixar resumo" action that downloads the consolidated summary PDF, and — once submitted — the approval timeline and post-conclusion materials. All copy SHALL be sourced from the `pt-BR` locale resource file.

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

#### Scenario: Member requests a second quote for an already-converted item
- **WHEN** a construction site member checks one or more items in the "Convertidos em orçamento" section and clicks "Criar orçamento"
- **THEN** `pantheon-web` submits that selection to `pantheon-service` the same way it would for pending items, and navigates to the newly created Orçamento

#### Scenario: Member downloads the consolidated summary PDF
- **WHEN** a construction site member with at least one linked Orçamento clicks "Baixar resumo" on the comparison table section
- **THEN** `pantheon-web` downloads the consolidated summary PDF from `pantheon-service`, separately from any per-supplier "Imprimir PDF" action

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

### Requirement: Pedido de Compra deletion
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to permanently delete a `PurchaseRequest` header, together with all of its `PurchaseRequestItem`s and any `PurchaseRequestInvoice`s attached to it (including their stored files), only while that header is `INICIADO`. `pantheon-service` SHALL reject a deletion attempt on a header in any other status (`ORCADO`, `CONFERIDO`, or `CONCLUIDO`).

#### Scenario: Deleting an Iniciado header
- **WHEN** a construction site member with `PURCHASE_REQUEST` access deletes a Pedido de Compra that is still `INICIADO`
- **THEN** `pantheon-service` permanently removes the `PurchaseRequest` and all of its `PurchaseRequestItem`s

#### Scenario: Deleting a header also removes its attached invoices
- **WHEN** a construction site member deletes an `INICIADO` Pedido de Compra that has one or more `PurchaseRequestInvoice`s attached
- **THEN** `pantheon-service` deletes those invoices' stored files from object storage and their database rows, alongside the header and its items

#### Scenario: Cannot delete once Orçado or later
- **WHEN** a construction site member attempts to delete a Pedido de Compra that is `ORCADO`, `CONFERIDO`, or `CONCLUIDO`
- **THEN** `pantheon-service` rejects the request and leaves the header and its items unchanged

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to delete a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Deleting a Pedido de Compra from the UI
`pantheon-web` SHALL show an "Excluir" action on a Pedido de Compra's detail view only while it is `Iniciado`, prompt the member for confirmation before submitting the deletion, and, on success, navigate back to the Pedido de Compra list. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member deletes an Iniciado Pedido de Compra from the UI
- **WHEN** a construction site member opens an `Iniciado` Pedido de Compra, clicks "Excluir", and confirms the prompt
- **THEN** `pantheon-web` submits the deletion to `pantheon-service` and navigates back to the Pedido de Compra list, which no longer shows that header

#### Scenario: Delete action hidden once Orçado or later
- **WHEN** a construction site member opens a Pedido de Compra that is `Orçado`, `Conferido`, or `Concluído`
- **THEN** `pantheon-web` does not show the "Excluir" action

### Requirement: Pedido de Compra invoice attachments
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` manage access to upload one or more invoice files (`PurchaseRequestInvoice`) to a `PurchaseRequest`, at any point in its lifecycle, storing each file in object storage together with its original filename and content type. Accepted file extensions SHALL be `pdf`, `xml`, `jpg`, `jpeg`, and `png`; any other extension, or an empty file, SHALL be rejected. `pantheon-service` SHALL allow any construction site member visible to `PURCHASE_REQUEST` to list a header's invoices and download one's content, and SHALL allow a member with `PURCHASE_REQUEST` manage access to delete one, removing both its database row and its stored file.

#### Scenario: Uploading an invoice
- **WHEN** a construction site member with `PURCHASE_REQUEST` access uploads a PDF file to a Pedido de Compra
- **THEN** `pantheon-service` stores the file in object storage and records a new `PurchaseRequestInvoice` with its original filename and content type, linked to that header

#### Scenario: Uploading regardless of header status
- **WHEN** a construction site member uploads an invoice to a Pedido de Compra that is `ORCADO`, `CONFERIDO`, or `CONCLUIDO`
- **THEN** `pantheon-service` accepts the upload the same way it would for an `INICIADO` header

#### Scenario: Rejecting a disallowed file type
- **WHEN** a construction site member attempts to upload a file whose extension is not `pdf`, `xml`, `jpg`, `jpeg`, or `png`
- **THEN** `pantheon-service` rejects the request

#### Scenario: Member lists and downloads invoices
- **WHEN** an authenticated member visible to `PURCHASE_REQUEST` requests a Pedido de Compra's invoices, then requests one's content
- **THEN** `pantheon-service` returns the list of attached invoices, and separately returns that invoice's stored bytes with its original filename and content type

#### Scenario: Deleting an invoice
- **WHEN** a construction site member with `PURCHASE_REQUEST` access deletes one of a header's invoices
- **THEN** `pantheon-service` removes that invoice's database row and deletes its file from object storage, leaving the header's other invoices (if any) untouched

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to upload or delete an invoice
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Pedido de Compra invoice section in the UI
`pantheon-web` SHALL provide, on the Pedido de Compra detail view, a "Notas fiscais" section — visible regardless of the header's status — listing every attached invoice with its filename and a download link, an upload control to attach a new file, and a remove action per invoice that confirms via the existing inline-popover pattern before deleting.

#### Scenario: Member uploads an invoice from the UI
- **WHEN** a construction site member selects a PDF file in the "Notas fiscais" section's upload control
- **THEN** `pantheon-web` uploads it to `pantheon-service` and shows it in the section's file list

#### Scenario: Member downloads an invoice from the UI
- **WHEN** a construction site member clicks an attached invoice's download link
- **THEN** `pantheon-web` fetches its content and saves it locally under its original filename

#### Scenario: Member removes an invoice from the UI
- **WHEN** a construction site member clicks an invoice's remove action and confirms the popover
- **THEN** `pantheon-web` deletes it and removes it from the section's file list

### Requirement: Pedido de Compra consolidated summary PDF
`pantheon-service` SHALL generate, for a given Pedido de Compra, a single PDF listing every one of its `PurchaseRequestItem`s with its quantity, its currently selected supplier's name (or "Não selecionado" when it has no current selection), the selected line item's unit price, and the resulting line total; followed by one subtotal per supplier that has at least one selected item ("valor a pagar" per supplier), and a grand total across all suppliers. This PDF coexists with, and does not replace, the existing per-supplier PDF.

#### Scenario: Summary lists every item with its assigned supplier
- **WHEN** a construction site member requests the consolidated summary PDF for a Pedido de Compra with items split across two suppliers
- **THEN** `pantheon-service` returns a PDF with one row per item showing that item's assigned supplier, unit price, and line total

#### Scenario: Summary shows a per-supplier subtotal and a grand total
- **WHEN** the requested Pedido de Compra has items selected from two different suppliers
- **THEN** `pantheon-service`'s PDF includes a subtotal for each supplier (the sum of that supplier's selected items' line totals) and a grand total across both

#### Scenario: Unselected items appear without a price
- **WHEN** one of the header's items has no current `selectedOrcamentoLineItemId`
- **THEN** `pantheon-service`'s PDF lists that item as "Não selecionado" with no unit price or line total, and excludes it from every subtotal

#### Scenario: Member without visibility blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` visibility attempts to request the consolidated summary PDF
- **THEN** `pantheon-service` rejects the request with HTTP 403
