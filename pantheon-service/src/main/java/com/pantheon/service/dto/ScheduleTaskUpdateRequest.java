package com.pantheon.service.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Every field is optional — only non-null fields are applied (PATCH semantics). {@code clearResponsible}
 * unassigns the task's responsible member; it takes precedence over {@code responsibleSiteMembershipId}. */
public record ScheduleTaskUpdateRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        UUID responsibleSiteMembershipId,
        boolean clearResponsible,
        Integer percentComplete,
        Integer sortOrder) {
}
