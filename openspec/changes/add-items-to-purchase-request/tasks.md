## 1. Backend

- [x] 1.1 `PurchaseRequestCreationRequest`: drop `@NotEmpty` from `items` (keep `@Valid` so any present items are still validated); confirm `PurchaseRequestService.create` already handles an empty/zero-length list correctly (it iterates the list, so no logic change should be needed — verify with a test).
- [x] 1.2 Add `PurchaseRequestNotIniciadoException` (mirrors `PurchaseRequestNotOrcadoException`); map it in `ConstructionExceptionHandler` to HTTP 409.
- [x] 1.3 `PurchaseRequestService.addItems(UUID purchaseRequestId, UUID actingUserId, List<PurchaseRequestItemCreationRequest> items)`: `requirePurchaseRequest`, `requireManage(purchaseRequest.getConstructionSiteId(), actingUserId)`, reject with `PurchaseRequestNotIniciadoException` if `status != INICIADO`; save each item the same way `create()` does (new `PurchaseRequestItem` per row, `PENDING` status); return the saved items.
- [x] 1.4 `PurchaseRequestController`: `POST /api/purchase-requests/{id}/items`, `@RequestBody PurchaseRequestCreationRequest` (reuse), returns `List<PurchaseRequestItemResponse>` with HTTP 201.

## 2. Backend tests

- [x] 2.1 `PurchaseRequestServiceTest`: `create` with an empty items list persists a header with zero items in `INICIADO` status.
- [x] 2.2 `PurchaseRequestServiceTest`: adding items to an `INICIADO` header succeeds and the items appear in a subsequent list/get; adding items to a non-`INICIADO` header (e.g. after marking `ORCADO`) throws `PurchaseRequestNotIniciadoException`; a member without `MANAGE` is rejected.
- [x] 2.3 Run `./mvnw test` from `pantheon-service/` and confirm the full suite passes.

## 3. Frontend

- [x] 3.1 `usePurchaseRequests.ts`: add `addPurchaseRequestItems(purchaseRequestId, items): Promise<PurchaseRequestItem[]>` (`POST /api/purchase-requests/{id}/items`), reusing whatever item-input type the create-request call already uses. Confirm the existing create call can be invoked with `items: []`.
- [x] 3.2 `PurchaseRequestPanel.vue`: remove the item-rows create-modal entirely (`showForm`, `rows`, `addRow`, `removeRow`, `onSubmitForm`, and the modal template block). Change the "Novo pedido" button's handler to call create with `items: []` and, on success, `router.push('/purchase-requests/${created.id}')`.
- [x] 3.3 `PurchaseRequestDetailView.vue`: add a `showAddItemsForm` ref and a `rows`-style repeatable item state (move the field markup/add-row/remove-row logic here from the now-removed `PurchaseRequestPanel.vue` form); add an "Adicionar produtos" button next to the existing action buttons, visible only when `detail.value?.purchaseRequest.status === 'INICIADO'` and the viewer's resolved access lets them manage (same gating already used for `canDelete`) — regardless of current item count; wrap the form in the `fixed inset-0 z-20 ... bg-black/40` centered `.modal-panel` dialog pattern (same as the six flows just converted elsewhere in this codebase). On submit, call `addPurchaseRequestItems`, reload the detail (`loadDetail`/equivalent already used after other mutations in this file), close the modal, and clear the row state; on error, show it inline in the modal without closing it.
- [x] 3.4 Add the new `pt-BR.json` strings under `purchaseRequests` (e.g. `addItemsButton: "Adicionar produtos"`, plus any new form-specific strings — reuse `purchaseRequests.form.*`/`purchaseRequests.addRowButton`/`purchaseRequests.removeRowButton` where the shape matches instead of duplicating; drop the now-unused `purchaseRequests.newButton`-modal-specific strings only if nothing else references them).

## 4. Verification

- [x] 4.1 Backend: `./mvnw test` green.
- [x] 4.2 Frontend: `npx vue-tsc --noEmit` and `npm run build` green.
- [x] 4.3 Static/live check: confirm "Novo pedido" creates immediately (no modal) and lands on the new empty header's detail page; confirm "Adicionar produtos" shows there right away (zero items, `Iniciado`); confirm it still shows on a header that already has items while `Iniciado`; confirm submitting adds items to the visible list without a full page reload; confirm the action disappears once the header moves to `ORCADO`.
