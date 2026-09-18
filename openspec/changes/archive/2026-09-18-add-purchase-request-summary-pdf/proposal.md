## Why

A Pedido de Compra with items split across multiple suppliers already has a per-supplier PDF (one document per linked Orçamento, listing only that supplier's selected items — kept as-is). There is no single document showing the whole picture: every product, which supplier will actually fulfill it, and how much is owed to each supplier in total. Whoever executes the purchases needs that one-page overview instead of opening every supplier PDF separately.

## What Changes

- `pantheon-service` SHALL generate, for a given Pedido de Compra, a single consolidated PDF listing every item with its selected supplier, unit price, and line total (or "Não selecionado" when an item has no current selection), followed by a per-supplier subtotal ("valor a pagar") and a grand total across all suppliers.
- The existing per-supplier PDF (`GET /api/purchase-requests/{id}/orcamentos/{orcamentoId}/pdf`) is unchanged and stays available alongside the new one.
- `pantheon-web`: a "Baixar resumo" action next to the comparison table, downloading the consolidated PDF — separate from each column's existing "Imprimir PDF" per-supplier action.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `purchase-requests`: adds a new consolidated-summary-PDF requirement alongside the existing per-supplier one, and extends the Purchase-request views requirement with the new UI action.

## Impact

- `pantheon-service`: `PurchaseRequestPdfService` (new `generateSummary` method reusing its existing HTML-to-PDF render pipeline), `PurchaseRequestController` (new endpoint).
- `pantheon-web`: `usePurchaseRequests.ts`, `PurchaseRequestDetailView.vue`, `pt-BR.json`.
