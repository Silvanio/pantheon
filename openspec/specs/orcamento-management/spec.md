# orcamento-management Specification

## Purpose
Defines Orçamento (budget/quote) management for a construction site: supplier-backed creation (optionally sourced from a single Pedido de Compra's items), line-item CRUD while in `DRAFT`, a lock state that follows its originating Pedido de Compra's approval outcome, and listing/detail/UI views. Approval itself, and conclusion into delivery-tracking materials, are handled by `purchase-request-approval-workflow`.

## Requirements
### Requirement: Orçamento creation
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to create an `Orcamento` for that site, either empty or pre-filled with line items copied from selected `PurchaseRequestItem`s of a single Pedido de Compra, starting in `DRAFT` status. Creation SHALL require supplier data: a CNPJ and a name are mandatory, and an address and a contact name/phone are optional. `pantheon-service` SHALL resolve the supplier via find-or-create against the site's company's `Fornecedor` registry and store a snapshot of the resolved supplier's fields on the Orçamento, together with a traceability reference to that `Fornecedor`. When created from a Pedido de Compra's items, `pantheon-service` SHALL set the Orçamento's `sourcePurchaseRequestId` to that header's id, and, if the header is still `INICIADO`, transition it to `ORCADO`.

#### Scenario: Orçamento created from scratch with a new supplier
- **WHEN** a construction site member with `ORCAMENTO_MANAGE` access creates a new Orçamento with no purchase-request items selected, supplying a CNPJ and name with no matching existing `Fornecedor`
- **THEN** `pantheon-service` persists a new `Orcamento` in `DRAFT` status with no line items, no `sourcePurchaseRequestId`, registers a new `Fornecedor`, and stores the supplier snapshot on the Orçamento

#### Scenario: Orçamento created reusing a known supplier
- **WHEN** a construction site member creates an Orçamento supplying a CNPJ that matches an existing `Fornecedor` of the site's company
- **THEN** `pantheon-service` reuses that `Fornecedor`, does not create a duplicate, and stores its fields as the Orçamento's supplier snapshot

#### Scenario: Cannot create without required supplier fields
- **WHEN** a construction site member attempts to create an Orçamento without a CNPJ or without a name
- **THEN** `pantheon-service` rejects the request

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: First Orçamento of a Pedido de Compra moves it to Orçado
- **WHEN** a construction site member converts items of an `INICIADO` Pedido de Compra into a new Orçamento
- **THEN** `pantheon-service` creates the Orçamento with `sourcePurchaseRequestId` set to that header, and transitions the header to `ORCADO`

### Requirement: Orçamento line item management
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to add, edit, or remove an `OrcamentoLineItem` (free-text name, optional type, quantity, and optional unit price) on an Orçamento only while it is `DRAFT`.

#### Scenario: Line item added while Draft
- **WHEN** a construction site member submits a name, quantity, and unit price for a new line item on a `DRAFT` Orçamento
- **THEN** `pantheon-service` adds the line item to that Orçamento

#### Scenario: Line items cannot change once Locked
- **WHEN** a construction site member attempts to add, edit, or remove a line item on a `LOCKED` Orçamento
- **THEN** `pantheon-service` rejects the request

### Requirement: Orçamento status follows its originating Pedido de Compra's approval state
`pantheon-service` SHALL keep every `Orcamento` in `DRAFT` status unless it has a `sourcePurchaseRequestId`, in which case its status SHALL track that Pedido de Compra's approval outcome: `pantheon-service` SHALL transition it to `LOCKED` when that Pedido de Compra's approval reaches `CONFERIDO`, and back to `DRAFT` when a rejection returns that Pedido de Compra to `ORCADO`. An Orçamento with no `sourcePurchaseRequestId` SHALL remain `DRAFT` indefinitely.

#### Scenario: Linked Orçamentos lock when their Pedido de Compra is approved
- **WHEN** a Pedido de Compra with two linked Orçamentos has its approval cycle fully approved, reaching `CONFERIDO`
- **THEN** `pantheon-service` transitions both linked Orçamentos to `LOCKED`

#### Scenario: Linked Orçamentos unlock when their Pedido de Compra's approval is rejected
- **WHEN** a `CONFERIDO` Pedido de Compra's approval cycle is reopened via rejection and returns to `ORCADO`
- **THEN** `pantheon-service` transitions its linked, `LOCKED` Orçamentos back to `DRAFT`

#### Scenario: Standalone Orçamento never locks
- **WHEN** an Orçamento with no originating Pedido de Compra exists
- **THEN** `pantheon-service` never transitions it to `LOCKED` regardless of any Pedido de Compra's approval activity

### Requirement: Orçamento listing and detail
`pantheon-service` SHALL allow any member of a construction site to list its Orçamentos — paginated, and optionally filtered to those created on a given calendar date, originating from a given Pedido de Compra, and/or whose supplier name contains a given text (case-insensitive) — and view one's full detail, including its supplier snapshot, its status, its originating Pedido de Compra (when any), and its line items with an indication of whether each line item is currently selected on its originating Pedido de Compra item.

#### Scenario: Member views an Orçamento's detail
- **WHEN** an authenticated member of a construction site requests a specific Orçamento by id
- **THEN** `pantheon-service` returns it with its supplier snapshot, status, originating Pedido de Compra reference (if any), and line items each flagged with whether they are the currently selected fulfillment for their Pedido-de-Compra item

#### Scenario: Member paginates and filters Orçamentos
- **WHEN** an authenticated member requests a site's Orçamentos with a page number, page size, and a supplier-name filter
- **THEN** `pantheon-service` returns the matching page of Orçamentos whose supplier name contains that text, along with the total count

### Requirement: Orçamento workflow views
`pantheon-web` SHALL provide, on a construction site's "Orçamentos" tab, a header separating the "Filtrar" action (opening a filter panel for date, originating Pedido de Compra, and supplier name) from the "Novo orçamento" creation action; a paginated card list showing each Orçamento's supplier, status badge (`DRAFT`/`LOCKED`), and line-item count; and a detail view presenting the supplier's data, a link to its originating Pedido de Compra when one exists, its status, and its line items with a computed total when unit prices are present, each line item visually marked when it is the currently selected fulfillment for its Pedido-de-Compra item. The detail view SHALL NOT present submission, approval, or conclusion controls. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member filters and paginates the Orçamento list from the UI
- **WHEN** a construction site member opens the "Filtrar" panel, sets a supplier-name filter, and navigates to a later page of results
- **THEN** `pantheon-web` shows only the matching Orçamentos for that page

#### Scenario: Detail view links back to the originating Pedido de Compra
- **WHEN** a construction site member opens an Orçamento that was created from a Pedido de Compra
- **THEN** `pantheon-web` shows that Pedido de Compra's name as a link that navigates to it

#### Scenario: Locked Orçamento shows no editing controls
- **WHEN** a construction site member opens a `LOCKED` Orçamento
- **THEN** `pantheon-web` shows its line items read-only and displays a "Bloqueado" status indicator, with no add/edit/remove controls

### Requirement: Orçamento deletion
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to permanently delete an `Orcamento`, together with all of its `OrcamentoLineItem`s, only while it is `DRAFT`. `pantheon-service` SHALL reject a deletion attempt on a `LOCKED` Orçamento.

When the deleted Orçamento has a `sourcePurchaseRequestId`, `pantheon-service` SHALL additionally, in the same transaction:
- for every `PurchaseRequestItem` of that header whose `convertedToOrcamentoId` equals the deleted Orçamento's id, revert it to `PENDING` status and clear its `convertedToOrcamentoId` and `convertedAt`;
- clear `selectedOrcamentoLineItemId` on any `PurchaseRequestItem` of that header that referenced one of the deleted Orçamento's line items;
- if, after the deletion, no `Orcamento` remains linked to that header, transition the header from `ORCADO` back to `INICIADO`.

#### Scenario: Deleting a standalone Draft Orçamento
- **WHEN** a construction site member with `ORCAMENTO_MANAGE` access deletes a `DRAFT` Orçamento with no `sourcePurchaseRequestId`
- **THEN** `pantheon-service` permanently removes the Orçamento and all of its line items

#### Scenario: Deleting a converted Orçamento reverts its items to Pendente
- **WHEN** a construction site member deletes a `DRAFT` Orçamento that was created from two `PurchaseRequestItem`s of a Pedido de Compra
- **THEN** `pantheon-service` removes the Orçamento and reverts both items to `PENDING` status with no `convertedToOrcamentoId`, making them available for a later conversion

#### Scenario: Deleting a converted Orçamento clears stale selections
- **WHEN** a construction site member deletes a `DRAFT` Orçamento whose line item is currently the `selectedOrcamentoLineItemId` of a Pedido de Compra item
- **THEN** `pantheon-service` clears that item's `selectedOrcamentoLineItemId`

#### Scenario: Deleting the last linked Orçamento reverts the header to Iniciado
- **WHEN** a construction site member deletes the only Orçamento linked to an `ORCADO` Pedido de Compra
- **THEN** `pantheon-service` transitions that header back to `INICIADO`

#### Scenario: Deleting one of several linked Orçamentos leaves the header Orçado
- **WHEN** a construction site member deletes one of two Orçamentos linked to an `ORCADO` Pedido de Compra
- **THEN** `pantheon-service` leaves the header's status as `ORCADO`, since the other Orçamento is still linked

#### Scenario: Cannot delete a Locked Orçamento
- **WHEN** a construction site member attempts to delete a `LOCKED` Orçamento
- **THEN** `pantheon-service` rejects the request and leaves the Orçamento and its line items unchanged

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to delete an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Deleting an Orçamento from the UI
`pantheon-web` SHALL show an "Excluir" action on an Orçamento's detail view only while it is `DRAFT` ("Rascunho"), prompt the member for confirmation before submitting the deletion, and, on success, navigate back to the Orçamento list. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member deletes a Draft Orçamento from the UI
- **WHEN** a construction site member opens a `DRAFT` Orçamento, clicks "Excluir", and confirms the prompt
- **THEN** `pantheon-web` submits the deletion to `pantheon-service` and navigates back to the Orçamento list, which no longer shows that Orçamento

#### Scenario: Delete action hidden once Locked
- **WHEN** a construction site member opens a `LOCKED` Orçamento
- **THEN** `pantheon-web` does not show the "Excluir" action
