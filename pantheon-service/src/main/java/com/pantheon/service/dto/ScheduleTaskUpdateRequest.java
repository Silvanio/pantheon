package com.pantheon.service.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Every field is optional — only non-null fields are applied (PATCH semantics); a boxed
 * {@code Boolean} (not primitive) so a request that omits it entirely deserializes cleanly.
 * {@code clearResponsible} unassigns the task's responsible member; it takes precedence over
 * {@code responsibleSiteMembershipId} when {@code true}. */
public record ScheduleTaskUpdateRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        UUID responsibleSiteMembershipId,
        Boolean clearResponsible,
        Integer percentComplete,
        Integer sortOrder) {
}
