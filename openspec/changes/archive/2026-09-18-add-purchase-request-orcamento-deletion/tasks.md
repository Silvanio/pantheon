## 1. Backend — exceptions

- [x] 1.1 Add `PurchaseRequestNotDeletableException` (`pantheon-service/src/main/java/com/pantheon/service/exception/`), constructed with the `PurchaseRequest` id, following the existing style of `PurchaseRequestNotOrcadoException`.
- [x] 1.2 Add `OrcamentoNotDeletableException`, constructed with the `Orcamento` id, following the existing style of `OrcamentoNotDraftException`.
- [x] 1.3 Register both in `ConstructionExceptionHandler`, mapped to HTTP 409, matching how `PurchaseRequestNotOrcadoException`/`OrcamentoNotDraftException` are already handled there.

## 2. Backend — Pedido de Compra deletion

- [x] 2.1 In `PurchaseRequestService`, add `@Transactional public void delete(UUID purchaseRequestId, UUID actingUserId)`: load the header (`requirePurchaseRequest`), require `PURCHASE_REQUEST` manage access (`requireManage`), throw `PurchaseRequestNotDeletableException` unless `status == INICIADO`, then delete all of its `PurchaseRequestItem`s (`itemRepository.deleteAll(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(...))` or add a repository `deleteByPurchaseRequestId` method) and finally delete the `PurchaseRequest` row itself.
- [x] 2.2 Add `@DeleteMapping("/api/purchase-requests/{id}")` to `PurchaseRequestController` calling the new service method and returning `204 No Content`.
- [x] 2.3 Add a `PurchaseRequestServiceTest` covering: deleting an `INICIADO` header removes it and its items; deleting an `ORCADO`/`CONFERIDO`/`CONCLUIDO` header throws `PurchaseRequestNotDeletableException`; a member without `PURCHASE_REQUEST` access is rejected.

## 3. Backend — Orçamento deletion

- [x] 3.1 In `OrcamentoService`, add `@Transactional public void delete(UUID orcamentoId, UUID actingUserId)`: load the Orçamento (`requireOrcamento`), require `ORCAMENTO_MANAGE` manage access, throw `OrcamentoNotDeletableException` unless `status == DRAFT`.
- [x] 3.2 Within that method, before deleting the Orçamento's line items: if `sourcePurchaseRequestId != null`, look up that header's `PurchaseRequestItem`s and, for each one whose `convertedToOrcamentoId` equals this Orçamento's id, revert it (`status = PENDING`, clear `convertedToOrcamentoId`/`convertedAt` — add a `revertConversion()` method on `PurchaseRequestItem` mirroring the existing `convertTo(...)`/`clearSelection()` methods) and save it; for any of that header's items whose `selectedOrcamentoLineItemId` matches one of this Orçamento's line-item ids, call the existing `clearSelection()` and save it.
- [x] 3.3 Delete the Orçamento's `OrcamentoLineItem`s, then delete the `Orcamento` row.
- [x] 3.4 If `sourcePurchaseRequestId != null`, re-check `orcamentoRepository.findBySourcePurchaseRequestId(sourcePurchaseRequestId)` after the delete; if empty and the header's status is `ORCADO`, revert the header to `INICIADO` (add a `revertToIniciado()` method on `PurchaseRequest` mirroring `markOrcado()`) and save it.
- [x] 3.5 Add `@DeleteMapping("/api/orcamentos/{id}")` to `OrcamentoController` calling the new service method and returning `204 No Content`.
- [x] 3.6 Add an `OrcamentoServiceTest` covering: deleting a standalone `DRAFT` Orçamento; deleting a converted `DRAFT` Orçamento reverts its items to `PENDING` and clears `convertedToOrcamentoId`; deleting it also clears a `selectedOrcamentoLineItemId` that pointed at one of its line items; deleting the header's last linked Orçamento reverts the header from `ORCADO` to `INICIADO`; deleting one of two linked Orçamentos leaves the header `ORCADO`; deleting a `LOCKED` Orçamento throws `OrcamentoNotDeletableException`; a member without `ORCAMENTO_MANAGE` access is rejected.

## 4. Frontend — composables

- [x] 4.1 In `usePurchaseRequests.ts`, add `deletePurchaseRequest(purchaseRequestId: string): Promise<void>` calling `DELETE /api/purchase-requests/${purchaseRequestId}` via `authFetch` (handles the existing 204-body case already supported by `authFetch`... verify it does; if not, reuse the same 204 handling already present in `useOrcamentos.ts`'s `authFetch`).
- [x] 4.2 In `useOrcamentos.ts`, add `deleteOrcamento(orcamentoId: string): Promise<void>` calling `DELETE /api/orcamentos/${orcamentoId}`, matching the existing `removeLineItem` pattern.

## 5. Frontend — views

- [x] 5.1 In `PurchaseRequestDetailView.vue`, add a "Excluir" button next to the existing submit/conclude actions, visible only when `detail.purchaseRequest.status === 'INICIADO'`; on click, `window.confirm(t('purchaseRequests.deleteConfirm'))`, then call `deletePurchaseRequest`, and on success `router.push('/...')` back to the site's Pedido de Compra list (match whatever route `router.back()`/the list view uses elsewhere in this file); show `purchaseRequests.deleteError` on failure.
- [x] 5.2 In `OrcamentoDetailView.vue`, add a "Excluir" button in the header area, visible only when `isDraft` is true; on click, `window.confirm(t('orcamento.deleteConfirm'))`, then call `deleteOrcamento`, and on success navigate back to the site's Orçamentos list; show `orcamento.deleteError` on failure.

## 6. i18n

- [x] 6.1 Add to `pt-BR.json` under `orcamento`: `deleteButton` ("Excluir"), `deleteConfirm` ("Tem certeza que deseja excluir este orçamento? Esta ação não pode ser desfeita."), `deleteError` ("Não foi possível excluir o orçamento.").
- [x] 6.2 Add to `pt-BR.json` under `purchaseRequests`: `deleteButton` ("Excluir"), `deleteConfirm` ("Tem certeza que deseja excluir este pedido de compra? Esta ação não pode ser desfeita."), `deleteError` ("Não foi possível excluir o pedido de compra.").

## 7. Verification

- [x] 7.1 Backend: `./mvnw -pl pantheon-service test` green, including the new `PurchaseRequestServiceTest`/`OrcamentoServiceTest` cases.
- [x] 7.2 Frontend: `npm run build` green in `pantheon-web` (vue-tsc + vite build, no errors).
- [ ] 7.3 Manual/browser check via the `run` workflow: create a Pedido de Compra, delete it while Iniciado (disappears from the list); create another, convert its items into an Orçamento, delete that Orçamento (items become Pending again, header shows Iniciado again, delete button reappears on the header); confirm the delete buttons are absent once a header is Orçado/Conferido/Concluído or an Orçamento is Bloqueado. NOT completed by the agent — requires an authenticated login, which entering credentials through browser automation is outside what this agent will do on its own. Backend/frontend builds and full automated test suites (200/200 backend tests, including the new delete-flow cases) are green; a human should do this pass before archiving.
- [x] 7.4 Run `openspec validate --strict` against the change before archiving.
