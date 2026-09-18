## Context

`PurchaseRequest` (Pedido de Compra) and `Orcamento` (budget/quote) both currently support create, list, detail, and (for Orçamento) per-line-item mutation, but neither supports deleting the whole record. The domain already has a well-defined status lifecycle for both (`PurchaseRequestStatus{INICIADO,ORCADO,CONFERIDO,CONCLUIDO}`, `OrcamentoStatus{DRAFT,LOCKED}`), and existing mutations (`addLineItem`/`updateLineItem`/`removeLineItem`) already gate on status (`DRAFT` only). Deletion needs to fit into that same lifecycle without leaving dangling references: a `PurchaseRequestItem.convertedToOrcamentoId`/`selectedOrcamentoLineItemId` pointing at a row that no longer exists, or a header stuck `ORCADO` with zero linked Orçamentos.

## Goals / Non-Goals

**Goals:**
- Let a member with the existing manage permission (`PURCHASE_REQUEST` / `ORCAMENTO_MANAGE`) delete a header/Orçamento while it is still low-risk to do so (no approval activity, no locked quote).
- Keep the remaining data consistent after a delete: no orphaned foreign-key-style references, and status fields that still reflect reality (e.g. a header with no more linked Orçamentos goes back to `INICIADO`).
- Reuse the existing permission/status-guard patterns already in `PurchaseRequestService`/`OrcamentoService` rather than introducing a new authorization model.

**Non-Goals:**
- Deleting a `PurchaseRequest` or `Orcamento` that has already entered the approval workflow (`ORCADO` and beyond for headers, `LOCKED` for Orçamentos). Unwinding an in-flight approval is already handled by the existing reject-step flow, which returns a header to `ORCADO` and unlocks its Orçamentos — deletion is not a substitute for that.
- Soft-delete / audit trail / undo. This is a hard delete, matching the existing `removeLineItem` behavior.
- Cascading deletion across unrelated aggregates (e.g. `Material` delivery records) — deletion is only reachable before those records can exist (materials are only created at `conclude()`, which requires `CONFERIDO`, a status strictly later than any status this change allows deletion from).

## Decisions

**1. Header deletion is gated on `INICIADO` only.**
`INICIADO` is precisely the status before any `Orcamento` has ever been created from the header (the first conversion is what triggers `INICIADO → ORCADO`, and there is no path back to `INICIADO` other than the new "last linked Orçamento deleted" transition this change adds — see Decision 3). Gating on `INICIADO` means a header's deletion never has to cascade into any `Orcamento` or approval record — only its own `PurchaseRequestItem`s, all of which are still `PENDING` at that point. This keeps the operation a simple two-table delete with no cross-aggregate cleanup.
- *Alternative considered*: allow deletion up through `ORCADO` (before submission), cascading into linked Orçamentos. Rejected — a linked Orçamento may carry real supplier data entered by a different flow (e.g. `OrcamentoService.create` called directly, not only via conversion); silently destroying it because someone deleted its parent header is a bigger blast radius than this feature needs. A user who wants that can delete the Orçamento(s) first (which already reverts the header to `INICIADO` per Decision 3), then delete the now-`INICIADO` header.

**2. Orçamento deletion is gated on `DRAFT` only, matching `removeLineItem`'s existing guard.**
`LOCKED` only happens once the originating header reaches `CONFERIDO`/`CONCLUIDO` — at that point the Orçamento is load-bearing for the approval record and (once `CONCLUIDO`) for `Material` creation, so it must not be deletable. This is the same rule already enforced by `requireDraft` for line-item add/edit/remove; deletion reuses it.

**3. Deleting a converted Orçamento reverts its source items and, if it was the last one, the header's status.**
An Orçamento created via `PurchaseRequestItemService.convertToOrcamento` carries `sourcePurchaseRequestId` and each of its `OrcamentoLineItem`s carries `sourcePurchaseRequestItemId`. Deleting it must:
   - For every `PurchaseRequestItem` whose `convertedToOrcamentoId` equals this Orçamento's id: clear `convertedToOrcamentoId`/`convertedAt` and revert `status` to `PENDING`, so the item becomes available for re-conversion instead of being permanently stuck `CONVERTED` with a dangling reference.
   - For every `PurchaseRequestItem` of that same header whose `selectedOrcamentoLineItemId` refers to one of this Orçamento's line items (which can happen even for items converted into a *different* Orçamento — selection is per-item, not per-Orçamento): clear the selection, since the target row is being deleted.
   - If, after removal, `orcamentoRepository.findBySourcePurchaseRequestId(headerId)` is empty, transition the header from `ORCADO` back to `INICIADO`. This is the exact inverse of `markOrcado()`, keeping "Orçado ⇔ has ≥1 linked Orçamento" an invariant rather than a one-way ratchet.
   This logic lives in `OrcamentoService.delete(...)`, mirroring how `lockAllForPurchaseRequest`/`unlockAllForPurchaseRequest` already live there rather than in `PurchaseRequestService`, since it's fundamentally about keeping `Orcamento`/`PurchaseRequestItem` consistent when an Orçamento's row disappears.
   - *Alternative considered*: forbid deleting an Orçamento that has any converted items at all (only allow deleting fully "orphan"/standalone Orçamentos). Rejected as too restrictive — the common mistake this feature targets ("picked the wrong supplier right after converting") is exactly the converted case, and the revert logic is a small, self-contained addition.

**4. New exceptions, following the existing per-entity exception style.**
Add `PurchaseRequestNotDeletableException` (thrown when status != `INICIADO`) and `OrcamentoNotDeletableException` (thrown when status != `DRAFT`), each mapped to HTTP 409 the same way `PurchaseRequestNotOrcadoException`/`OrcamentoNotDraftException` already are (check `GlobalExceptionHandler` — reuse its existing conflict-mapping pattern rather than inventing a new one).

**5. Frontend confirmation uses `window.confirm`, matching the existing pattern.**
`DailyReportDetailView.vue` already confirms a destructive action this way (`window.confirm(t('...'))`); no dedicated modal component exists in `pantheon-web` yet, so this change does not introduce one just for this feature.

## Risks / Trade-offs

- **[Risk] A header could sit `INICIADO` indefinitely with items nobody wants, and now also silently reappear at `INICIADO` after every Orçamento is deleted, which is a slightly unusual status transition (backwards) for users used to a forward-only stepper.** → Mitigation: the Pedido de Compra detail view's 4-step stepper already renders whatever status the header currently has; reverting to `INICIADO` just re-enables the "Criar orçamento" flow and (per this change) the delete button again, which is the intended, consistent state — call this out explicitly in the scenario text so it isn't mistaken for a bug.
- **[Risk] Deleting an Orçamento with converted items touches multiple `PurchaseRequestItem` rows plus the header row in one transaction.** → Mitigation: wrap in a single `@Transactional` method (consistent with existing multi-row operations like `submitForApproval`), no new failure mode introduced.
- **[Trade-off] Restricting deletion to `INICIADO`/`DRAFT` means a user who advanced too far (e.g. already submitted for approval) cannot delete — they must reject the approval step first to unwind back to `ORCADO`, then delete the Orçamentos, which reverts the header to `INICIADO`, then delete the header.** This is intentional (Non-Goals) but worth documenting in the UI copy/help text so it isn't reported as a bug.

## Migration Plan

No schema migration required. Roll out is a normal backend deploy (new endpoints/service methods) followed by a frontend deploy (new buttons wired to them); the two new endpoints are additive and don't change any existing response shape. No rollback concerns beyond a normal revert, since no data format changes.

## Open Questions

None — scope confirmed via the proposal's status-gating rules (`INICIADO` for headers, `DRAFT` for Orçamentos).
