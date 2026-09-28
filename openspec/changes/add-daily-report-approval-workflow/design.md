## Context

Pedido de Compra already has a complete, working, well-tested approval architecture: `SitePurchaseRequestApprovalLevel` (per-site configurable step chain), `PurchaseRequestApproval` (per-header step instances with cycle numbers), and `AccessLevel.VIEW_AND_APPROVE` (function-matched approvers, documented today as "meaningful only for `PURCHASE_REQUEST`"). `PermissionCapability.DAILY_REPORT` exists but today only meaningfully uses `VIEW`/`MANAGE`/`HIDDEN`. `DailyReportStatus` has two values (`DRAFT`, `SUBMITTED`); `DailyReportService.requireReport` gates single-report access on `permissionService.requireVisible(...)` (throws if `HIDDEN`) with no per-status filtering, and the accepted spec (`daily-construction-report`'s "Daily report listing and detail") explicitly guarantees every member sees every report "regardless of whether their daily-report access is `VIEW` or `MANAGE`" — a guarantee this change deliberately breaks for `VIEW`.

## Goals / Non-Goals

**Goals:**
- A per-site configurable, multi-step approval chain for Diário de Obra, submitted the same way Pedido de Compra's is.
- `VIEW_AND_APPROVE` as a real, selectable `DAILY_REPORT` access level, reusing Pedido de Compra's function-matching authorization mechanism exactly.
- A `VIEW`-only visibility rule specific to Daily Report: only `APPROVED` reports are visible — different from Pedido de Compra's `VIEW_AND_APPROVE` visibility rule, and explicitly not "corrected" to match it.
- Existing `SUBMITTED` reports keep working after migrating to `APPROVED` (sign-off, PDF export, etc. all still apply to the renamed terminal status).

**Non-Goals:**
- Generalizing Pedido de Compra's approval-level/approval-step entities into a shared, workflow-agnostic mechanism. Two small, independent, mirrored entities are an acceptable amount of duplication for a second workflow (see Decision 1) — this change does not touch Pedido de Compra's existing tables, entities, or tests.
- A mobile approval-levels configuration screen — Pedido de Compra doesn't have one either (confirmed: no approval-level UI exists anywhere under `pantheon-mobile/lib/`), so Daily Report's config screen is web-only too, consistent with that existing scoping.
- Changing who can create/edit a Diário de Obra draft, or the existing media/attachment/PDF-export mechanics — only the status field's valid values, the submission target, and view-only visibility change.

## Decisions

**1. Duplicate the approval-level/approval-step entities rather than generalize them.**
`SitePurchaseRequestApprovalLevel` is structurally generic already (`{constructionSiteId, stepOrder, approverFunction, active, timestamps}`, no PR-specific fields) — it could be turned into a shared `SiteApprovalLevel` with a workflow discriminator. Rejected for this change: doing so would require renaming/migrating Pedido de Compra's existing, shipped, tested table and code purely to avoid creating one new small entity, which is a much larger blast radius than the duplication it avoids. New, parallel entities instead: `SiteDailyReportApprovalLevel` (mirrors `SitePurchaseRequestApprovalLevel` field-for-field) and `DailyReportApproval` (mirrors `PurchaseRequestApproval` field-for-field: `dailyReportId`, `cycleNumber`, `stepOrder`, `approverFunction`, `status`, `decidedBySiteMembershipId`, `decidedAt`, `comment`). If a third workflow ever needs this shape, that's the point to revisit generalizing — not before.

**2. Reject transitions the report back to `DRAFT`, not to a distinct "rejected" status.**
The user specified this explicitly. `DailyReportStatus` stays a 3-value enum (`DRAFT`, `PENDING_APPROVAL`, `APPROVED`) — a rejection is recorded on the `DailyReportApproval` row itself (`status = REJECTED`, with a required comment/reason, mirroring Pedido de Compra's reject-with-reason), while the report header goes back to editable `DRAFT`. Resubmitting starts a new `cycleNumber`, exactly like Pedido de Compra; old cycles' steps are kept for history (visible as the report's approval history in the UI).

**3. `VIEW`'s new status-gated visibility is enforced in two places, mirroring how Pedido de Compra's `VIEW_AND_APPROVE` visibility already works mechanically (different rule, same mechanism):**
   - **List**: `DailyReportService.list(...)` post-filters the site's reports to `status == APPROVED` only when the caller's resolved `DAILY_REPORT` access is exactly `VIEW` (not `MANAGE`, not `VIEW_AND_APPROVE`).
   - **Single-report access**: `requireReport(...)` — after the existing `requireVisible` (`HIDDEN` check) — adds: if resolved access is `VIEW` and `report.getStatus() != APPROVED`, throw `DailyReportNotFoundException` (the same "behaves as if it doesn't exist" pattern Pedido de Compra's `requireViewAndApproveVisibility` uses — a 404, not a 403, so a `VIEW` member can't even infer a draft report exists).
   - `MANAGE` and `VIEW_AND_APPROVE` are exempt from this filter entirely — both need to see non-`APPROVED` reports (to write them, or to act on pending approval).

**4. Acting on a Daily Report approval step reuses Pedido de Compra's exact authorization mechanism**, not a simplified version: the acting user's `SiteMembership` function must match the step's function, OR the user is company staff with no `SiteMembership` of their own on that site (the same "bypass" Pedido de Compra grants); a user who does have a site role is never exempted by their company-staff status, even if it doesn't match. Resolved `DAILY_REPORT` access must be `MANAGE` or `VIEW_AND_APPROVE`. This is copied faithfully from `PurchaseRequestService`'s existing step-authorization check — same nuance, same edge cases, same test shape — since the user asked for "as mesmas permissões do pedido de compra" for the *acting* side; only the passive *viewing* side (Decision 3) differs.

**5. Submission gate**: unlike Pedido de Compra (gated on "every item has a selected Orçamento line item"), Diário de Obra has no equivalent per-line gate — submission for approval simply requires the report to be `DRAFT` and have its required core fields filled (weather, work hours — already enforced by `DailyReportCoreFieldsRequiredException` today). No new gate beyond what already exists for `submit()`.

**6. Data migration**: existing `SUBMITTED` rows become `APPROVED` (a report that was "submitted" under the old 2-status model was already effectively finalized — there is no equivalent history of who approved it, so no retroactive `DailyReportApproval` rows are created for pre-existing data, only the status value changes).

## Risks / Trade-offs

- **[Risk] Breaking the existing "any member sees every report" spec guarantee could surprise an existing `VIEW`-level integration or user workflow.** → Mitigation: this is the user's explicit, confirmed requirement; the `daily-construction-report` spec is updated accordingly (Modified Requirements) so the new behavior is the documented, accepted one going forward.
- **[Risk] Sign-off (`DailyReportSignature`) is currently gated on `status == SUBMITTED`.** → Mitigation: simple rename to `status == APPROVED` in that one check — sign-off's own behavior/requirements are otherwise unchanged (`daily-report-media-and-signoff` gets a Modified Requirement reflecting only the renamed status value).
- **[Trade-off] Two near-identical approval-level/approval-step entity pairs now exist in the codebase (Pedido de Compra's and Daily Report's).** Accepted per Decision 1 — revisit only if a third workflow needs the same shape.

## Migration Plan

1. New Flyway migration(s) (check current highest `V*.sql` first): alter `daily_report.status`'s check constraint from `('DRAFT','SUBMITTED')` to `('DRAFT','PENDING_APPROVAL','APPROVED')`; `UPDATE daily_report SET status = 'APPROVED' WHERE status = 'SUBMITTED'` before dropping the old constraint value; create `site_daily_report_approval_level` and `daily_report_approval` tables (mirror `V38__create_orcamento_approval_table.sql`'s / the current `purchase_request_approval` table's column shapes).
2. Backend: new entities/repositories/services/controller for the two new tables; `DailyReportService` submit/approve/reject methods; visibility changes in `list`/`requireReport`; `AccessLevel` javadoc update; `SitePermissionService` allows `VIEW_AND_APPROVE` for `DAILY_REPORT`.
3. Frontend: approval UI + status badges + new config screen.
4. Mobile: approval UI + status badges (no config screen, per Non-Goals).
5. Rollback: the migration is additive except for the status-constraint/value change and the `SUBMITTED`→`APPROVED` rename, which is not cleanly reversible without knowing which `APPROVED` rows were originally `SUBMITTED` — if rollback is ever needed, restore from a pre-migration backup rather than attempting a reverse migration.

## Open Questions

None outstanding — the status set, permission model, and visibility rule were all confirmed directly with the user before writing this proposal.
