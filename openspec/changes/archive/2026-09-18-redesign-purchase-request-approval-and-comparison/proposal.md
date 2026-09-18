## Why

Approval currently lives on the Orçamento, but a single Pedido de Compra can spawn several Orçamentos (one per supplier), and the real business decision — which supplier to buy each product from, possibly mixing suppliers to get the best price — happens across those Orçamentos, not inside any single one of them. Today's screens also force that decision to happen implicitly and invisibly: there is no side-by-side view of what each supplier quoted for the same product, no way to record a per-product supplier choice, and no way to approve or print the resulting mixed purchase. The list/detail screens for both Pedido de Compra and Orçamento are minimal grids with no pagination or filtering ergonomics, which no longer fits a workflow with a real approval chain and cross-supplier comparison.

## What Changes

- **BREAKING**: Move the entire approval workflow from `Orcamento` to `PurchaseRequest` (Pedido de Compra). `Orcamento` no longer has `submit`/`approve-step`/`reject-step`/`conclude` actions, an approval chain, or an `IN_APPROVAL`/`APPROVED`/`COMPLETED` status.
- **BREAKING**: Replace `OrcamentoStatus{DRAFT,IN_APPROVAL,APPROVED,COMPLETED}` with `OrcamentoStatus{DRAFT,LOCKED}`. Line items remain editable only in `DRAFT`; an Orçamento is automatically `LOCKED` while its originating Pedido de Compra is `CONFERIDO` or `CONCLUIDO`, and unlocked back to `DRAFT` if that approval is rejected.
- **BREAKING**: Give `PurchaseRequest` its own lifecycle status, `PurchaseRequestStatus{INICIADO,ORCADO,CONFERIDO,CONCLUIDO}`, replacing today's implicit, status-less header.
- Add a per-product, per-item supplier selection on `PurchaseRequestItem` (`selectedOrcamentoLineItemId`), letting a single Pedido de Compra be fulfilled by a mix of Orçamentos/suppliers — one product from one Orçamento, another product from a different one — to reach the best combined price.
- Add a comparison endpoint/view: one table per Pedido de Compra with a row per requested product and a column per linked Orçamento (supplier), showing each supplier's quoted price for that product and which cell is currently selected, so choosing the cheapest combination is a single glance.
- Add a per-supplier PDF export of the selected items for a Pedido de Compra, generated from that comparison table.
- Move the per-site approval-level configuration from Orçamento to Pedido de Compra (new capability), keeping the same authority rule (matching `SiteMembership` function, or company staff) and the same single-`ENGINEER`-level default.
- Add pagination and richer filtering (status, supplier/name) to both the Pedido de Compra and Orçamento list endpoints.
- Redesign both `pantheon-web` screens: a header that separates the "Filtrar" action from the "Novo pedido"/creation action, a paginated card list with a compact per-item summary, and a detail view that surfaces status, linked Orçamentos, the comparison table, the approval timeline, and post-conclusion materials without requiring the user to hunt for them.

## Capabilities

### New Capabilities
- `purchase-request-approval-workflow`: the multi-step approval chain (config, submission, approve/reject, conclusion into delivery-tracking materials) now scoped to the Pedido de Compra instead of the Orçamento.
- `orcamento-management`: the renamed, simplified home for Orçamento — per-supplier creation, line-item CRUD, and a `DRAFT`/`LOCKED` status driven by its originating Pedido de Compra's approval state. Replaces `orcamento-approval-workflow`, which is fully retired by this change (all of its requirements move here or to `purchase-request-approval-workflow`).

### Modified Capabilities
- `purchase-requests`: header gains a real status lifecycle (`INICIADO`/`ORCADO`/`CONFERIDO`/`CONCLUIDO`), items gain a per-item supplier selection, new comparison and per-supplier-PDF endpoints, pagination/status filtering on listing, and a redesigned UI.
- `orcamento-approval-workflow`: every requirement is removed (see Migration notes on each) — superseded by the new `orcamento-management` and `purchase-request-approval-workflow` capabilities. This capability is retired; `openspec/specs/orcamento-approval-workflow/` should be treated as gone once this change is archived.

## Impact

- **Backend**: `PurchaseRequest`/`PurchaseRequestItem` (new status, selection field), `Orcamento` (simplified status, approval fields removed), new `PurchaseRequestApproval`/`SitePurchaseRequestApprovalLevel` entities replacing `OrcamentoApproval`/`SiteOrcamentoApprovalLevel`, `PurchaseRequestController`/`OrcamentoController` endpoint changes, new `SitePurchaseRequestApprovalLevelController`, `MaterialService` conclusion source change, a new PDF-generation dependency, DB migration(s) (clean-cut, pre-production data).
- **Frontend**: `usePurchaseRequests.ts`, `useOrcamentos.ts`, a renamed approval-levels composable, `PurchaseRequestPanel.vue`/`PurchaseRequestDetailView.vue`, `OrcamentoListPanel.vue`/`OrcamentoDetailView.vue`, a new comparison-table component, `pt-BR.json`.
- No production data migration needed (pre-launch environment); existing `Orcamento`/approval rows in dev can be reset as prior changes in this domain have done.
