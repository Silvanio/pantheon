## Why

The Pedido de Compra list is currently a flat, unnamed collection of items with no way to group a day's requests or tell them apart, and an Orçamento carries no record of which supplier ("fornecedor") actually quoted it — so there's no CNPJ, name, address, or contact on file, and no way to reuse a known supplier across obras of the same company. A single Pedido de Compra also can't be split across more than one Orçamento today. Together this makes the purchasing flow hard to audit and the Orçamento screen hard to read at a glance.

## What Changes

- `PurchaseRequestItem`s are grouped under a new `PurchaseRequest` header (a "Pedido de Compra" document) with an auto-generated name — date plus a same-day sequence per site, e.g. "Pedido 16/02/2026 #1", "Pedido 16/02/2026 #2".
- Converting items into an Orçamento now operates on a single Pedido de Compra at a time: selecting a subset of its still-`PENDING` items creates one Orçamento; the remaining items stay available on that same Pedido de Compra to seed another Orçamento later. **BREAKING**: conversion no longer accepts item ids spanning more than one Pedido de Compra.
- New company-scoped `Fornecedor` (supplier) registry: CNPJ (required), name (required), address and contact name/phone (optional). Not tied to a single obra — reusable across every construction site of the owning `Company`.
- Orçamento creation now requires supplier data (CNPJ + name required, address/contact optional). Typing a CNPJ (from the 5th digit) queries existing suppliers of the site's company by CNPJ prefix for autocomplete; picking a match fills the remaining fields. If no match exists, the user finishes the fields manually and the backend finds-or-creates the `Fornecedor` by (company, CNPJ). The Orçamento stores a snapshot of the supplier fields at creation time plus an optional traceability link to the `Fornecedor` record.
- Orçamento gains an optional traceability link to its originating `PurchaseRequest`, used both for display and for filtering.
- Purchase-request and Orçamento listings gain filters: Pedido de Compra by date, Orçamento by date and by originating Pedido de Compra.
- `OrcamentoDetailView.vue` and the creation flows are reworked to be more elaborate and legible: supplier details, originating Pedido de Compra (linked), clearer status/approval-timeline presentation, an organized line-item section with totals when unit prices are present, and a clearly separated post-conclusion materials section.
- Existing manual QA/test rows in `orcamento`, `purchase_request_item`, and `material` (2 Orçamentos and their dependents) are cleared as part of the migration so the new required supplier columns can be added `NOT NULL` without backfill — disposable test data, no business value.

## Capabilities

### New Capabilities
- `supplier-registry`: company-scoped Fornecedor records (CNPJ, name, address, contact) and CNPJ-prefix search, reused across a company's construction sites.

### Modified Capabilities
- `purchase-requests`: introduces the `PurchaseRequest` header (auto-generated name, date-based sequence), scopes item listing/filtering to it, and restricts conversion-into-Orçamento to items of a single Pedido de Compra.
- `orcamento-approval-workflow`: Orçamento creation requires supplier (Fornecedor) data with find-or-create-by-CNPJ semantics and a snapshot on the Orçamento; Orçamento gains an optional originating-Pedido-de-Compra link; listing gains date/Pedido-de-Compra filters; the Orçamento detail/creation UI is redesigned for clarity.

## Impact

- **pantheon-service**: new `PurchaseRequest` and `Fornecedor` entities/repositories/exceptions; `PurchaseRequestItem` gains a required `purchaseRequestId`; `Orcamento` gains supplier snapshot fields, `sourceFornecedorId`, and `sourcePurchaseRequestId`; `PurchaseRequestItemService` split into request-header creation + item listing/conversion; new `FornecedorService`/`FornecedorController` (CNPJ search, used from `OrcamentoService`'s find-or-create); `OrcamentoService.create`/`createFromPurchaseRequestItems` signatures change to accept supplier data; new Flyway migrations (new tables, new columns, clearing the 2 existing test Orçamento rows and their dependents).
- **pantheon-web**: `usePurchaseRequests.ts` and `useOrcamentos.ts` composables updated for headers/suppliers/filters; `PurchaseRequestPanel.vue` reworked around Pedido de Compra documents; new supplier-picker component (CNPJ autocomplete) used from both Orçamento-creation entry points; `OrcamentoListPanel.vue` and `OrcamentoDetailView.vue` redesigned; `pt-BR.json` updated.
- No changes to the already-implemented multi-level approval mechanism (`SiteOrcamentoApprovalLevel`/`OrcamentoApproval`) or to `material-delivery-tracking`.
