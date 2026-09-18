## 1. Backend

- [x] 1.1 In `PurchaseRequestPdfService`, add `public byte[] generateSummary(UUID purchaseRequestId, UUID actingUserId)`: load the header, `requireVisible` `PURCHASE_REQUEST` (same access check as `generate`); load its items (`itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc`); resolve the distinct set of `selectedOrcamentoLineItemId`s via `lineItemRepository.findAllById(...)` into a `Map<UUID, OrcamentoLineItem>`; resolve the distinct set of those line items' `orcamentoId`s via `orcamentoRepository.findAllById(...)` into a `Map<UUID, Orcamento>`.
- [x] 1.2 Add a `buildSummaryHtml(PurchaseRequest, List<PurchaseRequestItem>, Map<UUID, OrcamentoLineItem>, Map<UUID, Orcamento>)` private method: one table row per item (name, quantity, supplier name or "Não selecionado", unit price, line total — blank price/total when unselected), accumulating a `Map<UUID orcamentoId, BigDecimal>` subtotal per supplier as it goes; after the item table, a second small table of per-supplier subtotals (supplier name + its subtotal), then a grand total line. Reuse the existing `escape`/style-block conventions from `buildHtml`.
- [x] 1.3 `generateSummary` calls `renderPdf` on the built HTML, same as `generate` does.
- [x] 1.4 `PurchaseRequestController`: add `GET /api/purchase-requests/{id}/summary-pdf` returning `byte[]` with `Content-Type: application/pdf` and `Content-Disposition: inline; filename="resumo-pedido-{id}.pdf"`, mirroring the existing `getSupplierPdf` endpoint's response-building style.

## 2. Backend — tests

- [x] 2.1 `PurchaseRequestPdfServiceTest`: new cases — summary lists every item with the right supplier/price/total for a multi-supplier selection; an unselected item appears with no price and is excluded from subtotals; per-supplier subtotals and the grand total are correct; a member without `PURCHASE_REQUEST` visibility is rejected.
- [x] 2.2 `./mvnw -pl pantheon-service test` green.

## 3. Frontend

- [x] 3.1 `usePurchaseRequests.ts`: add `getSummaryPdfBlob(purchaseRequestId)` mirroring `getSupplierPdfBlob`, calling `GET /api/purchase-requests/${purchaseRequestId}/summary-pdf`.
- [x] 3.2 `PurchaseRequestDetailView.vue`: add a "Baixar resumo" button next to the comparison table's title (alongside, not replacing, each column's "Imprimir PDF"), with its own loading/error state, downloading via the same blob+`URL.createObjectURL` pattern as `onPrintPdf`.
- [x] 3.3 `pt-BR.json`: add `purchaseRequests.comparison.summaryButton`, `.summaryPrinting`, `.summaryError`.
- [x] 3.4 `npm run build` green in `pantheon-web`.

## 4. Verification

- [x] 4.1 Run `openspec validate --strict` against the change before archiving.
