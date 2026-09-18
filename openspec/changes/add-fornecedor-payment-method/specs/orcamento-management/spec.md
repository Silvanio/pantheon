## MODIFIED Requirements

### Requirement: Orçamento creation
`pantheon-service` SHALL allow a construction site member with `ORCAMENTO_MANAGE` access to create an `Orcamento` for that site, either empty or pre-filled with line items copied from selected `PurchaseRequestItem`s of a single Pedido de Compra, starting in `DRAFT` status. Creation SHALL require supplier data: a CNPJ and a name are mandatory, an address and a contact name/phone are optional, and a payment method (Cartão, Boleto, Pix, or Dinheiro) and — only when the method is Pix — a Pix key are optional-but-conditionally-required as defined by `supplier-registry`. `pantheon-service` SHALL resolve the supplier via find-or-create against the site's company's `Fornecedor` registry and store a snapshot of the resolved supplier's fields — including payment method and Pix key — on the Orçamento, together with a traceability reference to that `Fornecedor`. When created from a Pedido de Compra's items, `pantheon-service` SHALL set the Orçamento's `sourcePurchaseRequestId` to that header's id, and, if the header is still `INICIADO`, transition it to `ORCADO`.

#### Scenario: Orçamento created from scratch with a new supplier
- **WHEN** a construction site member with `ORCAMENTO_MANAGE` access creates a new Orçamento with no purchase-request items selected, supplying a CNPJ and name with no matching existing `Fornecedor`
- **THEN** `pantheon-service` persists a new `Orcamento` in `DRAFT` status with no line items, no `sourcePurchaseRequestId`, registers a new `Fornecedor`, and stores the supplier snapshot on the Orçamento

#### Scenario: Orçamento created reusing a known supplier
- **WHEN** a construction site member creates an Orçamento supplying a CNPJ that matches an existing `Fornecedor` of the site's company
- **THEN** `pantheon-service` reuses that `Fornecedor`, does not create a duplicate, and stores its fields (including payment method and Pix key) as the Orçamento's supplier snapshot

#### Scenario: Cannot create without required supplier fields
- **WHEN** a construction site member attempts to create an Orçamento without a CNPJ or without a name
- **THEN** `pantheon-service` rejects the request

#### Scenario: Member without access blocked
- **WHEN** a construction site member with no `ORCAMENTO_MANAGE` access attempts to create an Orçamento
- **THEN** `pantheon-service` rejects the request with HTTP 403

#### Scenario: First Orçamento of a Pedido de Compra moves it to Orçado
- **WHEN** a construction site member converts items of an `INICIADO` Pedido de Compra into a new Orçamento
- **THEN** `pantheon-service` creates the Orçamento with `sourcePurchaseRequestId` set to that header, and transitions the header to `ORCADO`

#### Scenario: Payment method and Pix key are snapshotted onto the Orçamento
- **WHEN** a construction site member creates an Orçamento whose resolved `Fornecedor` has payment method `PIX` and a Pix key
- **THEN** `pantheon-service` stores that payment method and Pix key on the new Orçamento

### Requirement: Orçamento workflow views
`pantheon-web` SHALL provide, on a construction site's "Orçamentos" tab, a header separating the "Filtrar" action (opening a filter panel for date, originating Pedido de Compra, and supplier name) from the "Novo orçamento" creation action; a paginated card list showing each Orçamento's supplier, status badge (`DRAFT`/`LOCKED`), and line-item count; and a detail view presenting the supplier's data — including its payment method and, when the method is Pix, its Pix key — a link to its originating Pedido de Compra when one exists, its status, and its line items with a computed total when unit prices are present, each line item visually marked when it is the currently selected fulfillment for its Pedido-de-Compra item. The detail view SHALL NOT present submission, approval, or conclusion controls. All copy SHALL be sourced from the `pt-BR` locale resource file.

#### Scenario: Member filters and paginates the Orçamento list from the UI
- **WHEN** a construction site member opens the "Filtrar" panel, sets a supplier-name filter, and navigates to a later page of results
- **THEN** `pantheon-web` shows only the matching Orçamentos for that page

#### Scenario: Detail view links back to the originating Pedido de Compra
- **WHEN** a construction site member opens an Orçamento that was created from a Pedido de Compra
- **THEN** `pantheon-web` shows that Pedido de Compra's name as a link that navigates to it

#### Scenario: Locked Orçamento shows no editing controls
- **WHEN** a construction site member opens a `LOCKED` Orçamento
- **THEN** `pantheon-web` shows its line items read-only and displays a "Bloqueado" status indicator, with no add/edit/remove controls

#### Scenario: Detail view shows the Pix key only for a Pix supplier
- **WHEN** a construction site member opens an Orçamento whose supplier's payment method is `PIX`
- **THEN** `pantheon-web` shows both the payment method and the Pix key in the supplier section

#### Scenario: Detail view omits the Pix key for a non-Pix supplier
- **WHEN** a construction site member opens an Orçamento whose supplier's payment method is `CARTAO`, `BOLETO`, or `DINHEIRO`
- **THEN** `pantheon-web` shows the payment method and no Pix-key field
