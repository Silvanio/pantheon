## Context

This codebase already has two well-established file-attachment patterns to draw from: `MaterialDeliveryPhoto` (a minimal photo-per-`Material` entity: `storageKey`/`contentType`/`uploadedBy`/`createdAt`, no original filename, served back raw) and `SiteDocumentProjectAttachment` (a fuller file entity: adds `originalName`, validated against an extension allowlist, served back with a `Content-Disposition: attachment` header for a real download). A Pedido de Compra invoice is a named document a user will want to re-download by its real filename, so it's closer to the second pattern than the first.

## Goals / Non-Goals

**Goals:**
- Let a user attach and later retrieve/remove one or more invoice files per Pedido de Compra, at any point in its lifecycle.
- Reuse existing infrastructure exactly: `StorageService` (S3/MinIO), the `InvalidFileException`/extension-allowlist validation style, and the existing `PURCHASE_REQUEST` permission capability — no new capability, no new storage abstraction.
- Keep deleting a Pedido de Compra (existing feature) consistent: no orphaned objects left in storage or rows left in the DB pointing at a deleted header.

**Non-Goals:**
- OCR, structured NF-e XML parsing, or any validation of the invoice's *content* (matching it against the purchase's value, items, or supplier). This is a plain attachment, not an accounting integration.
- A dedicated `INVOICE_MANAGE`-style permission — it rides on the existing `PURCHASE_REQUEST` capability, matching how Orçamento materials/photos ride on `ORCAMENTO_MANAGE`/`PURCHASE_REQUEST` visibility rather than inventing a new one.
- Gating upload/delete by the header's status. Unlike line-item conversion or the header/Orçamento deletion features, there's no consistency invariant an invoice attachment could violate — it's inert metadata, safe to add or remove at any time.

## Decisions

**1. New `PurchaseRequestInvoice` entity, modeled on `SiteDocumentProjectAttachment` rather than `MaterialDeliveryPhoto`.**
Fields: `id`, `purchaseRequestId`, `storageKey`, `contentType`, `originalName`, `uploadedBy`, `createdAt`. No `constructionSiteId` column — like `MaterialDeliveryPhoto`, the site is always reachable via its `PurchaseRequest` (`requirePurchaseRequest(...).getConstructionSiteId()`), so storing it again would just be denormalized, driftable state for a lookup that's one join away.
- *Alternative considered*: put invoices in the existing `site_document_project_attachment` table with `purchaseRequestId` as an additional nullable FK. Rejected — that table's whole model (folder nesting, task-card linking, `SiteDocumentProjectService`'s folder-tree permission checks) is unrelated to a Pedido de Compra's own `PURCHASE_REQUEST` capability and would tangle two unrelated capabilities' authorization together.

**2. Methods live on `PurchaseRequestService`, not a new dedicated service.**
Mirrors `MaterialService` owning `MaterialDeliveryPhoto` directly rather than splitting off a `MaterialPhotoService` — the entity has no independent lifecycle or authorization model of its own; it's a strict child of one `PurchaseRequest`. New methods: `uploadInvoice`, `listInvoices`, `getInvoiceContent`, `deleteInvoice`.

**3. No status gate on upload/delete — only the existing `requireManage`/`requireVisible` `PURCHASE_REQUEST` checks.**
Every other Pedido-de-Compra mutation this codebase has gates on status because it protects a workflow invariant (item selection only while `ORCADO`, deletion only while `INICIADO` because later statuses have dependents). An invoice attachment has no such invariant — it can legitimately arrive at any stage (a pro-forma invoice before delivery, the real one after). Gating it would just be friction with no correctness benefit.

**4. Deleting a `PurchaseRequest` also deletes its invoices (rows + storage objects).**
`PurchaseRequestService.delete` already only runs while `INICIADO` (before any invoice would typically exist, but nothing stops uploading one early), so this is a small addition to that method: fetch and delete all `PurchaseRequestInvoice`s for the header (storage object then row) before deleting the header's items and the header itself, for the same no-orphans reasoning as the item/line-item cleanup already there.

**5. Extension allowlist: `pdf`, `xml`, `jpg`, `jpeg`, `png`.**
`xml` is added on top of `SiteDocumentProjectService`'s existing `{jpg, jpeg, png, pdf, mp4}` allowlist because Brazilian fiscal invoices (NF-e) are commonly distributed as signed XML rather than a rendered PDF; `mp4` is dropped since a video is never a fiscal document. This is a local constant on `PurchaseRequestService`, not a shared one — the two allowlists differ in composition and there's no reuse to extract without coupling two unrelated capabilities' validation rules together.

## Risks / Trade-offs

- **[Risk] No malware/content scanning on uploaded files.** → Mitigation: unchanged from every other upload path in this codebase (site documents, daily report media); out of scope to newly solve here.
- **[Trade-off] No size limit specified beyond whatever the multipart request layer already enforces globally.** Matches the existing site-document/daily-report upload behavior — introducing a bespoke limit for just this one upload path would be inconsistent rather than safer.

## Migration Plan

Additive: one new table (`V55__create_purchase_request_invoice_table.sql`), two new endpoints, one new small UI section. No existing data or behavior changes. Rollback is a normal revert; the new table has no incoming foreign keys from other tables so dropping it back out is also safe if ever needed.

## Open Questions

None.
