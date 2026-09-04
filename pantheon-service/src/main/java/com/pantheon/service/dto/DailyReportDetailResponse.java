package com.pantheon.service.dto;

import java.util.List;

public record DailyReportDetailResponse(
        DailyReportResponse report,
        List<WorkforceEntryResponse> workforceEntries,
        List<EquipmentUsageResponse> equipmentUsage,
        List<ActivityResponse> activities,
        List<OccurrenceResponse> occurrences,
        List<MaterialReceivedResponse> materialsReceived) {
}
