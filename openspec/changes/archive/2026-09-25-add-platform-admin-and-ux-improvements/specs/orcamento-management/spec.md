## MODIFIED Requirements

### Requirement: Orçamento workflow views
`pantheon-web` SHALL provide, on a construction site's "Orçamentos" tab, a header separating the "Filtrar" action (opening a filter panel for date, originating Pedido de Compra, and supplier name) from the "Novo orçamento" creation action; a paginated card list, with a page-size selector (1, 5, or 10 results per page), showing each Orçamento's supplier, status badge (`DRAFT`/`LOCKED`), and line-item count; and a detail view whose page header presents a three-level breadcrumb (a back-arrow icon linking back to the Orçamentos list, the obra's name linking to the obra, "Lista de Orçamentos" linking back to the Orçamentos list, and a final non-clickable "Orçamentos" segment marking the current page) in place of a plain back button, the supplier's data — including its payment method and, when the method is Pix, its Pix key — a link to its originating Pedido de Compra when one exists, its status, and its line items with a computed total when unit prices are present, each line item visually marked when it is the currently selected fulfillment for its Pedido-de-Compra item. The detail view SHALL NOT present submission, approval, or conclusion controls. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member filters and paginates the Orçamento list from the UI
- **WHEN** a construction site member opens the "Filtrar" panel, sets a supplier-name filter, and navigates to a later page of results
- **THEN** `pantheon-web` shows only the matching Orçamentos for that page

#### Scenario: Member changes the Orçamento list's page size
- **WHEN** a construction site member selects a different page-size option (1, 5, or 10) on the Orçamento list
- **THEN** `pantheon-web` reloads the list from the first page using the newly selected size

#### Scenario: Detail view links back to the originating Pedido de Compra
- **WHEN** a construction site member opens an Orçamento that was created from a Pedido de Compra
- **THEN** `pantheon-web` shows that Pedido de Compra's name as a link that navigates to it

#### Scenario: Breadcrumb returns to the Orçamentos list, not the obra summary
- **WHEN** a construction site member opens an Orçamento's detail view and then clicks the breadcrumb's "Lista de Orçamentos" link
- **THEN** `pantheon-web` returns to the obra's Orçamentos tab, not its Resumo tab

#### Scenario: Locked Orçamento shows no editing controls
- **WHEN** a construction site member opens a `LOCKED` Orçamento
- **THEN** `pantheon-web` shows its line items read-only and displays a "Bloqueado" status indicator, with no add/edit/remove controls

#### Scenario: Detail view shows the Pix key only for a Pix supplier
- **WHEN** a construction site member opens an Orçamento whose supplier's payment method is `PIX`
- **THEN** `pantheon-web` shows both the payment method and the Pix key in the supplier section

#### Scenario: Detail view omits the Pix key for a non-Pix supplier
- **WHEN** a construction site member opens an Orçamento whose supplier's payment method is `CARTAO`, `BOLETO`, or `DINHEIRO`
- **THEN** `pantheon-web` shows the payment method and no Pix-key field
