## 1. Backend — allow re-converting an item

- [x] 1.1 In `PurchaseRequestItemService.convertToOrcamento`, remove the `if (item.getStatus() != PENDING) throw new PurchaseRequestItemAlreadyConvertedException(...)` check (keep the `ItemsSpanMultiplePurchaseRequestsException` cross-header check).
- [x] 1.2 In the same method's post-creation loop, only call `item.convertTo(orcamento.getId(), now)` (and save) when `item.getStatus() == PENDING`; leave an already-`CONVERTED` item untouched.
- [x] 1.3 Delete `PurchaseRequestItemAlreadyConvertedException` and its `ConstructionExceptionHandler` mapping (now unused).
- [x] 1.4 Update `PurchaseRequestItemServiceTest`: remove/replace the test asserting `PurchaseRequestItemAlreadyConvertedException` on re-conversion with one asserting a `CONVERTED` item can be converted again into a second Orçamento, unchanged status/`convertedToOrcamentoId`.

## 2. Backend — fix Orçamento-deletion revert to not clobber still-quoted items

- [x] 2.1 Add `PurchaseRequestItem.repointConversion(UUID orcamentoId)`: sets `convertedToOrcamentoId` to the given id, leaves `status` and `convertedAt` unchanged (mirrors `revertConversion()`'s shape).
- [x] 2.2 In `OrcamentoService.delete`, before the existing revert loop, capture the header's *other* linked Orçamento ids (`orcamentoRepository.findBySourcePurchaseRequestId(sourcePurchaseRequestId)` minus the one being deleted — call this before deleting the row).
- [x] 2.3 In the revert loop, when an item's `convertedToOrcamentoId` equals the deleted Orçamento's id: query `orcamentoLineItemRepository.findByOrcamentoIdInAndSourcePurchaseRequestItemId(otherOrcamentoIds, item.getId())`; if non-empty, call `repointConversion(...)` with the first match's `orcamentoId` instead of `revertConversion()`.
- [x] 2.4 Add `OrcamentoServiceTest` coverage: deleting the Orçamento recorded on an item that is *also* quoted by another still-linked Orçamento re-points `convertedToOrcamentoId` to that other Orçamento and leaves `status` `CONVERTED` (does not revert to `PENDING`).

## 3. Frontend — select already-converted items too

- [x] 3.1 In `PurchaseRequestDetailView.vue`'s "Convertidos em orçamento" (`convertedItems`) list, add the same checkbox (`toggleSelection`/`selectedIds.has(item.id)`) as the pending list, so a converted item can be included in a new "Criar orçamento" selection.
- [x] 3.2 Verify visually (or via existing composable/type wiring) that `onConvertConfirmed`'s call to `convertToOrcamento` needs no change — it already just forwards `Array.from(selectedIds.value)` regardless of item status.

## 4. Verification

- [x] 4.1 Backend: `./mvnw -pl pantheon-service test` green.
- [x] 4.2 Frontend: `npm run build` green in `pantheon-web`.
- [x] 4.3 Run `openspec validate --strict` against the change before archiving.
