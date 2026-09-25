## ADDED Requirements

### Requirement: Adding items to an existing Pedido de Compra
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` `MANAGE` access to add one or more new `PurchaseRequestItem`s — each with the same shape as at creation (a free-text name, an optional type, a quantity, and an optional unit of measure) — to an existing `PurchaseRequest` header, but only while that header is in `INICIADO` status. Added items SHALL start in `PENDING` status, the same as items submitted at creation. `pantheon-service` SHALL reject the attempt with HTTP 409 if the header is not `INICIADO`, and with HTTP 403 if the acting member's resolved `PURCHASE_REQUEST` access is not `MANAGE`.

#### Scenario: Adding items to an Iniciado header
- **WHEN** a construction site member with `PURCHASE_REQUEST` `MANAGE` access adds two new items to a Pedido de Compra that is still `INICIADO`
- **THEN** `pantheon-service` creates both as new `PurchaseRequestItem`s in `PENDING` status, belonging to that same header

#### Scenario: Cannot add items once a header has moved past Iniciado
- **WHEN** a construction site member attempts to add items to a Pedido de Compra that is `ORCADO`, `CONFERIDO`, or `CONCLUIDO`
- **THEN** `pantheon-service` rejects the request with HTTP 409, and no item is created

#### Scenario: Member without manage access cannot add items
- **WHEN** a construction site member whose resolved `PURCHASE_REQUEST` access is not `MANAGE` attempts to add items to a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

## MODIFIED Requirements

### Requirement: Pedido de Compra header creation
`pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` access to create a `PurchaseRequest` (Pedido de Compra) for that site consisting of zero or more `PurchaseRequestItem`s submitted together, each with a free-text name, an optional type, a quantity, and an optional unit of measure, all starting in `PENDING` status. `pantheon-service` SHALL generate the `PurchaseRequest`'s name as `"Pedido " + <creation date as dd/MM/yyyy> + " #" + <sequence>`, where `<sequence>` is one more than the number of `PurchaseRequest`s already created for that site on the same calendar day. Every new `PurchaseRequest` SHALL start in `INICIADO` status.

#### Scenario: First Pedido de Compra of the day
- **WHEN** a construction site member with `PURCHASE_REQUEST` access creates a Pedido de Compra with two items, and no other Pedido de Compra exists for that site that day
- **THEN** `pantheon-service` persists a new `PurchaseRequest` named "Pedido \<today's date> #1", in `INICIADO` status, with both items in `PENDING` status

#### Scenario: Second Pedido de Compra of the same day
- **WHEN** a construction site member creates a second Pedido de Compra on a site that already has one Pedido de Compra created that same day
- **THEN** `pantheon-service` names the new `PurchaseRequest` "Pedido \<today's date> #2"

#### Scenario: A Pedido de Compra can be created with no items
- **WHEN** a construction site member with `PURCHASE_REQUEST` access creates a Pedido de Compra with an empty item list
- **THEN** `pantheon-service` persists a new, auto-named `PurchaseRequest` in `INICIADO` status with zero items

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `PURCHASE_REQUEST` access attempts to create a Pedido de Compra
- **THEN** `pantheon-service` rejects the request with HTTP 403

### Requirement: Purchase-request views
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a header separating the "Filtrar" action (opening a filter panel for status and date) from the "Novo pedido" action; a paginated card list where each card shows the Pedido de Compra's name, its status badge (Iniciado/Orçado/Conferido/Concluído), its item count, and the count of Orçamentos linked to it; and, on the detail view, the header's items — both pending and already-converted, each selectable for a new conversion regardless of its current status — showing each item's current selection when one exists, a link to every linked Orçamento, the comparison table (once at least one Orçamento is linked) with a per-supplier "Imprimir PDF" action and a separate "Baixar resumo" action that downloads the consolidated summary PDF, and — once submitted — the approval timeline and post-conclusion materials. Clicking "Novo pedido" SHALL NOT open any item-entry form; it SHALL immediately create an empty Pedido de Compra and navigate to its detail view. While the header is `INICIADO` (regardless of its current item count) and the viewer has `PURCHASE_REQUEST` `MANAGE` access, the detail view SHALL show an "Adicionar produtos" action that opens a modal with a repeatable item-rows form (name/type/quantity/unit, add-row/remove-row) — the only place items are entered anywhere in this flow; this action SHALL be hidden once the header is no longer `INICIADO`. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member creates a Pedido de Compra from the UI
- **WHEN** a construction site member clicks "Novo pedido"
- **THEN** `pantheon-web` creates a new, empty, auto-named Pedido de Compra in `Iniciado` status and navigates directly to its detail view, without showing any item-entry form

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

#### Scenario: Member adds the first products to a freshly created, empty Pedido de Compra
- **WHEN** a construction site member with manage access lands on the detail view of a Pedido de Compra they just created (zero items, `Iniciado`), clicks "Adicionar produtos", fills in one or more item rows, and submits
- **THEN** `pantheon-web` submits the new items to `pantheon-service`, closes the modal, and shows the added items in the header's item list

#### Scenario: Member adds more products to an already-populated Iniciado Pedido de Compra from the UI
- **WHEN** a construction site member with manage access opens a Pedido de Compra that already has items and is still `Iniciado`, clicks "Adicionar produtos", fills in one or more item rows, and submits
- **THEN** `pantheon-web` submits the new items to `pantheon-service`, closes the modal, and shows the added items alongside the existing ones in the header's item list

#### Scenario: Adicionar produtos action is hidden once a header is no longer Iniciado
- **WHEN** a construction site member opens a Pedido de Compra that is `Orçado`, `Conferido`, or `Concluído`
- **THEN** `pantheon-web` does not show the "Adicionar produtos" action
