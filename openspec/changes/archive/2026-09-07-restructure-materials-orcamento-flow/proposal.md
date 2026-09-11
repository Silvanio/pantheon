## Why

Today's "Materiais" tab conflates two unrelated things (an equipment registry and a manually-cadastro'd material catalog), and the purchase flow built on top of it (`MaterialRequest` → single-step client-approved `Orcamento` → per-item `ReceiptVerification`) forces every material to first exist as a catalog entry and only supports one approval step. In practice, obras need to jot down whatever materials a quote covers as free text (materials vary per purchase and a rigid catalog just adds friction), need more than one person to sign off on a budget before it's binding, and need a lightweight way to track "things we still need to buy" before a formal quote exists. This change replaces that rigid pipeline with a simpler one: a free-form purchase list, a quote (Orçamento) with its own free-text line items and a configurable multi-step approval chain, and delivery tracking that only starts once a quote is actually concluded.

## What Changes

- **BREAKING**: Remove the material catalog (`Material` registration/listing) and the `MaterialRequest` → single-step-approval → `Orcamento` → per-item `ReceiptVerification` pipeline entirely. Nothing migrates forward automatically; obras start the new flow empty (see design.md for the data-migration decision).
- Equipment keeps its own registry, status lifecycle, and its own obra tab — no longer bundled with "materials" in the UI or in the permission capability.
- New **Pedido de Compra** (purchase request) list per obra: free-text items (name, type, quantity) with no approval step of its own — just a running list of things the obra needs to buy.
- Selecting one or more Pedido de Compra items and choosing "Criar orçamento" creates a new Orçamento pre-filled with those items copied in as free-text line items (name, type, price left blank for the creator to fill in); the source items are marked as converted so they stop showing as pending.
- Orçamento line items are always free text (nome + tipo, plus unit price) entered when building the quote — never selected from a catalog, whether the quote was started from a Pedido de Compra or from scratch.
- Orçamento gains a configurable, sequential **multi-level approval workflow** (one or more approval steps, each tied to a construction-site function) replacing today's single client-approval gate. Rejecting at any step returns the Orçamento to Rascunho with the rejection reason instead of dead-ending it.
- Orçamento status becomes: **Rascunho → Em aprovação → Aprovado → Concluído**. "Concluído" is a separate, explicit action taken after approval (not automatic) — this is the point where delivery tracking begins.
- New **Material** delivery-tracking records: created automatically, one per Orçamento line item, the moment an Orçamento is marked Concluído. Status: **Aguardando entrega → Entregue → Entregue e conferido**, with photo attachments when marking as conferido — replacing the old `ReceiptVerification` mechanism.
- Diário de Obra's "material received" entries stop referencing the (removed) material catalog and become free-text (name + unit) like the rest of this redesign.
- Permission capabilities are restructured: `EQUIPMENT_MATERIAL`/`MATERIAL_REQUEST`/`MATERIAL_APPROVAL` are retired in favor of `EQUIPMENT`, `PURCHASE_REQUEST`, and `ORCAMENTO_MANAGE`; approval-step authority is driven by the new per-site approval-level configuration rather than a single `MANAGE`/`VIEW` capability.

## Capabilities

### New Capabilities
- `purchase-requests`: Free-text "Pedido de Compra" list per obra (name/type/quantity, no approval) and the action to convert selected items into a new Orçamento.
- `orcamento-approval-workflow`: Orçamento with free-text line items, per-site configurable sequential approval levels, the Rascunho/Em aprovação/Aprovado/Concluído lifecycle, and the "Concluir" action that hands off to delivery tracking.
- `material-delivery-tracking`: Post-Orçamento `Material` records (one per concluded line item) with delivery status and photo-backed conference.

### Modified Capabilities
- `equipment-material-registry`: Remove all material-catalog requirements (registration/listing of the cadastro'd `Material`); equipment registration/status/listing requirements remain, now standing alone as this capability's sole scope.
- `material-request-workflow`: Retired in full — every requirement (request creation/approval, the old single-step Orçamento sub-flow, `ReceiptVerification`) is superseded by `purchase-requests`, `orcamento-approval-workflow`, and `material-delivery-tracking`.
- `daily-construction-report`: The "material received" entry on a daily report stops referencing the removed material catalog and becomes free-text (name + unit).
- `obra-permission-management`: `PermissionCapability` enum and its per-function default matrix are updated to drop `EQUIPMENT_MATERIAL`/`MATERIAL_REQUEST`/`MATERIAL_APPROVAL` and add `EQUIPMENT`/`PURCHASE_REQUEST`/`ORCAMENTO_MANAGE`.

## Impact

- **pantheon-service**: new entities/tables (`PurchaseRequest`, `PurchaseRequestItem`, rewritten `Orcamento`/`OrcamentoLineItem`, `SiteOrcamentoApprovalLevel`, `OrcamentoApproval`, new `Material` delivery entity + `MaterialDeliveryPhoto`); Flyway migrations dropping the old `material`, `material_request`, `material_request_item`, `receipt_verification`, `receipt_verification_photo` tables and the old `orcamento`/`orcamento_line_item` shape; `DailyReportMaterialReceived` loses its `material_id` FK in favor of free-text columns; `PermissionCapability` enum change; new/updated controllers, services, DTOs, exceptions.
- **pantheon-message**: the existing `orcamento-sent`-style notification is replaced by notifications tied to the new approval-step and conclusion events (whoever is next in the approval chain gets notified; the obra is notified when materials are ready for delivery tracking).
- **pantheon-web**: obra detail tabs change — a standalone "Equipamentos" tab, a new "Pedido de Compra" tab, and an "Orçamentos" tab replace the current "Materiais" tab and the separate "Pedidos de Material" page; `MaterialRequestListView`/`MaterialRequestDetailView`/`MaterialsPanel` are replaced by new views/panels for purchase requests, orçamento detail (with approval actions), and material delivery tracking.
- No automatic data migration for existing catalog materials, material requests, or in-flight orçamentos — this is a clean cutover (see design.md).
