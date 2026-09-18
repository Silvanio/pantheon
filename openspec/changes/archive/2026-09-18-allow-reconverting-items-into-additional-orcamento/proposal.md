## Why

A Pedido de Compra's comparison table is explicitly designed to show two suppliers quoting the *same* item at different prices (`purchase-requests`' "Pedido de Compra comparison table" requirement), but that state can never actually be reached today: once a `PurchaseRequestItem` is converted into an `Orcamento`, it becomes `CONVERTED` and `pantheon-service` rejects any attempt to convert it again ("Already-converted items cannot be converted again"). In practice, the moment a user converts all of a header's items into a first supplier's Orçamento, they are permanently unable to add a second Orçamento to that header at all — there is nothing left to select. The one real use case for multiple Orçamentos on one header (get competing quotes for the same materials) is exactly the one the current rule blocks.

## What Changes

- `pantheon-service` SHALL allow selecting a `PurchaseRequestItem` for conversion regardless of whether it is already `CONVERTED` — an item may now be copied into more than one `Orcamento`'s line items, so its materials can be quoted by multiple suppliers and compared. **BREAKING** (behavioral): removes the "Already-converted items cannot be converted again" rejection.
- A `PurchaseRequestItem`'s `status`/`convertedToOrcamentoId` continue to exist as a "has this been quoted at all, and by which Orçamento first" convenience marker (used for the Pendente/Convertido UI grouping and the "view Orçamento" link) — they stop being a re-selection gate. The authoritative multi-quote data is, as already, each `OrcamentoLineItem.sourcePurchaseRequestItemId`.
- Deleting an Orçamento (from the deletion feature shipped just before this change) must not incorrectly revert an item to `PENDING` when that item is still quoted in another of the header's remaining Orçamentos — only revert it when no other Orçamento still references it.
- `pantheon-web`: the Pedido de Compra detail view's "Convertidos em orçamento" list gets a checkbox like the "Pendentes" list, feeding the same selection used by "Criar orçamento", so already-converted items can be included in a new conversion alongside or instead of pending ones.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `purchase-requests`: replaces the "Converting purchase-request items into an Orçamento" requirement's already-converted rejection with support for converting the same item into more than one Orçamento; updates "Purchase-request views" to reflect that converted items are also selectable.
- `orcamento-management`: refines the "Orçamento deletion" requirement (added in the immediately preceding change) so reverting a converted item to `PENDING` only happens when no other of the header's Orçamentos still references it.

## Impact

- `pantheon-service`: `PurchaseRequestItemService.convertToOrcamento`, `PurchaseRequestItem` (new non-destructive re-point-vs-revert distinction), `OrcamentoService.delete`, `OrcamentoLineItemRepository` (a query to check for other references), removal of the now-unused `PurchaseRequestItemAlreadyConvertedException` rejection path.
- `pantheon-web`: `PurchaseRequestDetailView.vue` (checkbox on converted items too).
