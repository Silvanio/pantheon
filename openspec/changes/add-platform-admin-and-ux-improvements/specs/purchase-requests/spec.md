## MODIFIED Requirements

### Requirement: Purchase-request views
`pantheon-web` SHALL provide, on a construction site's "Pedido de Compra" tab, a header separating the "Filtrar" action (opening a filter panel for status and date) from the "Novo pedido" creation action; a paginated card list, with a page-size selector (1, 5, or 10 results per page), where each card shows the Pedido de Compra's name, its status badge (Iniciado/Orçado/Conferido/Concluído), its item count, and the count of Orçamentos linked to it; and, on the detail view, a page header presenting a three-level breadcrumb (a back-arrow icon linking back to the Pedido de Compra list, the obra's name linking to the obra, "Lista de Pedido de Compra" linking back to the Pedido de Compra list, and a final non-clickable "Pedido de Compra" segment marking the current page) in place of a plain back button, the header's items — both pending and already-converted, each selectable for a new conversion regardless of its current status — showing each item's current selection when one exists, a link to every linked Orçamento, the comparison table (once at least one Orçamento is linked) with a per-supplier "Imprimir PDF" action and a separate "Baixar resumo" action that downloads the consolidated summary PDF, and — once submitted — the approval timeline and post-conclusion materials. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member creates a Pedido de Compra from the UI
- **WHEN** a construction site member opens "Novo pedido", fills in one or more items, and submits the form
- **THEN** `pantheon-web` submits the data to `pantheon-service` and shows the new, auto-named Pedido de Compra, in `Iniciado` status, in the site's list

#### Scenario: Member filters and paginates the list from the UI
- **WHEN** a construction site member opens the "Filtrar" panel, selects a status, and navigates to a later page of results
- **THEN** `pantheon-web` shows only the matching Pedidos de Compra for that page, without disturbing the separate "Novo pedido" action

#### Scenario: Member changes the Pedido de Compra list's page size
- **WHEN** a construction site member selects a different page-size option (1, 5, or 10) on the Pedido de Compra list
- **THEN** `pantheon-web` reloads the list from the first page using the newly selected size

#### Scenario: Member converts a selection from the UI
- **WHEN** a construction site member opens a Pedido de Compra, checks one or more of its pending items, and clicks "Criar orçamento"
- **THEN** `pantheon-web` submits the selection to `pantheon-service`, navigates to the newly created Orçamento, and removes the converted items from the pending list while leaving the Pedido de Compra open for any remaining pending items

#### Scenario: Detail view lists every linked Orçamento
- **WHEN** a construction site member opens a Pedido de Compra that has two linked Orçamentos
- **THEN** `pantheon-web` shows both as links that navigate to their respective Orçamento detail views

#### Scenario: Breadcrumb returns to the Pedido de Compra list, not the obra summary
- **WHEN** a construction site member opens a Pedido de Compra's detail view and then clicks the breadcrumb's "Lista de Pedido de Compra" link
- **THEN** `pantheon-web` returns to the obra's Pedido de Compra tab, not its Resumo tab

#### Scenario: Member requests a second quote for an already-converted item
- **WHEN** a construction site member checks one or more items in the "Convertidos em orçamento" section and clicks "Criar orçamento"
- **THEN** `pantheon-web` submits that selection to `pantheon-service` the same way it would for pending items, and navigates to the newly created Orçamento

#### Scenario: Member downloads the consolidated summary PDF
- **WHEN** a construction site member with at least one linked Orçamento clicks "Baixar resumo" on the comparison table section
- **THEN** `pantheon-web` downloads the consolidated summary PDF from `pantheon-service`, separately from any per-supplier "Imprimir PDF" action
