package com.pantheon.service.dto;

import com.pantheon.service.entity.ScheduleTask;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleTaskResponse(
        UUID id,
        UUID stageId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        UUID responsibleSiteMembershipId,
        int percentComplete,
        int sortOrder,
        List<ScheduleDependencyRef> dependsOn,
        UUID taskCardId,
        String taskCardTitle) {

    /** {@code taskCardTitle} is resolved by the caller (a lookup keyed on {@code task.getTaskCardId()}) —
     * {@code null} both when there's no link and when the linked card was deleted independently. */
    public static ScheduleTaskResponse from(ScheduleTask task, List<ScheduleDependencyRef> dependsOn, String taskCardTitle) {
        return new ScheduleTaskResponse(
                task.getId(), task.getStageId(), task.getTitle(), task.getStartDate(), task.getEndDate(),
                task.getResponsibleSiteMembershipId(), task.getPercentComplete(), task.getSortOrder(),
                dependsOn, task.getTaskCardId(), taskCardTitle);
    }
}
