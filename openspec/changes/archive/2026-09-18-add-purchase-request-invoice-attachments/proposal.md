## Why

A Pedido de Compra has no way today to record the supplier's fiscal invoice ("nota fiscal") once materials are delivered — there is no attachment/document concept on it at all, only the header, its items, its linked Orçamentos, and (post-conclusion) delivery-tracking `Material`s. Sites need to keep the invoice(s) alongside the purchase for accounting/audit purposes.

## What Changes

- `pantheon-service` SHALL allow a construction site member with `PURCHASE_REQUEST` manage access to upload one or more invoice files (`PurchaseRequestInvoice`) to a `PurchaseRequest`, at any point in its lifecycle (no status gate — an invoice can arrive before, during, or after approval/conclusion), and to delete one they no longer want attached.
- Accepted file types: PDF, XML (NF-e), JPG/JPEG, PNG — mirroring the existing site-document upload validation style (`SiteDocumentProjectService`'s `ALLOWED_EXTENSIONS`), stored in the same S3-compatible object storage as every other upload in this codebase (never as a DB blob).
- Any member with visibility into `PURCHASE_REQUEST` can list and download a header's invoices.
- Deleting a `PurchaseRequest` (the deletion feature shipped two changes ago, only reachable while `INICIADO`) also deletes any invoices already attached to it, including their stored objects.
- `pantheon-web`: a "Notas fiscais" section on the Pedido de Compra detail view — upload button, a compact list of attached files (name, uploader-agnostic timestamp, download link, remove action using the existing inline-popover confirm pattern), visible regardless of the header's status.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `purchase-requests`: adds invoice attachment upload/list/download/delete to the Pedido de Compra, and extends the deletion requirement to also clean up any attached invoices.

## Impact

- `pantheon-service`: new `PurchaseRequestInvoice` entity/table (migration `V55`), `PurchaseRequestInvoiceRepository`, upload/list/download/delete methods (added to `PurchaseRequestService`, alongside its other Pedido-de-Compra operations), new `StorageKeys.purchaseRequestInvoiceKey`, new `PurchaseRequestInvoiceNotFoundException`, controller endpoints on `PurchaseRequestController`, reuses the existing `InvalidFileException` for validation.
- `pantheon-web`: `usePurchaseRequests.ts` (upload/list/delete/download-blob functions), `PurchaseRequestDetailView.vue` (new section), `pt-BR.json`.
