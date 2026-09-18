## MODIFIED Requirements

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

## ADDED Requirements

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
