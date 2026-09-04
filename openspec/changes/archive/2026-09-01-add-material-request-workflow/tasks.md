## 1. Backend: Material request core

- [x] 1.1 Create `material_request` and `material_request_item` tables via Flyway migrations
- [x] 1.2 Add `MaterialRequest`/`MaterialRequestItem` entities, repositories, service (create, get, list), DTOs
- [x] 1.3 Add controller endpoints: `POST /api/construction-sites/{siteId}/material-requests`, `GET /api/construction-sites/{siteId}/material-requests`, `GET /api/material-requests/{id}`
- [x] 1.4 Unit tests for creation and listing

## 2. Backend: Approval and rejection

- [x] 2.1 Add approve/reject service methods enforcing ADMIN/ENGINEER permission and required rejection reason
- [x] 2.2 Add controller endpoints: `POST /api/material-requests/{id}/approve`, `POST /api/material-requests/{id}/reject`
- [x] 2.3 Unit tests: approve, reject with reason, reject without reason rejected, non-authorized member blocked

## 3. Backend: Receipt verification and status derivation

- [x] 3.1 Create `receipt_verification` table via Flyway migration
- [x] 3.2 Add `ReceiptVerification` entity, repository, service method recording a verification and recomputing the parent request's status transactionally — also added a DB-level `UNIQUE` index on `material_request_item_id` (defense in depth for the "exactly one conference per item" rule from design.md, matching the service-layer check)
- [x] 3.3 Add controller endpoint: `POST /api/material-requests/{id}/receipt-verifications`
- [x] 3.4 Unit tests: partial receipt status, full receipt status, verification rejected on non-approved request — 12/12 green (`MaterialRequestServiceTest`), also covers duplicate-verification rejection

## 4. Frontend

- [x] 4.1 Add material request creation view (multi-line-item form) under a construction site, copy sourced from `pt-BR.json` — `MaterialRequestListView.vue` includes an inline creation form with add/remove item rows
- [x] 4.2 Add request list/tracking view with status filter — `MaterialRequestListView.vue`, status `<select>` bound to `statusFilter`, reloads on change
- [x] 4.3 Add request detail view with approve/reject actions — `MaterialRequestDetailView.vue`, approve/reject buttons shown only while `PENDING`, reject requires a reason
- [x] 4.4 Add receipt verification recording view on an approved request's line items, showing divergence — per-item verification form shown while status is APPROVED/PARTIALLY_RECEIVED/RECEIVED and the item has no verification yet; divergence badge shown once verified
- [x] 4.5 Add API client functions for the new endpoints — `composables/useMaterialRequests.ts` (listRequests, createRequest, getDetail, approve, reject, recordVerification); wired into `ConstructionSitesPanel.vue` (new "Pedidos de Material" link) and `router/index.ts` (list + detail routes)

## 5. Verification

- [x] 5.1 Build and test `pantheon-service` (`mvn -pl pantheon-service compile test`) and confirm it passes — full suite green, no regressions across changes 1-5
- [x] 5.2 Build `pantheon-web` (`npm run build`) and confirm it passes — clean build after removing an unused helper (`materialLabel` duplicate in list view) that tripped TS6133
- [x] 5.3 Manually exercised end-to-end against a live `pantheon-service` (real Postgres, curl-driven): created a request with 2 items → PENDING; approved → APPROVED; verified item 1 under-quantity (8 of 10) → status PARTIALLY_RECEIVED, `divergent:true`; verified item 2 exact quantity → status RECEIVED, `divergent:false`; duplicate verification on item 1 rejected with 409; reject without a reason rejected with 400; reject with a reason on a fresh request → REJECTED; approving that rejected request rejected with 409; status filter (`?status=REJECTED`) returned only the matching request
