## 1. Database migration

- [x] 1.1 New migration `V65__add_daily_report_approval_workflow.sql` (confirmed `V64` was still
      the highest at the time of writing): `UPDATE daily_report SET status = 'APPROVED' WHERE
      status = 'SUBMITTED'` first, then adds `daily_report_status_check CHECK (status IN
      ('DRAFT','PENDING_APPROVAL','APPROVED'))`. Note: `daily_report.status` had no DB-level check
      constraint before this change (just `VARCHAR(20) NOT NULL`, enforced only at the JPA enum
      level) — design.md's Migration Plan assumed one existed to "alter"; there was none to alter,
      so this migration adds one fresh instead. Also adds `daily_report.current_approval_cycle INT
      NOT NULL DEFAULT 0` (needed for the entity field added in 2.4, not explicitly listed in
      design.md's Migration Plan bullet but required by task 2.4 below).
- [x] 1.2 Same migration: created `site_daily_report_approval_level` and `daily_report_approval`,
      column-for-column and index-for-index mirrors of `site_purchase_request_approval_level` /
      `purchase_request_approval` (see `V54__move_approval_from_orcamento_to_purchase_request.sql`).
      Verified by running the full test suite against the real dev Postgres — Flyway validated and
      applied all 65 migrations cleanly (`./mvnw test`, see group 6 below).

## 2. Backend: entities, status transitions

- [x] 2.1 `DailyReportStatus`: `DRAFT`, `PENDING_APPROVAL`, `APPROVED` (`SUBMITTED` removed).
- [x] 2.2 New entity `SiteDailyReportApprovalLevel`, mirroring `SitePurchaseRequestApprovalLevel`
      field-for-field (same constructor shape, `deactivate(Instant)`, all getters).
- [x] 2.3 New entity `DailyReportApproval` + `DailyReportApprovalStatus` (`PENDING`/`APPROVED`/
      `REJECTED`, confirmed matching `PurchaseRequestApprovalStatus`'s exact values), mirroring
      `PurchaseRequestApproval` field-for-field including `approve(...)`/`reject(...)`.
- [x] 2.4 `DailyReport.submit(Instant)` now transitions `DRAFT` → `PENDING_APPROVAL` and increments
      the new `currentApprovalCycle` field (default `0`, mirroring `PurchaseRequest`'s); added
      `approve(Instant)` (→ `APPROVED`) and `reject(Instant)` (→ `DRAFT`). Design decision: unlike
      `PurchaseRequest`, no `lastRejectionReason` field was added to the `DailyReport` header —
      design.md Decision 2 only specifies the rejection reason is recorded on the
      `DailyReportApproval` step itself, so the header mirror stops short of that one PR-specific
      field.
- [x] 2.5 `isEditable()` unchanged in logic (`status == DRAFT`), now checked against the new enum.

## 3. Backend: repositories, services, controllers for approval levels + steps

- [x] 3.1 `SiteDailyReportApprovalLevelRepository`, `SiteDailyReportApprovalLevelService` (
      `getEffectiveLevels`/`setLevels`, including the "no rows → single `ENGINEER` default,
      non-persisted" behavior, verified by test), `SiteDailyReportApprovalLevelController` at
      `/api/construction-sites/{siteId}/daily-report-approval-levels` (`GET`/`PUT`, company-staff-
      only via the same `requireCompanyStaff` pattern as the PR controller). New DTOs:
      `SiteDailyReportApprovalLevelResponse`, `SetDailyReportApprovalLevelsRequest`,
      `DailyReportApprovalLevelEntry`, `DailyReportApprovalResponse`, `RejectDailyReportRequest`.
- [x] 3.2 `DailyReportApprovalRepository`, mirroring `PurchaseRequestApprovalRepository`'s finders
      (by report+cycle, lowest-pending-by-step-order, exists-decided-by-function). The
      exists-decided-by-function finder is unused by `DailyReportService` today (Daily Report's
      `VIEW_AND_APPROVE` has no per-function dynamic visibility filtering, unlike Purchase
      Request's — see design.md Decision 3) but is included per this task's explicit instruction,
      for parity/future use.
- [x] 3.3 `DailyReportService.submit(...)` (kept its existing name/endpoint rather than renaming to
      `submitForApproval`, since `DailyReportController` already exposed `POST
      /api/daily-reports/{id}/submit` and no second "submit" method exists to disambiguate from):
      on submit, loads effective levels via `SiteDailyReportApprovalLevelService`, creates one
      `DailyReportApproval` per level for the new cycle, transitions the report to
      `PENDING_APPROVAL`.
- [x] 3.4 `DailyReportService.approveStep(...)`/`rejectStep(...)` added, copying
      `PurchaseRequestService`'s authorization mechanism verbatim (`requireStepAuthority`,
      `resolveSiteMembership`): function match or company-staff-with-no-site-role bypass; resolved
      `DAILY_REPORT` access must be `MANAGE` or `VIEW_AND_APPROVE`. Approving the final step →
      `APPROVED`; a non-final step leaves the next step actionable; rejecting requires a
      non-blank reason, marks the step `REJECTED` with that reason, returns the report to `DRAFT`.
      Design decision: no push/email notification is sent when a new step becomes pending (unlike
      `PurchaseRequestService.notifyStepPending`) — neither proposal.md nor design.md mentions
      notifications for this workflow, so this was left out to keep scope to what was specified;
      flagged here in case product wants parity with Pedido de Compra's notification behavior
      later.
- [x] 3.5 New endpoints on `DailyReportController`: `POST /api/daily-reports/{id}/approve-step`
      (optional `comment` query param) and `POST /api/daily-reports/{id}/reject-step` (`{reason}`
      body via `RejectDailyReportRequest`), mirroring `PurchaseRequestController`'s shapes exactly.
      `DailyReportDetailResponse` gained an `approvals: List<DailyReportApprovalResponse>` field
      (mirroring `PurchaseRequestDetailResponse`), populated via a new
      `DailyReportService.listApprovals(...)`.

## 4. Backend: visibility rule changes

- [x] 4.1 `DailyReportService.requireReport(...)`: after the existing `requireVisible` (`HIDDEN`)
      check, added `requireApprovedForViewOnly(...)` — if resolved `DAILY_REPORT` access is `VIEW`
      and `report.getStatus() != APPROVED`, throws `DailyReportNotFoundException` (same "behaves
      as if it doesn't exist" 404 pattern as `PurchaseRequestService#requireViewAndApproveVisibility`).
- [x] 4.2 `DailyReportService.list(...)`: when resolved `DAILY_REPORT` access is exactly `VIEW`,
      delegates to a new `DailyReportRepository.findByConstructionSiteIdAndStatus(...)` finder
      instead of `findByConstructionSiteId(...)` — pushed into the query (one query, correct
      pagination totals) rather than filtered in memory afterwards, so no per-row work or N+1 is
      introduced.
- [x] 4.3 `AccessLevel.java` javadoc rewritten: `VIEW_AND_APPROVE` now documented as meaningful for
      both `PURCHASE_REQUEST` and `DAILY_REPORT` (same acting-side mechanism), with the passive
      `VIEW`-visibility contrast between the two capabilities spelled out explicitly.
- [x] 4.4 `SitePermissionService`: confirmed no code anywhere restricts which `AccessLevel` values
      are settable per `PermissionCapability` (`resolve`/`setFunctionOverride`/`setMemberOverride`
      accept any `AccessLevel` for any capability already) — so `VIEW_AND_APPROVE` was already
      settable for `DAILY_REPORT` with zero code changes needed; only the class/`canApprove`
      javadoc was updated to state this explicitly instead of "meaningful only for
      `PURCHASE_REQUEST`".

## 5. Backend: sign-off status rename

- [x] 5.1 `DailyReportSignatureService.sign(...)`'s gate now checks `status == APPROVED` instead of
      `status == SUBMITTED`. Also renamed `DailyReportNotSubmittedException` →
      `DailyReportNotApprovedException` (message updated to "must be approved" instead of "must be
      submitted", since the old wording would now be actively misleading — a `PENDING_APPROVAL`
      report was arguably "submitted" under the new terminology but still isn't sign-off-eligible).
      Only 3 files referenced the old exception (the service, its handler, the class itself), all
      within this change's scope, so the rename was low-risk.

## 6. Backend tests

- [x] 6.1 Updated `DailyReportServiceTest` (no `DailyReportControllerTest` exists in this codebase
      for any controller — confirmed no controller-level tests exist anywhere under
      `pantheon-service`, so none was added here either, consistent with existing coverage depth).
      Constructor call updated for the two new dependencies; existing tests needed no assertion
      changes since `report.submit(...)` kept its name.
- [x] 6.2 New `SiteDailyReportApprovalLevelServiceTest` (3 tests: configured-levels-returned,
      no-rows-defaults-to-ENGINEER, setLevels-deactivates-and-persists). No PR-side
      `SitePurchaseRequestApprovalLevelServiceTest` exists to mirror (confirmed) — wrote a
      reasonable equivalent from scratch instead, following this codebase's Mockito style. No
      controller test, per the same no-controller-tests-anywhere precedent as 6.1.
- [x] 6.3 New tests in `DailyReportServiceTest` covering every scenario from the
      `daily-report-approval-workflow` spec: intermediate step approved, final step approved →
      `APPROVED`, reject → `DRAFT` with reason recorded on the step, non-matching member blocked
      (`NotCurrentDailyReportApprovalStepException`), company-staff-with-matching-role decides as
      that role, company-staff-with-no-site-role bypass, company-staff-with-non-matching-role still
      blocked, view-only blocked from approving even on function match, submit creates steps from
      the default level and transitions to `PENDING_APPROVAL`.
- [x] 6.4 New visibility-rule tests: `VIEW` sees only `APPROVED` reports in `list()` (and
      `MANAGE`/`VIEW_AND_APPROVE` both see every status, each asserted separately); direct access
      via `requireReport(...)` (exercised through `listWorkforceEntries(...)`) — `VIEW` gets
      `DailyReportNotFoundException` on a non-`APPROVED` report and succeeds once `APPROVED`;
      `MANAGE`/`VIEW_AND_APPROVE` succeed regardless of status.
- [x] 6.5 New `DailyReportSignatureServiceTest` (didn't exist before this change): sign rejected on
      `DRAFT`, rejected on `PENDING_APPROVAL`, succeeds on `APPROVED` and returns a signature
      attributed to the acting member's function.

Full suite: `./mvnw test` from `pantheon-service/` — **366 tests, 0 failures, 0 errors**, including
`PantheonServiceApplicationTests`' real-Postgres Flyway validation of all 65 migrations.

## 7. Frontend (`pantheon-web`)

- [x] 7.1 Update the Diário de Obra detail view: submit-for-approval action (replacing the old plain "submit"), approval-step display + approve/reject actions (reject requires a reason) for `MANAGE`/`VIEW_AND_APPROVE` members — mirror `PurchaseRequestDetailView.vue`'s approval UI pattern. `DailyReportDetailView.vue` now loads the viewer's resolved `DAILY_REPORT` access level and site function (mirroring `PurchaseRequestDetailView.vue`'s `loadDetail`), gates the submit button on `MANAGE` only (`canSubmit`), and adds an "Approval" card (current pending step + approve/reject actions gated by `canActOnApproval`, reject-reason inline form, and a per-cycle approval history list) directly under the header. The submit action's confirmation now goes through `window.confirm` no longer being used (removed, matching AGENTS.md's ban on native dialogs and `PurchaseRequestDetailView.vue`'s own submit action, which has no confirm step either) and reloads the full detail (including `approvals`) after submit/approve/reject, same as the PR view. `useDailyReports.ts` gained `approveStep`/`rejectStep` (mirroring `usePurchaseRequests.ts`'s shapes exactly: `POST .../approve-step?comment=`, `POST .../reject-step` with `{reason}` body), the `DailyReportApproval` interface, and `approvals: DailyReportApproval[]` on `DailyReportDetail`. New `src/composables/useDailyReportApprovalLevels.ts` (mirrors `usePurchaseRequestApprovalLevels.ts`).
- [x] 7.2 Status badges for `DRAFT`/`PENDING_APPROVAL`/`APPROVED`: added a `dailyReport` kind to `StatusBadge.vue`'s `CLASS_MAP`/`LABEL_KEY` (`SUBMITTED` mapping never existed there before; the view previously rendered plain text instead of a badge). Now used in `DailyReportDetailView.vue`'s header and `DailyReportsPanel.vue`'s history table (previously plain `t('dailyReports.status...')` text), matching `PurchaseRequestPanel.vue`'s existing use of `StatusBadge`. Sign-off gating (`isApproved` computed) was also corrected to require `APPROVED` specifically, not just "not DRAFT" (which would have wrongly allowed attempting to sign a `PENDING_APPROVAL` report after this change).
- [x] 7.3 New approval-levels configuration screen for Diário de Obra: `src/components/SiteDailyReportApprovalLevelsPanel.vue` (mirrors `SitePurchaseRequestApprovalLevelsPanel.vue` field-for-field), wired to `useDailyReportApprovalLevels.ts` and the `/api/construction-sites/{siteId}/daily-report-approval-levels` endpoint. Placed in `SiteDetailView.vue`'s Permissões tab, directly under `SitePurchaseRequestApprovalLevelsPanel`, so both approval-level configuration screens sit together, each with its own clearly-labeled title ("Níveis de aprovação de pedido de compra" vs. "Níveis de aprovação de Diário de Obra").
- [x] 7.4 Report history/list view: confirmed `DailyReportsPanel.vue` does no client-side status filtering of its own — it renders whatever page `GET /api/construction-sites/{siteId}/daily-reports` returns, so the backend's `VIEW`-only-sees-`APPROVED` filter (task 4.2) applies automatically with no frontend change needed beyond the status badge update in 7.2.
- [x] 7.5 `pt-BR.json`: updated `dailyReports.status.*` (`PENDING_APPROVAL`/`APPROVED` replacing `SUBMITTED`), `dailyReports.detail.submitButton`/`submitError` copy for "enviar para aprovação" (and removed the now-unused `submitConfirm` key), `dailyReports.signatures.signRequiresSubmit` copy (now "precisa estar aprovado"), and added `dailyReports.approveStepButton`/`rejectStepButton`/`rejectReasonPlaceholder`/`confirmReject`/`approvalError`/`approvalHistoryTitle`/`pendingStepSubtitle`/`cycleLabel`/`stepLabel`/`approverFunction.*` (mirroring the equivalent `purchaseRequests.*` keys) and `dailyReports.approvalLevels.*` (config screen copy).

## 8. Mobile (`pantheon-mobile`)

- [x] 8.1 Updated `daily_report_detail_screen.dart`: submit-for-approval action (relabeled
      "Enviar para aprovação", now requires being online instead of queueing offline — since it
      now creates approval steps, not just flipping a status flag), approval-step display +
      approve/reject actions (reject requires a reason via a dialog) for `MANAGE`/
      `VIEW_AND_APPROVE` members — mirrors `purchase_request_detail_screen.dart`'s `_Authority`/
      `canActOn`/`currentCycle`/`currentCyclePending`/`_act` pattern verbatim, keyed off the
      `DAILY_REPORT` capability instead of `PURCHASE_REQUEST`. New `DailyReportRepository`
      methods `approveStep`/`rejectStep` (plus `submit` changed from `mutateQueueable` to a plain
      online `POST`), and a new `DailyReportApproval` model / `DailyReportDetail.approvals` field
      mirroring `PurchaseRequestApproval`/`PurchaseRequestDetail`. No client-side visibility
      filtering was added for `VIEW_AND_APPROVE`/`MANAGE` (design.md Decision 3: unlike Pedido de
      Compra, Daily Report's approver access sees every status already, purely server-side).
- [x] 8.2 Status display for the 3 new status values: added `StatusBadgeKind.dailyReport`
      (`DRAFT`/`PENDING_APPROVAL`/`APPROVED`) to `status_badge.dart` and switched both
      `daily_report_detail_screen.dart`'s header badge and `daily_report_list_screen.dart`'s
      list-card badge from the old ad hoc draft/"Enviado" two-state `Container` to the shared
      `StatusBadge` widget (the old code had no `SUBMITTED` case to remove — it never used
      `StatusBadgeKind` at all, just an inline `isDraft` ternary).
- [x] 8.3 No approval-levels configuration screen on mobile (matches the existing Pedido de Compra scoping — confirmed no such screen exists there either).

## 9. Verification

- [ ] 9.1 `./mvnw test` from `pantheon-service/` — full suite green.
- [ ] 9.2 `npm run build`/`vue-tsc --noEmit` from `pantheon-web/` — clean.
- [ ] 9.3 `flutter analyze` / `flutter test` from `pantheon-mobile/` — clean.
- [ ] 9.4 Manual end-to-end check: submit a report, approve through a 2-step chain as each function in turn, confirm `APPROVED`; submit another, reject with a reason, confirm it returns to `DRAFT` and can be resubmitted (new cycle); confirm a `VIEW`-only member cannot see either report until `APPROVED`.
