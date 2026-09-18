## Context

`PurchaseRequestPdfService.generate` already renders a supplier-scoped PDF from a Pedido de Compra's items: for a given Orçamento, it filters the header's items down to the ones whose `selectedOrcamentoLineItemId` belongs to that Orçamento, and renders name/quantity/unit-price/line-total plus a grand total. The consolidated summary is the same underlying data (every item's current selection) with two differences: it is not scoped to one Orçamento (every item appears, whichever supplier it's assigned to, or none), and it adds a per-supplier subtotal grouping on top of the existing line-item table and grand total.

## Goals / Non-Goals

**Goals:**
- One PDF, one request, showing every item + its assigned supplier + a per-supplier subtotal + a grand total.
- Reuse the existing HTML-to-PDF pipeline (`openhtmltopdf`, `useFastMode`, the same inline `<style>` block) rather than introducing a second rendering approach.

**Non-Goals:**
- Changing or removing the existing per-supplier PDF — both coexist, per the proposal ("Devemos manter").
- Requiring every item to be selected before the summary can be generated. An item with no current selection is shown as "Não selecionado" with no price, rather than blocking the whole document — this is a reporting tool, not a gate on the approval workflow (which already has its own "every item selected" requirement for submission).

## Decisions

**1. New method `generateSummary(purchaseRequestId, actingUserId)` on the existing `PurchaseRequestPdfService`, not a new service.**
It shares the same access-check (`requireVisible` `PURCHASE_REQUEST`), the same `escape`/`renderPdf` helpers, and conceptually the same "walk the header's items, resolve each one's selection" logic as `generate` already has — splitting it into a separate service would just duplicate that plumbing.

**2. Per-supplier subtotal is computed by grouping the same line-total figures already computed for the per-item table, keyed by the selected line item's Orçamento id — not a second query.**
One pass over the header's items, resolving each one's `OrcamentoLineItem` (via `lineItemRepository.findAllById` on the distinct set of `selectedOrcamentoLineItemId`s) and each line item's `Orcamento` (via `orcamentoRepository.findAllById` on the distinct set of `orcamentoId`s from those line items), builds both the per-item rows and a running `Map<UUID orcamentoId, BigDecimal>` subtotal in the same loop.

**3. Unselected items are listed, not omitted.**
An item with `selectedOrcamentoLineItemId == null` still gets a row (name, quantity, "Não selecionado", no price), so the summary doubles as a quick check for "did I forget to pick a supplier for something" — consistent with showing the full item list rather than silently hiding gaps.

## Risks / Trade-offs

- **[Trade-off] No caching — regenerated on every request,** identical to the existing per-supplier PDF's behavior (`{@code never cached}` per its own doc comment). Consistent, and the documents are cheap to render (small tables, no images).

## Migration Plan

Purely additive: one new service method, one new `GET` endpoint, one new UI button. No schema change, no change to the existing per-supplier PDF endpoint or its output. Rollback is a normal revert.

## Open Questions

None.
