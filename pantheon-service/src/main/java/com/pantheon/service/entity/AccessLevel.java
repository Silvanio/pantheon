package com.pantheon.service.entity;

/**
 * Resolved or configured access for a {@link PermissionCapability}. Most capabilities only ever
 * use {@code VIEW} (unrestricted read) or {@code MANAGE} (full read/write). {@code HIDDEN} means
 * no access at all — not even read — and the capability's menu is not shown to that member;
 * company staff can never resolve to {@code HIDDEN} (see {@code SitePermissionService#resolve}).
 * {@code VIEW_AND_APPROVE} is meaningful for both {@code PURCHASE_REQUEST} and
 * {@code DAILY_REPORT}: it cannot create or submit either document, but can act on an approval
 * step matching the member's {@code SiteMembership} function (see
 * {@code PurchaseRequestService#requireStepAuthority} and {@code DailyReportService}'s
 * equivalent step-authorization check — the two share the exact same function-match /
 * company-staff-with-no-site-role mechanism).
 *
 * <p>The two capabilities differ, though, on plain {@code VIEW}'s <em>passive</em> visibility —
 * this is deliberate, not an oversight (see {@code add-daily-report-approval-workflow}'s
 * design.md, Decision 3): {@code PURCHASE_REQUEST}'s {@code VIEW_AND_APPROVE} only sees a Pedido
 * de Compra that is {@code CONCLUIDO} or relevant to the member's function in the current
 * approval cycle (see {@code PurchaseRequestService}'s dynamic-visibility helper), while plain
 * {@code VIEW} there sees everything. {@code DAILY_REPORT} inverts which level gets the
 * restriction: its plain {@code VIEW} only ever sees a report once it is {@code APPROVED} (a
 * {@code DRAFT}/{@code PENDING_APPROVAL} report behaves as if it doesn't exist — see
 * {@code DailyReportService#list}/{@code #requireReport}), while its {@code VIEW_AND_APPROVE}
 * has no such dynamic filtering and sees a report in every status, exactly like {@code MANAGE}.
 */
public enum AccessLevel {
    VIEW,
    MANAGE,
    VIEW_AND_APPROVE,
    HIDDEN
}
