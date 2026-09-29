## Context

Diário de Obra today: one PATCH (core fields) + five independent POST-add endpoints (workforce/equipment/activity/occurrence/materials-received), each saved immediately, no update/delete for any sub-resource row except whole-report delete. `DailyReportEquipmentUsage.equipmentId` is a hard, non-null FK — no "unregistered equipment" path exists. `Material` (delivery-tracking, created when a Pedido de Compra is concluded) and `PurchaseRequestInvoice` ("nota fiscal") are both fully built, working features with zero connection to Diário de Obra today. Mobile has only workforce (read-only) and activities (read-only) beyond the core fields — no equipment usage, occurrences, materials, attachments, or signatures at all. Full research backing every claim above was gathered before writing this proposal (entity fields, exact endpoint shapes, exact line numbers) — see the change's own investigation, not repeated here.

## Goals / Non-Goals

**Goals:** one global save action; workforce/equipment pickers that reuse what the app already has (team roster, equipment registry + its filter); a read-only view of materials actually delivered on this report's date, sourced from the real delivery-tracking data; drag-and-drop media upload with editable captions; automatic surfacing of same-day Pedido de Compra invoices; replace the signatures block with a properly-attributed approval history; a shared visual language (web/mobile/PDF) matching the approved mockup; mobile reaches feature parity with web for every section in scope.

**Non-Goals:** deleting or migrating `DailyReportMaterialReceived` (explicit user decision: keep it, unchanged, alongside the new PR-sourced view); deleting the `DailyReportSignature` backend capability (kept, just unused by this UI); redesigning "Ocorrências" (not requested — restyled only); a single monolithic "save everything in one HTTP call" backend endpoint (see Decision 1); building a generic drag-and-drop component for reuse elsewhere (scoped to this feature for now).

## Decisions

**1. "Global save" is a client-side orchestration pattern, not a new backend mega-endpoint.**
Combining core-fields, workforce, equipment, activities, and media into one atomic transaction would be a much larger, riskier backend change (heterogeneous validation rules, partial-failure semantics across unrelated tables) for a request that's fundamentally about UX ("don't make me click save five times"), not data consistency. The existing per-section endpoints stay. The web/mobile UI keeps pending changes in local state, shows a "não salvo"/"tudo salvo" indicator, and its one "Salvar alterações" button fires every pending call (new adds, edits, deletes) together, reporting a single combined success/error. This also means each endpoint keeps its own existing validation (e.g. core fields' all-or-nothing weather/hours rule) rather than needing a new combined-validation layer.

**2. Equipment usage's "outro equipamento" mirrors workforce's existing nullable-FK pattern exactly.**
`DailyReportEquipmentUsage.equipmentId` becomes nullable; a new `customName` column (nullable, required only when `equipmentId` is null — enforced in the service layer, same style as `WorkforceEntryRequest`'s "membershipId or roleDescription" rule) holds the free-text name. `statusNote` (already free text) keeps its existing meaning as an optional observation, usable either way.

**3. Materials-delivered-on-this-date is a new, read-only, computed view — not a new table.**
New `MaterialRepository.findByConstructionSiteIdAndDeliveredAtBetween(UUID siteId, Instant startInclusive, Instant endExclusive)`, called with `reportDate.atStartOfDay(ZoneId.systemDefault())`/`reportDate.plusDays(1).atStartOfDay(...)` (matching how the rest of the codebase treats `LocalDate` vs `Instant` boundaries — no site-specific timezone concept exists elsewhere to borrow instead). Exposed via a new `GET /api/daily-reports/{id}/delivered-materials` (or folded into the detail response — implementer's call, document whichever is chosen) returning the existing `MaterialResponse` shape (already has `sourcePurchaseRequestId`/`Name`). "Marcar conferido" from this view calls the existing `POST /api/materials/{id}/mark-checked` directly — no new write path.

**4. Same-day Pedido de Compra invoices are surfaced the same way — computed, not copied.**
New `PurchaseRequestInvoiceRepository.findByPurchaseRequestIdInAndCreatedAtBetween(List<UUID> purchaseRequestIds, Instant start, Instant end)`, fed by first collecting the site's Pedido de Compra ids (`PurchaseRequestRepository.findByConstructionSiteId`-style, already exists in some form per the approval-workflow work) and the same day-boundary math as Decision 3. A new response DTO includes the source `purchaseRequestId` (and ideally its display name, batched the same way `MaterialService.resolveSourcePurchaseRequests` already does) so the UI can link back and label it "Importado do Pedido de Compra #N".

**5. Signatures: hide, don't delete.**
`DailyReportSignature`/`DailyReportSignatureService`/its endpoints stay exactly as they are — only `pantheon-web`'s UI block is removed. Rationale: the user said "ainda não vamos trabalhar com assinaturas" (not "never") — a working, tested, small feature that costs nothing to leave in place is not worth a destructive removal for a "not right now."

**6. Approval display-name resolution mirrors `PurchaseRequestPdfService`'s existing (currently-private) `displayNameForMembership` pattern**, extracted or duplicated (consistent with this change-family's established "small, independent duplication over a risky shared refactor" precedent — see `add-daily-report-approval-workflow`'s Decision 1) into `DailyReportService`/`DailyReportPdfService`: a `SiteMembership`'s own `displayName` when set (accountless members), else its linked `AppUser`'s name/email.

**7. Delete endpoints added for workforce/equipment/activity/media/attachment** (none exist today beyond whole-report delete), all `DRAFT`-only (same `requireEditableReport` gate the add-endpoints already use), needed for the "add several, remove one before saving" UX the mockup shows. `DailyReportMaterialReceived` and `DailyReportOccurrence` are out of scope for this change (Non-Goals) so they do not get delete endpoints here.

**8. Media/attachment caption becomes editable via a new PATCH endpoint** (`PATCH /api/daily-reports/{id}/media/{mediaId}` body `{caption}`) — upload-time caption entry stays, this just allows changing it afterward, matching the mockup's per-thumbnail caption field.

## Risks / Trade-offs

- **[Risk] Mobile parity is a large amount of genuinely new code** (equipment usage, materials-from-PR, attachments, caption editing, drag-and-drop-equivalent multi-photo staging, approval names) — not a redesign of existing screens but new features on top of a smaller existing base. Mitigated by implementing backend first and verifying its contract before mobile work starts, and by reusing the exact multi-photo staging pattern already built for Materiais this session.
- **[Risk] Drag-and-drop is new, unproven UI in this codebase.** Mitigated by keeping the existing plain file-input as a fallback inside the same dropzone (click-to-browse), so drag-and-drop is additive, not a replacement path that could break uploading entirely if it has a bug.
- **[Trade-off] Two visually-similar "materials" sections on one screen** (manual entry + PR-delivered view) could read as redundant. Accepted per the user's explicit decision — the mockup should visually distinguish them (different subtitle/framing) rather than merge them.

## Visual source of truth

The user approved a pixel-level mockup before this proposal was written and was explicit afterward: the real implementation (web, mobile, PDF) must match it **identically** — same fonts (Manrope, already this app's font), same colors (the mockup was built directly from this app's own `blueprint-*`/`steel-*`/`safety-*`/`emerald-*`/`amber-*` hex values, not an approximation), same sections, same order, same card/icon language. The mockup's three `.dc.html` files are the literal reference for implementation, not inspiration to riff on:
- `/private/tmp/claude-501/-Users-jrsilvanio-Documents-dev-projetos-pantheon/6c882d9e-879e-45bb-a49c-7d7bd945ab1e/scratchpad/daily-report-redesign/project/Main.dc.html` (web)
- `.../project/DailyReportMobile.dc.html` (mobile)
- `.../project/DailyReportPDF.dc.html` (PDF)

Whoever implements each surface should read the corresponding file first and match its exact layout, spacing, colors, section order, icons, and copy — translating the static mockup markup into real Vue components / Flutter widgets / PDF HTML bound to real data, not designing a new interpretation of "modern."

## Migration Plan

1. Migration: `equipment_id` nullable + `custom_name` column on `daily_report_equipment_usage`.
2. Backend: delete endpoints (workforce/equipment/activity/media/attachment), media/attachment caption PATCH + `createdAt` exposure, equipment "outro" support + registry-filter reuse, materials-by-date + invoices-by-date read endpoints, approval display-name resolution, PDF redesign.
3. Frontend web: full page redesign per the approved mockup, global-save orchestration.
4. Mobile: redesign existing sections + build the missing ones to parity.
5. Rollback: the schema change is additive/widening only; no rollback complexity beyond the usual "revert the commit."
