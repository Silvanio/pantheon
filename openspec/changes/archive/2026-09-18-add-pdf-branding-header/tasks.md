## 1. Backend — shared branding service

- [x] 1.1 New `PdfBrandingService` (`pantheon-service/src/main/java/com/pantheon/service/service/`): constructor deps `ConstructionSiteRepository`, `CompanyRepository`, `StorageService`. Nested `record Branding(String companyName, String siteName, String logoDataUri)` (`logoDataUri` is a ready `data:<mime>;base64,<bytes>` string, or `null` when the company has no `logoObjectKey`).
- [x] 1.2 `resolve(UUID constructionSiteId) -> Branding`: load the `ConstructionSite` (`ConstructionSiteNotFoundException` if missing), load its `Company` (`CompanyNotFoundException` if missing — should never actually happen but mirrors existing lookup style), company display name = `tradeName` if non-blank else `name`; if `logoObjectKey != null`, fetch bytes via `storageService.getObject(...)`, base64-encode, and infer content type from the key's file extension (`jpg`/`jpeg` → `image/jpeg`, `png` → `image/png`, `gif` → `image/gif`, default `image/jpeg`).
- [x] 1.3 `renderHeaderHtml(Branding) -> String`: an HTML fragment — a flex/table header row with the logo `<img>` (omitted when `logoDataUri` is null) beside the company name and site name, followed by a divider. Also expose the shared header/table CSS as a `public static final String STYLE` constant on this class (or a small colocated constant) for both PDF services to append into their own `<style>` blocks — page margins via `@page`, header layout, a `thead`/`tr:nth-child(even)` table shading rule, refreshed color palette.

## 2. Backend — Pedido de Compra PDFs

- [x] 2.1 `PurchaseRequestPdfService`: add constructor dependencies `AppUserRepository`, `SiteMembershipRepository`, `PurchaseRequestApprovalRepository`, `PdfBrandingService`.
- [x] 2.2 Add a private `buildRequestMetaHtml(PurchaseRequest)` method: resolve the creator's display name via `AppUserRepository.findById(purchaseRequest.getCreatedBy())` (display name or email, "—" if somehow missing); resolve the current cycle's decided approval steps via `purchaseRequestApprovalRepository.findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(id)`, filtered to `purchaseRequest.getCurrentApprovalCycle()` and `status != PENDING`, each resolved to a name (via `SiteMembershipRepository.findById(decidedBySiteMembershipId)` → `getDisplayName()`, or "Equipe da empresa" when `decidedBySiteMembershipId == null`) and its `approverFunction`/`status`/`decidedAt`; render "Aberto por X em DD/MM/YYYY", one line per decided step ("Função — Nome — Aprovado/Rejeitado em DD/MM/YYYY HH:mm"), and "Finalizado em DD/MM/YYYY" or "Em andamento" based on `completedAt`.
- [x] 2.3 In both `buildHtml` (per-supplier) and `buildSummaryHtml`, call `pdfBrandingService.resolve(purchaseRequest.getConstructionSiteId())`/`renderHeaderHtml(...)` and splice the result right after `<body>`, before the existing `<h1>`; splice `buildRequestMetaHtml(purchaseRequest)` right after the `<h1>`, before the existing supplier/meta lines; append `PdfBrandingService.STYLE` into each method's `<style>` block alongside (not replacing) their existing rules, removing now-duplicated rules (e.g. redundant `body`/`table` base rules) in favor of the shared ones.

## 3. Backend — Daily Report PDF

- [x] 3.1 `DailyReportPdfService`: add constructor dependency `PdfBrandingService`.
- [x] 3.2 In `buildHtml`, call `pdfBrandingService.resolve(site.getId())`/`renderHeaderHtml(...)` and splice the result right after `<body>`, before the existing `<h1>`; append `PdfBrandingService.STYLE` into its `<style>` block, removing now-duplicated base rules.

## 4. Backend — tests

- [x] 4.1 `PdfBrandingServiceTest`: `resolve` returns the company's `tradeName` when set, falls back to `name` otherwise; returns a non-null `logoDataUri` with the right inferred mime type when `logoObjectKey` is set (per extension: jpg/png/gif), and `null` when it isn't; `renderHeaderHtml` output contains the company/site name text and, when present, an `<img>` tag with the logo data URI.
- [x] 4.2 `PurchaseRequestPdfServiceTest`: new cases (PDFBox text extraction, matching the existing payment-method tests' style) — the branded header's company/site name appear in the rendered text; the requester's name and opening date appear; a decided approval step's approver name/function appear; a `CONCLUIDO` header shows its finalization date and a non-concluded one shows the in-progress indicator instead; same checks repeated for `generateSummary`.
- [x] 4.3 `DailyReportPdfServiceTest` (create if it doesn't already exist — check first): the branded header's company/site name appear in the rendered PDF text.
- [x] 4.4 `./mvnw -pl pantheon-service test` green.

## 5. Verification

- [x] 5.1 Run `openspec validate --strict` against the change before archiving.
