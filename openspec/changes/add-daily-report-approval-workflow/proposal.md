## Why

Diário de Obra today has only two statuses (`DRAFT`, `SUBMITTED`) and no approval gate — any member with `MANAGE` access can submit it directly, after which anyone can sign off. The user wants a real, multi-level, per-site-configurable approval workflow before a report is considered final, mirroring the one that already exists and works well for Pedido de Compra — with permission levels (view-only vs. approve) matching Pedido de Compra's model, and a stricter visibility rule for pure viewers: they should only ever see a report once it's fully approved.

## What Changes

- **New status set** for `DailyReportStatus`: `DRAFT` → `PENDING_APPROVAL` → `APPROVED` (replaces `DRAFT`/`SUBMITTED`; `SUBMITTED` is renamed to `APPROVED`, since it now means "approved," not just "sent"). Existing `SUBMITTED` rows migrate to `APPROVED`.
- **New per-site configurable approval levels for Daily Report** (`SiteDailyReportApprovalLevel`), configured by company staff exactly like Pedido de Compra's levels — an ordered list of `{stepOrder, approverFunction}`, defaulting to a single `ENGINEER` step when unconfigured.
- **New `DailyReportApproval` entity** (per-report approval steps: cycle number, step order, approver function, status, decided-by membership, decided-at, comment) — submitting for approval creates one step per configured level, all `PENDING`, tagged with a new cycle. Approving the last pending step moves the report to `APPROVED`; rejecting requires a reason, marks the step `REJECTED`, and returns the report to `DRAFT` for edits (a later resubmission starts a new cycle, old cycles kept for history).
- **`AccessLevel.VIEW_AND_APPROVE` extended to `PermissionCapability.DAILY_REPORT`** (today it's documented as meaningful only for `PURCHASE_REQUEST`). Acting on a Daily Report approval step reuses Pedido de Compra's exact authorization mechanism: the acting user's `SiteMembership` function must match the step's function (or the user is company staff with no site role of their own on that site), and their resolved `DAILY_REPORT` access must be `MANAGE` or `VIEW_AND_APPROVE`.
- **New, Daily-Report-specific visibility rule for plain `VIEW` access — intentionally different from how Pedido de Compra's `VIEW_AND_APPROVE` visibility works**: a member with plain `VIEW` access only sees a report (in listings and on direct access) once it is `APPROVED`; `DRAFT`/`PENDING_APPROVAL` reports behave as if they don't exist to that member. `MANAGE` and `VIEW_AND_APPROVE` members see reports in every status (they need to, to write drafts or act on pending approvals).
- **BREAKING**: `daily-construction-report`'s "Daily report listing and detail" requirement currently states any member sees every report "regardless of whether their daily-report access is `VIEW` or `MANAGE`" — this is no longer true for `VIEW` once this change ships.
- Frontend (`pantheon-web`) and mobile (`pantheon-mobile`) get submit/approve/reject actions and status displays mirroring Pedido de Compra's existing approval UI, plus a new approval-levels configuration screen (web; see design.md for whether mobile needs one too).

## Capabilities

### New Capabilities
- `daily-report-approval-workflow`: per-site configurable approval levels, submitting a Diário de Obra for approval, and acting on (approving/rejecting) an approval step — mirrors `purchase-request-approval-workflow`'s structure.

### Modified Capabilities
- `daily-construction-report`: the "Daily report submission" requirement's target status changes from `SUBMITTED` to `PENDING_APPROVAL` (approval, not submission alone, now reaches a final state); the "Daily report listing and detail" requirement's "any member sees every report regardless of VIEW/MANAGE" guarantee is replaced by the new VIEW-only-sees-APPROVED rule.
- `daily-report-media-and-signoff`: the "Report sign-off" requirement's status gate changes from `SUBMITTED` to `APPROVED` (sign-off still only makes sense once a report is finalized; the terminal status is simply renamed).

## Impact

- **Affected code**: `pantheon-service` — `DailyReportStatus`, `DailyReport` entity (submit/approve/reject transitions), new `DailyReportApproval`/`SiteDailyReportApprovalLevel` entities/repositories/services/controllers, `DailyReportService`'s visibility helpers, `AccessLevel` javadoc, `SitePermissionService` (allow `VIEW_AND_APPROVE` for `DAILY_REPORT`), new Flyway migration(s). `pantheon-web` — `DailyReportDetailView.vue` (or equivalent) approval actions/status badges, new approval-levels config screen (likely under Permissões). `pantheon-mobile` — `daily_report_detail_screen.dart` approval actions/status display.
- **Data migration**: existing `SUBMITTED` rows become `APPROVED`.
- **Affected specs**: `daily-construction-report`, `daily-report-media-and-signoff` (both modified), new `daily-report-approval-workflow`.
