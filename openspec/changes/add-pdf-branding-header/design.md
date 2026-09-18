## Context

Two independent services (`DailyReportPdfService`, `PurchaseRequestPdfService`) each build their own HTML string and render it via `openhtmltopdf`, with separate, near-identical inline `<style>` blocks. Neither has access to `Company` data today (only `ConstructionSite`, which itself has `companyId`). The branding header (logo + company name + site name) is identical content needed in all three documents, so it belongs in one place both services can call, rather than being copy-pasted a third time.

## Goals / Non-Goals

**Goals:**
- One shared component resolves and renders the branding header, used identically by both PDF services.
- The Pedido de Compra PDFs gain a metadata block answering "who opened this, who approved it, when did it open/close" using data that already exists (`PurchaseRequest.createdBy`/`createdAt`/`completedAt`, `PurchaseRequestApproval`), not new fields to collect.
- Consistent, nicer base styling (margins, table shading, header divider) across all three documents via one shared CSS fragment.

**Non-Goals:**
- Adding a logo-upload feature — `Company.logoObjectKey` and its upload endpoint already exist; this change only *reads* it for embedding.
- Changing what data `DailyReport` PDFs show beyond the new header — the report's own existing "Assinaturas" section already covers its who-did-what.
- Tracking which specific company-staff user decided a step — `PurchaseRequestApproval.decidedBySiteMembershipId` is `null` for a company-staff decision today (see `approveStep`'s `access.companyStaff() ? null : ...`), and that data genuinely isn't recorded elsewhere; the header shows "Equipe da empresa" for that case rather than guessing.

## Decisions

**1. New `PdfBrandingService`, not a static utility.**
It needs `ConstructionSiteRepository`, `CompanyRepository`, and `StorageService` (to fetch the logo bytes) — real dependencies, so a Spring-managed `@Service` fits the codebase's existing style better than a static helper class none of the other PDF/HTML-building code in this project uses. It exposes `resolve(UUID constructionSiteId) -> Branding` (a small record: company display name, site name, an already-base64-encoded `data:` URI for the logo or `null`) and `renderHeaderHtml(Branding) -> String`. Callers own their overall HTML document structure and just splice in this fragment plus its CSS.

**2. Company display name prefers `tradeName`, falling back to `name`.**
`Company.tradeName` is the operating/commercial name set during onboarding (`completeProfile`); `Company.name` is the bare name given at signup, before that profile may even be completed. A `Company` with no completed profile (no `tradeName`, no `logoObjectKey`) simply renders with its bare `name` and no logo image — never blocks PDF generation.

**3. Logo content-type is inferred from the stored object key's file extension**, via a small `jpg/jpeg → image/jpeg, png → image/png, gif → image/gif` map (default `image/jpeg`), the same information `StorageKeys.companyLogoKey(companyId, extension)` already encodes in the key itself. This avoids adding a `logo_content_type` column purely to re-derive what the key's suffix already tells us.

**4. Approver metadata lists every *decided* step of the Pedido de Compra's current (highest) approval cycle, not just the ones that led to final approval.**
A rejected step is still a real decision worth showing ("Rejeitado por X"), and cycles before the current one are superseded history — showing only the latest cycle's decisions keeps the block short and matches what the approval-timeline UI already treats as "the" current cycle (see `PurchaseRequestDetailView.vue`'s `currentCycle`/`approvalsByCycle`).

**5. This block only appears on the two Pedido de Compra PDFs, not the Daily Report PDF.**
A daily report has no "who approved," "opening/closing date" concept of its own — it has activities, workforce, and its own sign-off section already. Forcing the same metadata block onto it would just be blank/irrelevant fields.

## Risks / Trade-offs

- **[Risk] A company with a very large logo image bloats the PDF (base64 inflates size ~33%) and slows rendering.** → Mitigation: same trade-off already accepted for `DailyReportPdfService`'s existing photo embedding (also raw base64, no resizing) — consistent with, not worse than, existing behavior. Not solved here; a future size/dimension cap on logo upload would be a separate change.
- **[Trade-off] "Equipe da empresa" is a fixed, non-personalized label for company-staff approvals** — accepted since the underlying data isn't tracked (Non-Goals).

## Migration Plan

No schema change — every field used already exists (`Company.tradeName`/`logoObjectKey`, `PurchaseRequest.createdBy`/`createdAt`/`completedAt`, `PurchaseRequestApproval`). Purely additive to PDF content; no endpoint, request, or response-shape changes. Rollback is a normal revert.

## Open Questions

None.
