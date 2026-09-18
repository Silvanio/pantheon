## MODIFIED Requirements

### Requirement: Per-supplier Pedido de Compra PDF
`pantheon-service` SHALL generate, for a given Pedido de Compra and one of its linked Orçamentos, a PDF document opening with a branded header (the company's logo, when configured, and name, and the construction site's name), followed by a metadata block showing who created the Pedido de Compra and when, who decided each completed approval step of its current cycle (name and function, or "Equipe da empresa" for a company-staff decision) and when, and its finalization date (or an in-progress indicator if not yet concluded); then that Orçamento's line items that are the currently selected fulfillment for their Pedido de Compra item, each with quantity, unit price, and line total, alongside the Pedido de Compra's name and the supplier's snapshot data, and a grand total.

#### Scenario: Member downloads a supplier-scoped PDF
- **WHEN** a construction site member requests the PDF for one of a Pedido de Compra's linked Orçamentos
- **THEN** `pantheon-service` returns a PDF containing only that Orçamento's currently-selected items, their prices, and a grand total

#### Scenario: Non-selected items excluded from the PDF
- **WHEN** a linked Orçamento has line items that are not the currently selected fulfillment for their item
- **THEN** `pantheon-service` excludes those line items from that Orçamento's PDF

#### Scenario: PDF opens with the company's branding
- **WHEN** a construction site member requests a Pedido de Compra PDF for a company that has a logo configured
- **THEN** `pantheon-service`'s PDF opens with that logo, the company's name, and the construction site's name

#### Scenario: Requester and approval metadata shown
- **WHEN** a construction site member requests the PDF for a Pedido de Compra that has been submitted and had at least one approval step decided
- **THEN** `pantheon-service`'s PDF shows who created the header and its opening date, and each decided step's approver name (or "Equipe da empresa") and function

#### Scenario: In-progress header shows no finalization date
- **WHEN** a construction site member requests the PDF for a Pedido de Compra that has not yet reached `CONCLUIDO`
- **THEN** `pantheon-service`'s PDF indicates the header is still in progress instead of showing a finalization date

### Requirement: Pedido de Compra consolidated summary PDF
`pantheon-service` SHALL generate, for a given Pedido de Compra, a single PDF opening with the same branded header and requester/approval/date metadata block as the per-supplier PDF, followed by every one of its `PurchaseRequestItem`s with its quantity, its currently selected supplier's name (or "Não selecionado" when it has no current selection), the selected line item's unit price, and the resulting line total; followed by one subtotal per supplier that has at least one selected item ("valor a pagar" per supplier), and a grand total across all suppliers. This PDF coexists with, and does not replace, the existing per-supplier PDF.

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

#### Scenario: Summary PDF also opens with the branded header and metadata
- **WHEN** a construction site member requests the consolidated summary PDF
- **THEN** `pantheon-service`'s PDF opens with the same company/site branding and requester/approval/date metadata block as the per-supplier PDF
