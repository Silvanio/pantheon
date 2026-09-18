## 1. Database migration

- [x] 1.1 Write a clean-cut Flyway migration (next `V` number after the current latest) that: drops/clears existing `orcamento_approval` and `site_orcamento_approval_level` tables' data (or the tables themselves, being recreated under new names), clears existing `material`, `orcamento_line_item`, `orcamento` rows and any `purchase_request_item` rows referencing a converted Orçamento (dev-only data, no production carry-forward, per design.md Decision/Risk notes).
- [x] 1.2 Create `purchase_request_approval` table (mirrors old `orcamento_approval` shape, FK `purchase_request_id`) and `site_purchase_request_approval_level` table (mirrors old `site_orcamento_approval_level`, FK `construction_site_id`); drop the old two tables.
- [x] 1.3 Add `status` (`VARCHAR`, not null, default `'INICIADO'` at migration time then drop default), `submitted_at`, `approved_at`, `completed_at`, `current_approval_cycle` (default 0), `last_rejection_reason` columns to `purchase_request`.
- [x] 1.4 Add `selected_orcamento_line_item_id` (nullable FK to `orcamento_line_item`) to `purchase_request_item`.
- [x] 1.5 Replace `orcamento.status` values: migrate column to only allow `DRAFT`/`LOCKED` (drop `submitted_at`, `approved_at`, `completed_at`, `current_approval_cycle`, `last_rejection_reason` columns from `orcamento` after the data-clearing step in 1.1, since no rows remain).
- [x] 1.6 Verify migration applies cleanly against a fresh dev database (`./mvnw -pl pantheon-service flyway:migrate` or equivalent test bootstrap).

## 2. Backend entities & enums

- [x] 2.1 Add `PurchaseRequestStatus` enum (`INICIADO`, `ORCADO`, `CONFERIDO`, `CONCLUIDO`).
- [x] 2.2 Update `PurchaseRequest` entity: add `status`, `submittedAt`, `approvedAt`, `completedAt`, `currentApprovalCycle`, `lastRejectionReason` fields and transition methods (`markOrcado()`, `submitForApproval()`, `approve()`, `returnToOrcadoAfterRejection(reason)`, `complete()`), mirroring the transition-method style currently on `Orcamento`.
- [x] 2.3 Update `PurchaseRequestItem` entity: add nullable `selectedOrcamentoLineItemId` field and a `select(orcamentoLineItemId)`/`clearSelection()` method.
- [x] 2.4 Rename `OrcamentoApproval` → `PurchaseRequestApproval` (field `orcamentoId` → `purchaseRequestId`, everything else unchanged).
- [x] 2.5 Rename `OrcamentoApprovalStatus` → `PurchaseRequestApprovalStatus` (or keep enum name if reused as-is — confirm no naming collision with `PurchaseRequestItemStatus`).
- [x] 2.6 Rename `SiteOrcamentoApprovalLevel` → `SitePurchaseRequestApprovalLevel`.
- [x] 2.7 Simplify `OrcamentoStatus` to `{DRAFT, LOCKED}`; remove `submittedAt`/`approvedAt`/`completedAt`/`currentApprovalCycle`/`lastRejectionReason` fields and the `submitForApproval`/`approve`/`returnToDraftAfterRejection`/`complete` methods from `Orcamento`; add a `lock()`/`unlock()` method pair instead.
- [x] 2.8 Update `OrcamentoLineItem` if needed so a query can efficiently look up "the line item quoted against Pedido-de-Compra item X within Orçamento Y" (index on `(orcamentoId, sourcePurchaseRequestItemId)` if not already indexable).

## 3. Backend repositories

- [x] 3.1 Rename `OrcamentoApprovalRepository`-equivalent queries to target `PurchaseRequestApproval`/`purchaseRequestId`.
- [x] 3.2 Rename `SiteOrcamentoApprovalLevelRepository` → `SitePurchaseRequestApprovalLevelRepository`.
- [x] 3.3 Add `PurchaseRequestRepository` query methods: paginated `findByConstructionSiteId(..., Pageable)` with optional date/status filters (e.g. Specification or derived query variants), and a lookup used to detect "is this the header's first Orçamento" (or handle that in the service via a count query).
- [x] 3.4 Add `OrcamentoRepository` query methods: paginated listing with optional date/purchaseRequestId/supplier-name-contains filters; `findBySourcePurchaseRequestId(id)` (for locking/unlocking every linked Orçamento).
- [x] 3.5 Add `OrcamentoLineItemRepository` query method to fetch all line items, across a set of Orçamentos, matching a given `sourcePurchaseRequestItemId` (for the comparison table and for validating selections).

## 4. Backend services — Orçamento simplification

- [x] 4.1 Strip `OrcamentoService` of `submitForApproval`, `approveStep`, `rejectStep`, `conclude`, and all approval-authority checks (`requireStepAuthority` etc.) — keep only creation (`create`, `createFromPurchaseRequestItems`) and line-item CRUD.
- [x] 4.2 `OrcamentoService.createFromPurchaseRequestItems`: after persisting the new Orçamento, if the source `PurchaseRequest` is `INICIADO`, transition it to `ORCADO` and save it.
- [x] 4.3 Add `OrcamentoService.lockAllForPurchaseRequest(purchaseRequestId)` and `unlockAllForPurchaseRequest(purchaseRequestId)`, each loading every Orçamento with that `sourcePurchaseRequestId` and calling `lock()`/`unlock()`.
- [x] 4.4 Update line-item CRUD guards to check `orcamento.status == DRAFT` (was: `DRAFT` among four statuses — same check, simplified enum).
- [x] 4.5 Update Orçamento detail/listing assembly to include the `DRAFT`/`LOCKED` status and, per line item, whether it is the `selectedOrcamentoLineItemId` of its `sourcePurchaseRequestItemId`'s `PurchaseRequestItem` (requires a lookup back to `PurchaseRequestItemRepository`).

## 5. Backend services — Pedido de Compra selection & comparison

- [x] 5.1 Add `PurchaseRequestItemService.setSelection(itemId, orcamentoLineItemId, actingUserId)`: load the item and its header; require header status `ORCADO`; if `orcamentoLineItemId` non-null, load the target `OrcamentoLineItem` and its `Orcamento`, validate `orcamento.sourcePurchaseRequestId == header.id` and (`lineItem.sourcePurchaseRequestItemId == null || == itemId`); call `item.select(...)`/`clearSelection()`; persist.
- [x] 5.2 Add corresponding exceptions: `SelectionNotAllowedException` (wrong header status), `OrcamentoLineItemNotLinkedException` (cross-header or mismatched-item selection attempt).
- [x] 5.3 Add `PurchaseRequestService.getComparison(purchaseRequestId)`: load header items and every linked Orçamento (with suppliers + line items); build a row-per-item, column-per-Orçamento grid, populating cells from line items whose `sourcePurchaseRequestItemId` matches the row, flagging the selected cell.
- [x] 5.4 Add DTOs: `PurchaseRequestComparisonResponse` (rows: item id/name/quantity/unit; columns: orcamentoId/supplier name; cells: price/quantity/lineItemId/selected boolean).

## 6. Backend services — approval workflow (moved to Pedido de Compra)

- [x] 6.1 Rename `SiteOrcamentoApprovalLevelService` → `SitePurchaseRequestApprovalLevelService` (same `getEffectiveLevels`/config-write logic, now keyed however `SiteOrcamentoApprovalLevel` was keyed, just renamed).
- [x] 6.2 Add to `PurchaseRequestService`: `submitForApproval(purchaseRequestId, actingUserId)` — require `ORCADO`, require every item's `selectedOrcamentoLineItemId` non-null, read effective levels, create ordered `PENDING` `PurchaseRequestApproval` rows for a new cycle, notify first approver via existing `EventPublisher` pattern (rename/relocate `OrcamentoApprovalStepPendingEvent` → `PurchaseRequestApprovalStepPendingEvent`).
- [x] 6.3 Add `approveStep(purchaseRequestId, actingUserId, comment)`: same authority check as before (company staff or matching `SiteMembership.function`), advance to next step or, on final step, call `header.approve()` and `orcamentoService.lockAllForPurchaseRequest(id)`.
- [x] 6.4 Add `rejectStep(purchaseRequestId, actingUserId, reason)`: require non-blank reason, mark step `REJECTED`, `header.returnToOrcadoAfterRejection(reason)`, `orcamentoService.unlockAllForPurchaseRequest(id)`.
- [x] 6.5 Add `conclude(purchaseRequestId, actingUserId)`: require `CONFERIDO`; for each `PurchaseRequestItem`, resolve its selected `OrcamentoLineItem` and pass to `MaterialService`; call `header.complete()`.
- [x] 6.6 Rename/relocate exceptions: `OrcamentoNotDraftException`/`OrcamentoNotApprovedException`/`OrcamentoEmptyException`/`NoPendingApprovalStepException`/`NotCurrentApprovalStepException` → Pedido-de-Compra-flavored equivalents (e.g. `PurchaseRequestNotOrcadoException`, `PurchaseRequestNotConferidoException`, `PurchaseRequestSelectionIncompleteException`, reusing the two generic "no pending step"/"not current step" exceptions renamed only if they were Orçamento-specific).
- [x] 6.7 Update `MaterialService.createFromOrcamento(orcamento, items)` → `createFromPurchaseRequestSelections(purchaseRequest, items)`, sourcing name/type/quantity/unitPrice from each item's selected `OrcamentoLineItem` instead of iterating one Orçamento's line items directly.

## 7. Backend services — PDF generation

- [x] 7.1 Add `com.github.librepdf:openpdf` dependency to `pantheon-service/pom.xml` (check for version conflicts first; confirm no existing PDF lib already present).
- [x] 7.2 Add `PurchaseRequestPdfService` (or method on `PurchaseRequestService`) that, given a Pedido de Compra id and an Orçamento id, loads the Orçamento's line items that are the selected fulfillment of their Pedido-de-Compra item, and renders a PDF (header: Pedido de Compra name + date, supplier snapshot; table: item name/quantity/unit price/line total; footer: grand total) to a `byte[]`/`ByteArrayOutputStream`.
- [x] 7.3 Validate the target Orçamento is linked to the given Pedido de Compra before generating; raise the existing "not found"/"not linked" exception pattern otherwise.

## 8. Backend controllers & DTOs

- [x] 8.1 `PurchaseRequestController`: add `status`/`page`/`size` query params to the list endpoint; add `PUT /api/purchase-requests/{id}/items/{itemId}/selection`; add `GET /api/purchase-requests/{id}/comparison`; add `GET /api/purchase-requests/{id}/orcamentos/{orcamentoId}/pdf` (returns `ResponseEntity<byte[]>` with `application/pdf` content type and a `Content-Disposition` filename); add `POST /api/purchase-requests/{id}/submit`, `POST /api/purchase-requests/{id}/approve-step?comment=`, `POST /api/purchase-requests/{id}/reject-step` (body `{reason}`), `POST /api/purchase-requests/{id}/conclude`.
- [x] 8.2 `OrcamentoController`: remove `submit`/`approve-step`/`reject-step`/`conclude` endpoints.
- [x] 8.3 Rename `SiteOrcamentoApprovalLevelController` → `SitePurchaseRequestApprovalLevelController`, base path `/api/construction-sites/{siteId}/purchase-request-approval-levels`.
- [x] 8.4 Update `PurchaseRequestResponse` DTO: add `status`, `submittedAt`, `approvedAt`, `completedAt`, `lastRejectionReason`, linked Orçamento summaries (id + supplier name) for the list/detail views; update `PurchaseRequestItemResponse` to include `selectedOrcamentoLineItemId`.
- [x] 8.5 Update `OrcamentoResponse` DTO: replace old status/approval/materials-adjacent fields with `status` (`DRAFT`/`LOCKED`) and per-line-item `selected` boolean; remove approval-step history field.
- [x] 8.6 Add `PurchaseRequestApprovalResponse` DTO (cycle/step/status/approver/decidedAt/comment) mirroring the old `OrcamentoApprovalResponse` shape.
- [x] 8.7 Add request DTOs: `SetItemSelectionRequest { UUID orcamentoLineItemId }`, `RejectPurchaseRequestRequest { String reason }`.
- [x] 8.8 Add/relocate the `@ExceptionHandler` mappings in `ConstructionExceptionHandler` for every renamed/new exception from tasks 6.6/5.2.

## 9. Backend permissions

- [x] 9.1 Confirm `PermissionCapability.PURCHASE_REQUEST` gates the new submit/approve-step/reject-step/conclude/selection/approval-level-config endpoints (company staff bypass preserved per existing pattern); confirm `ORCAMENTO_MANAGE` still gates Orçamento creation/line-item CRUD only.
- [x] 9.2 Update any permission-matrix documentation/tests (e.g. `obra-permission-management` fixtures) that reference the old `ORCAMENTO_MANAGE`-gated approval actions.

## 10. Backend tests

- [x] 10.1 Update/rename `OrcamentoServiceTest` scenarios that covered submit/approve/reject/conclude to live in a new `PurchaseRequestServiceTest` (or extend an existing one), covering: submit blocked without full selection, submit blocked when not `ORCADO`, approve advances/finalizes + locks Orçamentos, reject returns to `ORCADO` + unlocks, conclude creates one `Material` per item from its selection, authority checks (matching function / company staff / 403 otherwise).
- [x] 10.2 Add `PurchaseRequestItemServiceTest` (or extend existing) coverage for `setSelection`: happy path, cross-header rejection, mismatched-item rejection, blocked outside `ORCADO`, clearing a selection.
- [x] 10.3 Add tests for the comparison endpoint: two suppliers quoting the same item at different prices, an unquoted cell left empty, selected flag correctness.
- [x] 10.4 Add a test for the PDF endpoint: generated bytes are non-empty / valid PDF header bytes; only selected items appear (assert via extracted text or line-item count if a text-extraction utility is convenient, otherwise assert on the byte-generation call boundaries).
- [x] 10.5 Update existing `OrcamentoServiceTest`/`OrcamentoControllerTest`-equivalents to drop assertions about the removed approval flow and cover the new lock/unlock-follows-header behavior instead.
- [x] 10.6 Run the full backend test suite (`./mvnw -pl pantheon-service test`) and fix any fallout in unrelated tests referencing renamed types.

## 11. Frontend composables

- [x] 11.1 `usePurchaseRequests.ts`: add `status`/`page`/`size` params to `listPurchaseRequests`; add `setItemSelection`, `getComparison`, `getPurchasePdfUrl`/`downloadPdf`, `submitForApproval`, `approveStep`, `rejectStep`, `conclude`; add `PurchaseRequestStatus` type and `PurchaseRequest`/`PurchaseRequestItem` type updates (status, selection, linked Orçamentos, approval fields).
- [x] 11.2 `useOrcamentos.ts`: remove `submitForApproval`/`approveStep`/`rejectStep`/`conclude`/approval-level methods and types; update `OrcamentoStatus` to `'DRAFT' | 'LOCKED'`; add `supplier`/`page`/`size` params to `listOrcamentos`; add `selected` field to line item type.
- [x] 11.3 Add `usePurchaseRequestApprovalLevels.ts` (renamed from the Orçamento-approval-level parts of `useOrcamentos.ts`), pointed at `/purchase-request-approval-levels`.

## 12. Frontend — Pedido de Compra screen redesign

- [x] 12.1 Redesign `PurchaseRequestPanel.vue` header: a "Filtrar" button opening a filter popover (status select + date), visually separated from a "Novo pedido" button that opens the creation form.
- [x] 12.2 Redesign the list into a paginated card grid: each card shows name, status badge (4-state color coding), item count, and linked-Orçamento count/link chips; add pagination controls wired to the composable's page/size params.
- [x] 12.3 Redesign `PurchaseRequestDetailView.vue`: add a 4-step status stepper (Iniciado/Orçado/Conferido/Concluído); show items with their current selection (supplier + price) inline; list every linked Orçamento as a navigable chip/link; keep the existing pending/converted items + "Criar orçamento" flow for `INICIADO`/`ORCADO` headers.
- [x] 12.4 Add the comparison table section (rendered once ≥1 Orçamento is linked): rows = items, columns = suppliers, selectable cells (radio-per-row) calling `setItemSelection`; a "Imprimir PDF" action per supplier column at the table's end, calling the PDF endpoint and triggering a browser download.
- [x] 12.5 Add the approval section (moved from `OrcamentoDetailView.vue`): current step, full history grouped by cycle, approve/reject controls when the viewer's function matches (reject requires a reason), "Enviar para aprovação" action when `ORCADO` and every item selected, "Concluir" action when `CONFERIDO`.
- [x] 12.6 Move the post-conclusion materials section (delivery/checked actions, photo upload) from `OrcamentoDetailView.vue` into `PurchaseRequestDetailView.vue`, unchanged in behavior.
- [x] 12.7 Update the site's "Permissões"/settings tab: rename `SiteOrcamentoApprovalLevelsPanel.vue` → `SitePurchaseRequestApprovalLevelsPanel.vue`, pointed at the renamed composable/endpoint.

## 13. Frontend — Orçamento screen redesign

- [x] 13.1 Redesign `OrcamentoListPanel.vue` header the same way (separate "Filtrar" — date/supplier/originating-Pedido — from "Novo orçamento"); paginated card grid with supplier name, status badge (`DRAFT`/`LOCKED`), line-item count.
- [x] 13.2 Redesign `OrcamentoDetailView.vue`: remove the approval-history and submit/approve/reject/conclude UI entirely; keep supplier data block, link to originating Pedido de Compra, line items table with totals, add a visual "Selecionado" marker per line item (from the `selected` field) and a "Bloqueado" indicator when `LOCKED`; remove the materials section (moved to Pedido de Compra detail).

## 14. Frontend — shared components & i18n

- [x] 14.1 Extract a reusable `PurchaseRequestComparisonTable.vue` component (used by task 12.4) if the table logic is non-trivial enough to warrant isolation from the detail view.
- [x] 14.2 Extract a reusable status-badge component (or a shared mapping) covering both the 4-state Pedido de Compra status and the 2-state Orçamento status, for consistent coloring across cards and detail views.
- [x] 14.3 Update `pt-BR.json`: add/rename keys for the new status labels (`iniciado`/`orcado`/`conferido`/`concluido`), the filter panel, pagination controls, comparison table headers/actions, the "Imprimir PDF" action, and the relocated approval-section copy (reusing existing `orcamento.*` approval strings' wording where sensible, moved under `purchaseRequests.*`); remove now-orphaned `orcamento.*` approval keys.

## 15. Verification

- [x] 15.1 Backend: `./mvnw -pl pantheon-service test` green (190/190, including a post-implementation fix to `MaterialService`'s permission check — see note below).
- [x] 15.2 Frontend: `npm run build` green in `pantheon-web` (vue-tsc + vite build, no errors).
- [ ] 15.3 Manual smoke pass via the `run` workflow: NOT completed by the agent — the app requires an authenticated login, and entering login credentials/creating an account through browser automation is outside what this agent will do on its own. The dev/prod backend and frontend are already running locally (confirmed healthy, schema at migration V54) for the user to do this pass themselves; see the change's follow-up notes for the exact scenario to click through.
- [x] 15.4 Run `openspec validate --strict` against the change before archiving.

**Post-implementation fix (found during verification, not in the original task list):** `MaterialService.list`/`getPhotoContent` still gated visibility on `PermissionCapability.ORCAMENTO_MANAGE` only, but the materials section was moved into the Pedido de Compra detail view (task 12.6), which is gated by `PURCHASE_REQUEST`. A site member with `PURCHASE_REQUEST` access but no `ORCAMENTO_MANAGE` access would have hit a 403 loading that section. Fixed by making both methods visible to a member with either capability (`requireVisibleToOrcamentoOrPurchaseRequest` in `MaterialService`). Full backend suite re-verified green after the fix (190/190).
