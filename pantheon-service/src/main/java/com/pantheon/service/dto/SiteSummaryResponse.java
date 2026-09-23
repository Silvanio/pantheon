package com.pantheon.service.dto;

/** Every field is {@code null} when the caller's resolved access to the matching capability is
 * {@code HIDDEN} — the section is omitted, not returned empty/zeroed. */
public record SiteSummaryResponse(
        Integer schedulePercentComplete,
        DailyReportSummaryResponse dailyReports,
        PurchaseRequestSummaryResponse purchaseRequests,
        OrcamentoSummaryResponse orcamentos,
        EquipmentSummaryResponse equipment,
        CountStatResponse projects,
        CountStatResponse tasks,
        Long teamMembersCount) {
}
