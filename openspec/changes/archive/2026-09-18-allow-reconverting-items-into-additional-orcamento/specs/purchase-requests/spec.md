## MODIFIED Requirements

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
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a header separating the "Filtrar" action (opening a filter panel for status and date) from the "Novo pedido" creation action; a paginated card list where each card shows the Pedido de Compra's name, its status badge (Iniciado/Orçado/Conferido/Concluído), its item count, and the count of Orçamentos linked to it; and, on the detail view, the header's items — both pending and already-converted, each selectable for a new conversion regardless of its current status — showing each item's current selection when one exists, a link to every linked Orçamento, the comparison table (once at least one Orçamento is linked) with a per-supplier "Imprimir PDF" action, and — once submitted — the approval timeline and post-conclusion materials. All copy SHALL be sourced from the `pt-BR` locale resource file.

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
