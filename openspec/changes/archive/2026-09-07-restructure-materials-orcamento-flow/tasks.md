## 1. Data model & migrations (pantheon-service)

- [x] 1.1 Flyway migration: drop `receipt_verification_photo`, `receipt_verification`, `orcamento_attachment`, `orcamento_line_item`, `orcamento`, `material_request_item`, `material_request`, `material`; delete `site_permission_override` rows whose `capability` is `EQUIPMENT_MATERIAL`, `MATERIAL_REQUEST`, or `MATERIAL_APPROVAL`
- [x] 1.2 Flyway migration: create `purchase_request_item` (id, construction_site_id FK, name, type nullable, quantity numeric(19,3), unit nullable, status, created_by FK, created_at, converted_to_orcamento_id nullable FK, converted_at nullable)
- [x] 1.3 Flyway migration: recreate `orcamento` (id, construction_site_id FK, status, created_by FK, created_at, submitted_at nullable, approved_at nullable, completed_at nullable, current_approval_cycle int default 0, last_rejection_reason nullable) and `orcamento_line_item` (id, orcamento_id FK, name, type nullable, quantity numeric(19,3), unit_price numeric(19,2) nullable, source_purchase_request_item_id nullable FK)
- [x] 1.4 Flyway migration: create `site_orcamento_approval_level` (id, construction_site_id FK, step_order int, approver_function varchar, active boolean, created_at, updated_at; unique on construction_site_id+step_order where active)
- [x] 1.5 Flyway migration: create `orcamento_approval` (id, orcamento_id FK, cycle_number int, step_order int, approver_function varchar, status, decided_by_site_membership_id nullable FK, decided_at nullable, comment nullable, created_at)
- [x] 1.6 Flyway migration: create `material` (id, construction_site_id FK, orcamento_line_item_id FK, name, type nullable, quantity numeric(19,3), status, delivered_at nullable, delivered_by nullable FK, checked_at nullable, checked_by nullable FK, created_at) and `material_delivery_photo` (id, material_id FK, storage_key, content_type, uploaded_by FK, created_at)
- [x] 1.7 Flyway migration: alter `daily_report_material_received` — drop `material_id` FK column, add `material_name` varchar not null and `unit` varchar nullable
- [x] 1.8 JPA entities/enums: `PurchaseRequestItem`, `PurchaseRequestItemStatus` (PENDING, CONVERTED); rewritten `Orcamento`, `OrcamentoStatus` (DRAFT, IN_APPROVAL, APPROVED, COMPLETED), `OrcamentoLineItem`; `SiteOrcamentoApprovalLevel`; `OrcamentoApproval`, `OrcamentoApprovalStatus` (PENDING, APPROVED, REJECTED); rewritten `Material`, `MaterialDeliveryStatus` (AWAITING_DELIVERY, DELIVERED, DELIVERED_AND_CHECKED), `MaterialDeliveryPhoto`; update `DailyReportMaterialReceived` (drop `materialId`, add `materialName`/`unit`); repositories for all of the above
- [x] 1.9 Delete obsolete entities/repositories/DTOs: old `Material` catalog registration DTOs, `MaterialRequest`, `MaterialRequestItem`, `ReceiptVerification`, `ReceiptVerificationPhoto`, `OrcamentoAttachment`, `AttachmentKind`, and their repositories

## 2. Permission capability changes

- [x] 2.1 `PermissionCapability` enum: remove `EQUIPMENT_MATERIAL`, `MATERIAL_REQUEST`, `MATERIAL_APPROVAL`; add `EQUIPMENT`, `PURCHASE_REQUEST`, `ORCAMENTO_MANAGE`
- [x] 2.2 `SitePermissionService` default matrix updated per design.md Decision 6 (company staff MANAGE all; ENGINEER/ARCHITECT MANAGE on PURCHASE_REQUEST/ORCAMENTO_MANAGE + VIEW on EQUIPMENT; SITE_FOREMAN MANAGE on EQUIPMENT + VIEW on PURCHASE_REQUEST/ORCAMENTO_MANAGE; CLIENT/SERVICE_PROVIDER VIEW only)
- [x] 2.3 `SitePermissionsPanel`/permission-config UI updated to the new capability list (no UI change needed for approval-level config here — that's its own panel, see 8.x)

## 3. Purchase request domain logic

- [x] 3.1 `PurchaseRequestItemService`: create (requires `PURCHASE_REQUEST` manage), list (any member, optional status filter)
- [x] 3.2 `PurchaseRequestItemService.convertToOrcamento(siteId, actingUserId, itemIds)` (requires `PURCHASE_REQUEST` manage): validates all items are `PENDING` and on the given site, creates a new `Orcamento` (DRAFT) with line items copied from the selected items (name/type/quantity, `sourcePurchaseRequestItemId` set), marks the items `CONVERTED` with `convertedToOrcamentoId`/`convertedAt`, rejects if any selected item is already `CONVERTED`
- [x] 3.3 Exceptions: `PurchaseRequestItemNotFoundException`, `PurchaseRequestItemAlreadyConvertedException`
- [x] 3.4 `PurchaseRequestController`: `POST/GET /api/construction-sites/{siteId}/purchase-request-items`, `POST /api/construction-sites/{siteId}/purchase-request-items/convert-to-orcamento`

## 4. Orçamento domain logic

- [x] 4.1 `SiteOrcamentoApprovalLevelService`: get/set the ordered list of approval levels for a site (requires company-staff `MANAGE` on site permissions, reusing the existing permission-admin check); resolve effective levels (configured list, or the single-`ENGINEER` default when none configured)
- [x] 4.2 `OrcamentoService.create(siteId, actingUserId, lineItems, sourcePurchaseRequestItemIds?)` (requires `ORCAMENTO_MANAGE`): persists DRAFT Orcamento + line items
- [x] 4.3 `OrcamentoService` line item management: add/edit/remove a line item, only while `DRAFT` (requires `ORCAMENTO_MANAGE`)
- [x] 4.4 `OrcamentoService.submitForApproval(orcamentoId, actingUserId)` (requires `ORCAMENTO_MANAGE`, requires ≥1 line item): resolves effective approval levels, increments `currentApprovalCycle`, creates ordered `OrcamentoApproval` rows (PENDING), transitions Orcamento to `IN_APPROVAL`
- [x] 4.5 `OrcamentoService.approveStep(orcamentoId, actingUserId)` / `rejectStep(orcamentoId, actingUserId, reason)`: resolves the current cycle's lowest-order PENDING step, requires caller is company staff or an active `SiteMembership` whose function matches that step; approve marks step APPROVED and either activates next step or (if last) transitions Orcamento to `APPROVED`; reject requires a reason, marks step REJECTED, sets `lastRejectionReason`, transitions Orcamento back to `DRAFT`
- [x] 4.6 `OrcamentoService.conclude(orcamentoId, actingUserId)` (requires `ORCAMENTO_MANAGE`, requires status `APPROVED`): transitions to `COMPLETED`, calls `MaterialService.createFromOrcamento(orcamento)` (see 5.1)
- [x] 4.7 `OrcamentoService`: list by site, get detail (line items + all `OrcamentoApproval` rows across cycles, ordered)
- [x] 4.8 Exceptions: `OrcamentoNotFoundException`, `OrcamentoNotDraftException`, `OrcamentoNotInApprovalException`, `OrcamentoNotApprovedException`, `OrcamentoEmptyException`, `NotCurrentApprovalStepException`
- [x] 4.9 `OrcamentoController`: `POST/GET /api/construction-sites/{siteId}/orcamentos`, `GET /api/orcamentos/{id}`, `POST/PUT/DELETE /api/orcamentos/{id}/line-items(/{lineItemId})`, `POST /api/orcamentos/{id}/submit`, `POST /api/orcamentos/{id}/approve-step`, `POST /api/orcamentos/{id}/reject-step`, `POST /api/orcamentos/{id}/conclude`
- [x] 4.10 `SiteOrcamentoApprovalLevelController`: `GET/PUT /api/construction-sites/{siteId}/orcamento-approval-levels`

## 5. Material delivery-tracking domain logic

- [x] 5.1 `MaterialService.createFromOrcamento(orcamento)`: creates one `Material` (`AWAITING_DELIVERY`) per `OrcamentoLineItem`, copying name/type/quantity — called only from `OrcamentoService.conclude`
- [x] 5.2 `MaterialService.markDelivered(materialId, actingUserId)` (requires `ORCAMENTO_MANAGE`, requires current status `AWAITING_DELIVERY`) and `markCheckedWithPhotos(materialId, actingUserId, photos)` (requires current status `DELIVERED`)
- [x] 5.3 `MaterialDeliveryPhotoService`/storage wiring: upload photo(s) to object storage on check, reusing the existing `StorageService`/`StorageKeys` pattern
- [x] 5.4 `MaterialService.listBySite(siteId)` / `listByOrcamento(orcamentoId)`
- [x] 5.5 Exceptions: `MaterialNotFoundException`, `MaterialDeliveryStatusOrderException`
- [x] 5.6 `MaterialController`: `GET /api/construction-sites/{siteId}/materials?orcamentoId=`, `POST /api/materials/{id}/mark-delivered`, `POST /api/materials/{id}/mark-checked` (multipart, photos), `GET /api/material-photos/{photoId}/content`

## 6. Daily report material-received decoupling

- [x] 6.1 `DailyReportService.addMaterialReceived`: accept free-text `materialName`/`unit` instead of `materialId`; update `MaterialReceivedRequest`/`MaterialReceivedResponse` DTOs and `DailyReportController` accordingly

## 7. Cleanup of retired code

- [x] 7.1 Remove `MaterialController`, `MaterialService`(+Impl) for the old catalog, `MaterialRequestController`, `MaterialRequestService`(+Impl), old `OrcamentoService`(+Impl) methods tied to `MaterialRequest`/`ReceiptVerification`, and their DTOs/exceptions not already covered above
- [x] 7.2 Remove now-unused i18n bundle keys / exception handler entries referencing removed exceptions

## 8. pantheon-message

- [x] 8.1 Replace the `orcamento-sent` event/handler with an `orcamento-approval-step-pending` event, published whenever a new step becomes PENDING (on submit, and on advancing to the next step), notifying every active `SiteMembership` on the site whose function matches that step's function
- [x] 8.2 Update/rename the corresponding email handler and templates

## 9. pantheon-web

- [x] 9.1 `SiteDetailView.vue` tabs: replace `materials` tab with three tabs — `equipment`, `purchaseRequests`, `orcamentos` — each rendering its own panel/view
- [x] 9.2 New composable `useEquipment.ts` (equipment-only, split out of `useEquipmentMaterials.ts`); delete the material-catalog half
- [x] 9.3 New composable `usePurchaseRequests.ts`: list/create purchase-request items, convert selection to Orçamento
- [x] 9.4 New composable `useOrcamentos.ts`: full Orçamento lifecycle (create, line-item CRUD, submit, approve-step, reject-step, conclude, list, detail) and approval-level config; new composable `useMaterialDeliveries.ts` for the post-conclusion material list (mark delivered/checked, photo upload)
- [x] 9.5 `EquipmentPanel.vue` relocated as the `Equipamentos` tab's content (unchanged registration/status UI)
- [x] 9.6 New `PurchaseRequestPanel.vue`: add-item form, list with checkboxes for pending items, "Criar orçamento" action, converted-items section
- [x] 9.7 New `OrcamentoListPanel.vue` (Orçamentos tab: list + "Novo orçamento") and `OrcamentoDetailView.vue` (route `/orcamentos/:id`): line-item editor while Rascunho, submit button, approval-step timeline with approve/reject controls shown only to the current step's authorized viewer, conclude button once Aprovado, and (once Concluído) the generated materials list with delivery-status controls and photo upload
- [x] 9.8 New `SiteOrcamentoApprovalLevelsPanel.vue` (small config panel, e.g. on the Permissões tab): ordered list of function-per-step, add/remove/reorder
- [x] 9.9 Update `DailyReportDetailView.vue`'s materials-received section to free-text name/unit instead of a catalog `<select>`
- [x] 9.10 Remove `MaterialsPanel.vue`, `MaterialRequestListView.vue`, `MaterialRequestDetailView.vue`, `useMaterialRequests.ts`, and their router entries (`/construction-sites/:siteId/material-requests`, `/material-requests/:id`); add new routes for `/orcamentos/:id`
- [x] 9.11 i18n: add `purchaseRequests.*`, rewrite `orcamento.*` (statuses Rascunho/Em aprovação/Aprovado/Concluído, approval-step copy), add `materialDelivery.*` (Aguardando entrega/Entregue/Entregue e conferido); remove `materials.*` and `materialRequests.*`

## 10. Tests & verification

- [x] 10.1 `pantheon-service` unit/slice tests: purchase-request item creation/listing/conversion (including the already-converted-item rejection)
- [x] 10.2 `pantheon-service` tests: Orçamento creation, line-item management gated to DRAFT, submit-for-approval level snapshotting (configured and default cases)
- [x] 10.3 `pantheon-service` tests: approval-step actions — intermediate approve, final approve, reject-returns-to-draft, non-matching-function blocked, company-staff-can-always-act
- [x] 10.4 `pantheon-service` tests: conclude requires APPROVED, creates one Material per line item; Material status progression (AWAITING_DELIVERY→DELIVERED→DELIVERED_AND_CHECKED, cannot skip)
- [x] 10.5 `pantheon-service` tests: updated `SitePermissionService` default-matrix tests for the new capability set
- [x] 10.6 `pantheon-web`: `vue-tsc -b` and `vite build` both pass clean
- [x] 10.7 Manual QA performed live in a real browser: create a purchase-request item, convert it into an Orçamento, submit for approval, approve (default single-level chain), conclude, mark a generated material delivered then checked — confirmed via live dev-database rows (2 completed Orçamentos, approval steps APPROVED, materials AWAITING_DELIVERY/DELIVERED_AND_CHECKED)
- [x] 10.8 Run `/opsx:sync-specs` (or archive) once implemented and verified, updating `openspec/specs/` accordingly
