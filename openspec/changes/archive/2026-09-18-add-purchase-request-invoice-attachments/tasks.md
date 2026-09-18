## 1. Backend — data model

- [x] 1.1 Migration `V55__create_purchase_request_invoice_table.sql`: table `purchase_request_invoice` (`id` PK, `purchase_request_id` FK to `purchase_request(id)` not null, `storage_key` varchar(500) not null, `content_type` varchar(100) not null, `original_name` varchar(255) not null, `uploaded_by` FK to `app_user(id)` not null, `created_at` timestamptz not null default now()), plus an index on `purchase_request_id`.
- [x] 1.2 Entity `PurchaseRequestInvoice` (id, purchaseRequestId, storageKey, contentType, originalName, uploadedBy, createdAt) — mirror `SiteDocumentProjectAttachment`'s shape/style.
- [x] 1.3 Repository `PurchaseRequestInvoiceRepository extends JpaRepository<PurchaseRequestInvoice, UUID>` with `findByPurchaseRequestIdOrderByCreatedAtDesc(UUID)`.
- [x] 1.4 `StorageKeys.purchaseRequestInvoiceKey(UUID constructionSiteId, UUID purchaseRequestId, UUID invoiceId, String extension)`.
- [x] 1.5 `PurchaseRequestInvoiceNotFoundException`, registered in `ConstructionExceptionHandler` mapped to HTTP 404.

## 2. Backend — service methods on PurchaseRequestService

- [x] 2.1 Add `private static final Set<String> INVOICE_ALLOWED_EXTENSIONS = Set.of("pdf", "xml", "jpg", "jpeg", "png");` and a private `extensionOf`/file-validation helper (or reuse an existing shared one if `PurchaseRequestService` already has access to one — check `SiteDocumentProjectService`'s `extensionOf`/`readBytes` for the exact shape to mirror, don't just copy-paste if a shared utility already exists).
- [x] 2.2 `uploadInvoice(UUID purchaseRequestId, UUID actingUserId, MultipartFile file) -> PurchaseRequestInvoice`: `requireManage` (no status gate), validate not empty + allowed extension (`InvalidFileException` otherwise), store via `StorageService.putObject` at `StorageKeys.purchaseRequestInvoiceKey(...)`, save and return the entity.
- [x] 2.3 `listInvoices(UUID purchaseRequestId, UUID actingUserId) -> List<PurchaseRequestInvoice>`: `requireVisible` `PURCHASE_REQUEST`, delegate to the repository.
- [x] 2.4 `getInvoiceContent(UUID invoiceId, UUID actingUserId) -> FileContent` (bytes, contentType, originalName — a small local record, mirroring `SiteDocumentProjectService.FileContent`): look up the invoice (404 via `PurchaseRequestInvoiceNotFoundException`), resolve its header for the site/visibility check (`requireVisible` `PURCHASE_REQUEST`), fetch bytes via `StorageService.getObject`.
- [x] 2.5 `deleteInvoice(UUID invoiceId, UUID actingUserId)`: look up the invoice, resolve its header for `requireManage` `PURCHASE_REQUEST`, delete the storage object then the row.
- [x] 2.6 In the existing `delete(UUID purchaseRequestId, UUID actingUserId)` method, before deleting items: fetch `invoiceRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId)`, delete each one's storage object, then `invoiceRepository.deleteAll(...)`.

## 3. Backend — controller endpoints

- [x] 3.1 `POST /api/purchase-requests/{id}/invoices` (multipart, `file` param) → 201 with a `PurchaseRequestInvoiceResponse`.
- [x] 3.2 `GET /api/purchase-requests/{id}/invoices` → list of `PurchaseRequestInvoiceResponse`.
- [x] 3.3 `GET /api/purchase-request-invoices/{id}/content` → bytes with `Content-Disposition: attachment; filename=...` and the stored content type (mirror `SiteDocumentProjectController.getFileContent`).
- [x] 3.4 `DELETE /api/purchase-request-invoices/{id}` → 204.
- [x] 3.5 `PurchaseRequestInvoiceResponse` DTO (id, purchaseRequestId, originalName, contentType, uploadedBy, createdAt).

## 4. Backend — tests

- [x] 4.1 `PurchaseRequestServiceTest` (or a focused nested/separate test if the class is getting large — match existing convention) covering: successful upload persists an entity and calls `StorageService.putObject`; disallowed extension throws `InvalidFileException`; empty file throws `InvalidFileException`; upload succeeds regardless of header status (e.g. also test on a `CONCLUIDO` header); list/delete visibility and manage-access checks; deleting a header with invoices also deletes their storage objects and rows.
- [x] 4.2 `./mvnw -pl pantheon-service test` green.

## 5. Frontend

- [x] 5.1 `usePurchaseRequests.ts`: add `uploadInvoice(purchaseRequestId, file)` (multipart POST, mirror `useSiteDocumentProjects.ts`'s `authUpload`), `listInvoices(purchaseRequestId)`, `deleteInvoice(invoiceId)`, `getInvoiceContentBlob(invoiceId)` (mirror `authFetchBlob`/`getFileContentBlob`). Add the `authUpload`/`authFetchBlob` helpers to this composable's file if not already present there (they currently only exist in `useSiteDocumentProjects.ts`).
- [x] 5.2 `PurchaseRequestDetailView.vue`: new "Notas fiscais" section (visible regardless of status) — file input triggering `uploadInvoice` + reload, a compact list (name, download link via blob+`URL.createObjectURL`, remove button using the same inline-popover confirm pattern as the header's own delete action), load invoices alongside the rest of the detail data in `load()`.
- [x] 5.3 `pt-BR.json`: add `purchaseRequests.invoices.*` keys (title, uploadButton, uploadError, empty, downloadLabel, removeButton, removeConfirm, removeError).
- [x] 5.4 `npm run build` green in `pantheon-web`.

## 6. Verification

- [x] 6.1 Run `openspec validate --strict` against the change before archiving.
