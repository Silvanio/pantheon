package com.pantheon.service.exception;

import java.util.UUID;

/**
 * Thrown when adding a {@code DailyReportEquipmentUsage} entry with neither a registered {@code
 * equipmentId} nor a free-text {@code customName} — exactly one is required, mirroring {@code
 * WorkforceEntryRequest}'s membershipId-or-roleDescription rule. See {@code
 * daily-construction-report}'s "Equipment usage logging".
 */
public class EquipmentReferenceRequiredException extends RuntimeException {

    public EquipmentReferenceRequiredException(UUID reportId) {
        super("Either equipmentId or customName is required for a daily report equipment usage entry: " + reportId);
    }
}
