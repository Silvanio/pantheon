## Context

`restructure-materials-orcamento-flow` (implemented, archived) introduced a flat, site-scoped `PurchaseRequestItem` list (no header/document entity) and an `Orcamento` with no supplier/company information. This change adds a header entity for Pedido de Compra, a company-scoped supplier registry, and supplier data on Orçamento, plus filtering and a more elaborate Orçamento screen. It builds directly on the existing multi-level approval mechanism (`SiteOrcamentoApprovalLevel`/`OrcamentoApproval`) and delivery-tracking (`Material`), neither of which changes here.

Two Orçamentos (and their dependent purchase-request items and materials) exist in the dev database from manual QA of the prior change. They carry no business value and are cleared by this change's migration so the new `NOT NULL` supplier columns don't need a backfill story.

## Goals / Non-Goals

**Goals:**
- Group Pedido de Compra items under a named, dated header; allow a single header to spawn more than one Orçamento over time as its items are converted in batches.
- Record supplier (Fornecedor) data on every Orçamento, reusing a company-level registry with CNPJ-prefix autocomplete and find-or-create semantics.
- Filter Pedidos de Compra by date, and Orçamentos by date and by originating Pedido de Compra.
- Make the Orçamento screen easier to read: supplier block, originating-Pedido link, clearer status/approval presentation, organized totals, distinct materials section.

**Non-Goals:**
- CNPJ checksum/format validation (mod-11 or similar) — out of scope, matching the existing free-text `Company.cnpj` field. Only presence (`NotBlank`) is enforced.
- Editing an existing Fornecedor's data, or a standalone Fornecedor management screen — the only write path is find-or-create during Orçamento creation.
- Any change to the approval-level configuration, approval-step mechanics, or Material delivery-tracking status machine.
- Cross-Pedido-de-Compra selection in a single conversion action (explicitly excluded — see Decision 2).

## Decisions

### 1. `PurchaseRequest` is a real header entity; `PurchaseRequestItem` keeps its own `constructionSiteId`
`PurchaseRequest`: id, constructionSiteId, name, createdBy, createdAt. `PurchaseRequestItem` gains a required `purchaseRequestId` FK but keeps its existing `constructionSiteId` column (denormalized) so the current site-scoped listing/filtering queries don't need a join rewrite. The header's name is generated server-side at creation: `"Pedido " + ddMMyyyy + " #" + (n+1)`, where `n` is the count of `PurchaseRequest` rows for that site whose `createdAt` falls on the same UTC calendar day. UTC is used because no site-level timezone field exists yet; a request made right at UTC midnight could in theory land in the "wrong" day's sequence — an accepted, cosmetic edge case, not a correctness issue (the header is still uniquely identified by id).

**Alternative considered**: keep items flat and derive a synthetic "pedido" grouping from `createdAt` proximity. Rejected — arbitrary time-windowing is fragile and doesn't give a stable id to link an Orçamento back to, which the filtering requirement needs.

### 2. Conversion is scoped to one `PurchaseRequest` at a time; Orçamento stores `sourcePurchaseRequestId` directly
`convertToOrcamento` now takes `(siteId, actingUserId, purchaseRequestId, itemIds)`; every id in `itemIds` must belong to `purchaseRequestId` and be `PENDING`, otherwise the whole conversion is rejected. The created Orçamento gets `sourcePurchaseRequestId = purchaseRequestId`. Unconverted items remain on the same header, so a second conversion later produces a second Orçamento with the same `sourcePurchaseRequestId` — this is exactly "a Pedido de Compra can have more than one Orçamento," and it makes the "filter Orçamentos by Pedido de Compra" requirement a plain equality filter with no join through line items.

**Alternative considered**: let a single conversion span items from multiple headers, and derive an Orçamento's "source pedidos" as a set. Rejected — the user's requirement is specifically framed as one Pedido de Compra producing multiple Orçamentos, not the reverse; a single scalar `sourcePurchaseRequestId` is simpler and sufficient, and per-item traceability already exists via `OrcamentoLineItem.sourcePurchaseRequestItemId`.

### 3. `Fornecedor` is company-scoped, not site-scoped
New entity: id, companyId, cnpj, name, address (nullable), contactName (nullable), contactPhone (nullable), createdBy, createdAt; unique on `(companyId, cnpj)`. Resolved from a site via `ConstructionSite.companyId` (same pattern `SiteAccessService` already uses for company-staff resolution). CNPJ-prefix search (`GET` under the site's purchase/orçamento routes, resolving to the site's company internally) requires only site access (any active member), matching the read-only nature of a lookup; no `MANAGE` capability needed to search.

### 4. Orçamento snapshots supplier data; find-or-create happens once, at creation
`Orcamento` gains `fornecedorCnpj` (not null), `fornecedorNome` (not null), `fornecedorEndereco`, `fornecedorContatoNome`, `fornecedorContatoTelefone` (all nullable), and `sourceFornecedorId` (nullable FK, traceability only — never read back to "fix up" the Orçamento). `OrcamentoService.create` (both the blank-creation and from-purchase-request-items paths) takes a supplier payload, calls `FornecedorService.findOrCreate(companyId, actingUserId, payload)` — returns the existing `Fornecedor` if one matches `(companyId, cnpj)` exactly (ignoring any differences in the other submitted fields), otherwise creates a new one — and copies its fields onto the new Orçamento. This mirrors the line-item snapshot convention from the prior change: copy at the moment of creation, keep an optional origin pointer, never a live reference.

**Alternative considered**: let the Orçamento hold only `fornecedorId` and read supplier fields live from `Fornecedor`. Rejected — an Orçamento is a historical document; if a Fornecedor's address or contact changes later, past Orçamentos should keep showing what was true when they were made, consistent with how line items already work.

### 5. Filtering is query-parameter based, no new listing endpoints
`GET .../purchase-requests?date=` and `GET .../orcamentos?date=&purchaseRequestId=` add optional query parameters to the existing list endpoints rather than introducing parallel search endpoints. `date` filters `createdAt`'s UTC calendar day equality (same convention as the header-naming sequence in Decision 1).

### 6. Clean-cut migration for the two dev-only test Orçamentos
The migration deletes the 2 existing `orcamento` rows and their dependents (`orcamento_line_item`, `orcamento_approval`, `material`, and any `material_delivery_photo`) and the 2 `purchase_request_item` rows referencing them, before adding the new `NOT NULL` columns — consistent with the "clean-cut, no data carry-forward" precedent already established for dev-only data in the prior change.

## Risks / Trade-offs

- **UTC-day sequencing**: a Pedido de Compra created a few hours before/after local midnight could get a sequence number that looks "off" to a user in a different timezone from UTC. Accepted as cosmetic; revisit if/when the platform gains a per-company timezone setting.
- **Supplier data staleness**: because Orçamento snapshots supplier fields, a Fornecedor's address/phone edited after the fact (not currently possible — no edit path exists — but foreseeable later) would not retroactively update past Orçamentos. This is intentional (see Decision 4) but worth flagging for future supplier-editing work.
- **Breaking change to conversion API**: `convertToOrcamento` now requires a `purchaseRequestId` and rejects cross-header selections. The frontend's `PurchaseRequestPanel.vue` is being reworked in this same change, so there are no external callers left on the old signature.
