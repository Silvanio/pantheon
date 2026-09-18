## REMOVED Requirements

### Requirement: Orçamento creation
**Reason**: This capability is retired. Orçamento creation moves to the new `orcamento-management` capability, unchanged in behavior.
**Migration**: See `orcamento-management`'s "Orçamento creation" requirement.

### Requirement: Orçamento line item management
**Reason**: This capability is retired. Line-item CRUD moves to the new `orcamento-management` capability; the gating status changes from `DRAFT`-only-among-four-statuses to `DRAFT`-only-among-two-statuses (`DRAFT`/`LOCKED`).
**Migration**: See `orcamento-management`'s "Orçamento line item management" requirement.

### Requirement: Per-site Orçamento approval levels
**Reason**: Approval is no longer performed on the Orçamento — a single Pedido de Compra can span several Orçamentos and is approved as one document. Approval-level configuration moves to the Pedido de Compra.
**Migration**: See `purchase-request-approval-workflow`'s "Per-site Pedido de Compra approval levels" requirement. Any site-level configuration previously stored against `SiteOrcamentoApprovalLevel` must be recreated against the new `SitePurchaseRequestApprovalLevel` configuration (dev-only data, no production carry-forward needed).

### Requirement: Submitting an Orçamento for approval
**Reason**: Submission for approval is now an action on the Pedido de Compra, covering every Orçamento linked to it at once.
**Migration**: See `purchase-request-approval-workflow`'s "Submitting a Pedido de Compra for approval" requirement.

### Requirement: Acting on an approval step
**Reason**: Approval steps are now recorded against the Pedido de Compra, not the Orçamento.
**Migration**: See `purchase-request-approval-workflow`'s "Acting on an approval step" requirement.

### Requirement: Concluding an approved Orçamento
**Reason**: Conclusion (and the resulting `Material` creation) is now a Pedido de Compra action, since a concluded purchase can draw selected items from multiple Orçamentos.
**Migration**: See `purchase-request-approval-workflow`'s "Concluding an approved Pedido de Compra" requirement.

### Requirement: Orçamento listing and detail
**Reason**: This capability is retired. Listing/detail moves to `orcamento-management`, dropping the approval-step history (now on the Pedido de Compra) and adding the `DRAFT`/`LOCKED` status and pagination/supplier filtering.
**Migration**: See `orcamento-management`'s "Orçamento listing and detail" requirement.

### Requirement: Orçamento workflow views
**Reason**: This capability is retired. The Orçamento UI is redesigned and simplified in `orcamento-management` (no more submit/approve/conclude affordances); the approval UI moves to the Pedido de Compra detail view, covered by `purchase-request-approval-workflow`.
**Migration**: See `orcamento-management`'s "Orçamento workflow views" requirement and `purchase-request-approval-workflow`'s UI requirement.
