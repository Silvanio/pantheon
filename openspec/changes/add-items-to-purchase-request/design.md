## Context

`PurchaseRequest` items are only ever written by `PurchaseRequestService.create`, at header-creation time, and `create` currently requires at least one item. The header's status lifecycle is `INICIADO` (no Orçamento yet) → `ORCADO` (at least one Orçamento created from some of its items) → `CONFERIDO` → `CONCLUIDO`. Once a header leaves `INICIADO`, its item set is meant to be stable relative to the Orçamentos already quoted against it — adding items after quoting has started would silently invalidate in-flight comparisons. So "add items" only makes sense, and is only allowed, while `INICIADO`.

The user explicitly redirected the original plan: "Novo pedido" SHALL NOT show any item-entry modal at all. It creates an empty Pedido de Compra immediately and takes the user straight to its detail page. From there, whenever no Orçamento has been created yet for that header (i.e. it's `INICIADO`), an "Adicionar produtos" button is available to add items. This collapses what used to be two separate item-entry surfaces (the create-modal, and the new add-items-later modal) into one: the item-rows form (name/type/quantity/unit, add-row/remove-row) moves out of `PurchaseRequestPanel.vue` entirely and lives only inside `PurchaseRequestDetailView.vue`'s "Adicionar produtos" modal, which is used both to populate a freshly created empty header and to add more items to one later.

## Goals / Non-Goals

**Goals:**
- Clicking "Novo pedido" creates an empty `PurchaseRequest` (zero items) and navigates directly to its detail page — no intermediate modal.
- The detail page's "Adicionar produtos" action is the single, reused entry point for adding items, available any time the header is `INICIADO` — whether it currently has zero items (just created) or already has some.

**Non-Goals:**
- Adding items after a header has moved past `INICIADO` (once any Orçamento exists for it). This is intentionally blocked, not just hidden in the UI — the backend rejects it too.
- Editing or removing existing items (out of scope; a separate concern from adding).
- Requiring at least one item before an Orçamento can be created from a header — out of scope for this change; a header with zero items simply has nothing to convert yet, which the existing conversion UI already handles (nothing selectable).

## Decisions

- **`create` no longer requires items.** `PurchaseRequestCreationRequest`'s `@NotEmpty` on `items` is dropped (keeping `@Valid` so any items that *are* present are still validated); `PurchaseRequestService.create` already just iterates the list, so an empty list naturally results in a header with no items. `PurchaseRequestPanel.vue`'s "Novo pedido" button calls create with `items: []` directly (no form, no modal) and immediately routes to `/purchase-requests/{id}`.
- **The item-rows form has exactly one home now: the detail page's "Adicionar produtos" modal.** `PurchaseRequestPanel.vue` loses its `showForm`/`rows`/`addRow`/`removeRow`/`onSubmitForm` state and its modal template block entirely — that logic moves to `PurchaseRequestDetailView.vue`, reusing the same field markup.
- **New endpoint reuses the existing item DTOs.** `POST /api/purchase-requests/{id}/items` takes the same `PurchaseRequestCreationRequest` body shape (`{ items: [...] }`, each a `PurchaseRequestItemCreationRequest`), and returns the newly created items as `List<PurchaseRequestItemResponse>` with HTTP 201 — no new DTO types needed.
- **Status gate lives in the service, not just the UI.** `addItems` throws a new `PurchaseRequestNotIniciadoException` (HTTP 409, following the existing `PurchaseRequestNotOrcadoException`/`PurchaseRequestNotConferidoException` pattern) if `purchaseRequest.getStatus() != INICIADO`. The frontend also hides the button once the status moves on, but the backend is the actual guarantee.
- **"Adicionar produtos" shows whenever `INICIADO`, regardless of current item count** — including immediately after an empty create, which is exactly the case the user called out ("lá dentro do produto se não tiver orçamento criado deve aparecer o botão"). No special-casing for "zero items" vs "some items already"; it's the same gate (`status === 'INICIADO'` + manage access) either way.
- **Reuses `canManage`-style gating already computed in `PurchaseRequestDetailView.vue`.** No new permission-resolution logic on the frontend — mirrors how `canDelete`/`canConclude` are already derived from `detail.value?.purchaseRequest.status` plus the resolved capability.

## Risks / Trade-offs

- [Risk] A member could open the "Adicionar produtos" modal, then someone else concurrently creates the first Orçamento (moving the header to `ORCADO`) before they submit → the add attempt fails with 409 after they've filled the form. [Mitigation] Show the 409's error message in the modal (same inline error-row pattern every other form in this codebase uses) rather than losing their input; this is an acceptable, rare race — no locking needed.
- [Risk] A header can now exist with zero items indefinitely (created, never populated). [Mitigation] Not a data-integrity issue — the conversion-to-Orçamento flow already requires selecting at least one item, so an empty header simply can't progress past `INICIADO` until items are added; it just sits in the list as "Iniciado" with an item count of 0, same as any other in-progress header.
