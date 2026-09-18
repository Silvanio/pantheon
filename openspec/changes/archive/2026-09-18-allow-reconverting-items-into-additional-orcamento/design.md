## Context

`PurchaseRequestItem.status`/`convertedToOrcamentoId` were designed under an implicit one-item-to-one-Orçamento assumption: `convertToOrcamento` rejects an already-`CONVERTED` item, and the just-shipped Orçamento-deletion feature reverts an item straight to `PENDING` whenever the Orçamento holding its `convertedToOrcamentoId` is deleted. But the actual multi-supplier comparison feature (`purchase-requests`' comparison table, `PurchaseRequestItemService.setSelection`'s "mixed selection across two suppliers") already treats `OrcamentoLineItem.sourcePurchaseRequestItemId` as a many-to-one-per-Orçamento, many-Orçamentos-per-item relationship — it's only the single-valued `PurchaseRequestItem` fields that don't allow it. This change relaxes those fields to be a "first/primary conversion" convenience marker rather than a re-selection gate.

## Goals / Non-Goals

**Goals:**
- Let a user request a second (third, ...) Orçamento's quote for materials that are already part of an earlier Orçamento, so the comparison table can actually show competing prices for the same item — the scenario it was built for.
- Keep `PurchaseRequestItem.status`/`convertedToOrcamentoId` meaningful for the existing Pendente/Convertido UI split and the "jump to its Orçamento" link, without them gating anything.
- Fix the interaction with the just-added Orçamento-deletion revert logic so it doesn't regress an item to `PENDING` while it's still legitimately quoted elsewhere.

**Non-Goals:**
- Tracking the full list of every Orçamento an item has been quoted into on the `PurchaseRequestItem` entity itself (that's what querying `OrcamentoLineItem` by `sourcePurchaseRequestItemId` is for — no new denormalized field).
- Changing `setSelection`, the comparison table, or the approval/conclude flow — none of them gate on `PurchaseRequestItem.status`, only on `selectedOrcamentoLineItemId`, so they already work correctly once conversion is unblocked.
- Preventing conversion once a header is `CONFERIDO`/`CONCLUIDO`. That gap predates this change (the original single-shot conversion also had no such guard) and is out of scope here.

## Decisions

**1. Drop the `PENDING`-only gate in `convertToOrcamento`; keep the cross-header guard.**
The only remaining validation on selected items is that they all belong to the same `purchaseRequestId` (`ItemsSpanMultiplePurchaseRequestsException`). `PurchaseRequestItemAlreadyConvertedException` and its `ConstructionExceptionHandler` mapping are removed — nothing in the codebase throws it anymore ceases to exist as dead code per the no-cruft convention, rather than being kept "just in case."

**2. `PurchaseRequestItem` gets a `recordConversion`-once semantic instead of always overwriting.**
In the conversion loop, only call `item.convertTo(orcamentoId, now)` when the item is still `PENDING` (its first conversion). An already-`CONVERTED` item stays pointed at whichever Orçamento it was first converted into — that's still a valid, dereferenceable Orçamento, so the existing "view its Orçamento" UI link keeps working, and nothing about the new Orçamento's own line item needs the `PurchaseRequestItem` to know about it (the `OrcamentoLineItem` row is the record of that link).
- *Alternative considered*: track a list/many-to-many join. Rejected as unnecessary — `OrcamentoLineItem.sourcePurchaseRequestItemId` already is that join table in spirit; adding a second one would be duplicated, driftable state for zero new capability.

**3. Orçamento-deletion revert becomes conditional on no other reference remaining.**
In `OrcamentoService.delete`, when a `PurchaseRequestItem`'s `convertedToOrcamentoId` equals the Orçamento being deleted: before reverting it to `PENDING`, check whether any of the header's *other* Orçamentos still has an `OrcamentoLineItem` with `sourcePurchaseRequestItemId` equal to that item (via `OrcamentoLineItemRepository.findByOrcamentoIdInAndSourcePurchaseRequestItemId`, called with the header's other Orçamento ids — already computed via `orcamentoRepository.findBySourcePurchaseRequestId` before the delete). If such a reference exists, re-point `convertedToOrcamentoId` to one of those Orçamentos (add `PurchaseRequestItem.repointConversion(UUID)`, which only touches `convertedToOrcamentoId`, leaving `convertedAt`/`status` alone) instead of reverting. Only revert to `PENDING` (`revertConversion()`, unchanged) when no other reference exists.
- *Edge case accepted*: if the deleted Orçamento happens to be the one on record and another still-live Orçamento also quotes the item, re-pointing to "some other" Orçamento is an arbitrary but harmless choice — it's a display convenience, not authoritative data.

**4. Frontend: converted items become selectable too, not moved back to a "pending" list.**
`PurchaseRequestDetailView.vue` keeps its existing Pendente/Convertido section split (still useful signal — "has at least one quote yet or not") but adds the same checkbox/`toggleSelection` wiring to the Convertido list's items as the Pendente list already has. "Criar orçamento" (already gated on `selectedIds.size > 0`) works unchanged from a mixed selection of pending and converted item ids.

## Risks / Trade-offs

- **[Risk] A user could now accidentally include an already-quoted item in a second Orçamento by mistake, inflating the comparison table with an unwanted extra column.** → Mitigation: this is the intended, requested capability (comparing multiple suppliers for the same item), and it's easily undone — deleting a mistaken `DRAFT` Orçamento (the just-shipped deletion feature) cleanly removes it and, per Decision 3, correctly leaves the item's other quotes intact.
- **[Trade-off] `convertedToOrcamentoId` is no longer a complete picture of "every Orçamento this item is in"** — it was never meant to be (see Non-Goals); call sites that need the full picture already query `OrcamentoLineItem`, not this field.

## Migration Plan

No schema change. Backend deploy is additive/relaxing (removes a rejection, adds one conditional branch in an existing transactional method); frontend deploy adds checkboxes to an already-rendered list. No rollback concerns beyond a normal revert.

## Open Questions

None.
