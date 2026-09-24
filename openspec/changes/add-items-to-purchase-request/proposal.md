## Why

Once a Pedido de Compra is created, there is currently no way to add more items to it — `PurchaseRequestService.create` is the only code path that writes `PurchaseRequestItem` rows, and it only runs once, at header creation. If a member forgets an item, or new needs come up while the request is still `INICIADO` (not yet quoted), the only workaround is creating a whole separate Pedido de Compra. The detail screen has no "add item" action at all.

## What Changes

- `pantheon-service` gains a new endpoint to add one or more items to an existing Pedido de Compra, allowed only while that header is still in `INICIADO` status (before any Orçamento has been created for it).
- `pantheon-service`'s Pedido de Compra creation endpoint no longer requires at least one item — a header can now be created empty.
- `pantheon-web`'s "Novo pedido" action no longer opens an item-rows modal: clicking it immediately creates an empty Pedido de Compra and navigates straight to its detail page. The item-rows form (name/type/quantity/unit, add-row/remove-row) moves entirely to the detail page's new "Adicionar produtos" action, which is the only place items are entered from now on — used both to fill a freshly created empty header and to add more items later.
- `pantheon-web`'s Pedido de Compra detail screen gains that "Adicionar produtos" action, visible only while the header is `INICIADO` (i.e. before any Orçamento has been created for it, regardless of current item count) and only to a member who can manage `PURCHASE_REQUEST`, opening a centered modal with the item-rows form.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `purchase-requests`: adds a requirement allowing items to be added to an existing `PurchaseRequest` header while it is `INICIADO`, and extends the purchase-request views requirement to cover the new "Adicionar produtos" action on the detail screen.

## Impact

- Backend: `PurchaseRequestService` (new `addItems` method; `create` now accepts zero items), `PurchaseRequestCreationRequest` (drop the "at least one item" validation), `PurchaseRequestController` (new `POST /api/purchase-requests/{id}/items` endpoint), a new `PurchaseRequestNotIniciadoException` mapped in `ConstructionExceptionHandler`, new/updated tests in `PurchaseRequestServiceTest`.
- Frontend: `PurchaseRequestPanel.vue` (removes its item-rows create-modal entirely; "Novo pedido" becomes a direct create-and-navigate action), `PurchaseRequestDetailView.vue` (new modal + "Adicionar produtos" action button, reusing the item-rows form moved from `PurchaseRequestPanel.vue`), the shared composable (`usePurchaseRequests.ts`: new `addPurchaseRequestItems` function; `createPurchaseRequest`-style call now sends an empty items array), new `pt-BR.json` strings.
- No schema/migration changes — reuses the existing `PurchaseRequestItem` entity and `PurchaseRequestItemCreationRequest`/`PurchaseRequestItemResponse` DTOs.
