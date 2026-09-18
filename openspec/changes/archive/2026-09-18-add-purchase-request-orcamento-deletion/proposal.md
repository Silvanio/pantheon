## Why

There is currently no way to undo a mistaken "Pedido de Compra" or "Orçamento": a header created with the wrong items, or a quote started for the wrong supplier, stays in the list forever with no removal path (only individual Orçamento line items can be removed, via `DELETE /api/orcamentos/{id}/line-items/{lineItemId}`). Users need an explicit way to delete a Pedido de Compra or an Orçamento while it is still safe to do so — i.e. before real downstream work (approval, quoting, material delivery) depends on it.

## What Changes

- Add `DELETE /api/purchase-requests/{id}`: deletes a `PurchaseRequest` (and its `PurchaseRequestItem`s) when it is still `INICIADO` (no `Orcamento` has been created from it yet) and the acting member has `PURCHASE_REQUEST` manage access. Rejected with a conflict for any later status.
- Add `DELETE /api/orcamentos/{id}`: deletes an `Orcamento` (and its `OrcamentoLineItem`s) when it is `DRAFT` and the acting member has `ORCAMENTO_MANAGE` access. Rejected with a conflict when `LOCKED`.
  - If the Orçamento was converted from a Pedido de Compra, deletion reverts its converted `PurchaseRequestItem`s back to `PENDING` (clearing `convertedToOrcamentoId`/`convertedAt`) and clears `selectedOrcamentoLineItemId` on any item that had selected one of its line items, so the header's items remain consistent and re-convertible.
  - If that was the header's last remaining linked Orçamento, the header reverts from `ORCADO` back to `INICIADO` (the inverse of the automatic `INICIADO` → `ORCADO` transition on first conversion).
- `pantheon-web`: add a "Excluir" action, with a confirmation prompt, on the Pedido de Compra detail view (visible only while `Iniciado`) and the Orçamento detail view (visible only while `Rascunho`/`DRAFT`), navigating back to the respective list on success.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `purchase-requests`: adds the ability to delete a Pedido de Compra header while `INICIADO`.
- `orcamento-management`: adds the ability to delete an Orçamento while `DRAFT`, including reverting its source Pedido de Compra's converted items and status.

## Impact

- `pantheon-service`: `PurchaseRequestController`/`PurchaseRequestService`, `OrcamentoController`/`OrcamentoService`, `PurchaseRequestItemRepository`, `OrcamentoLineItemRepository`; new exceptions for the "not deletable in this status" cases.
- `pantheon-web`: `usePurchaseRequests.ts`, `useOrcamentos.ts`, `PurchaseRequestDetailView.vue`, `OrcamentoDetailView.vue`, `pt-BR` locale strings.
- No schema migration needed — deletion uses existing tables/relationships (cascading deletes of items/line items scoped to the deleted header/orçamento).
