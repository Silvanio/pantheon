package com.pantheon.service.dto;

import java.util.UUID;

/** Either {@code equipmentId} (registered) or {@code customName} ("outro equipamento") must be given — enforced in {@code DailyReportService#addEquipmentUsage}, mirroring {@link WorkforceEntryRequest}'s membershipId-or-roleDescription rule. */
public record EquipmentUsageRequest(UUID equipmentId, String customName, String statusNote) {
}
