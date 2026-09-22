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
        List<ScheduleDependencyRef> dependsOn) {

    public static ScheduleTaskResponse from(ScheduleTask task, List<ScheduleDependencyRef> dependsOn) {
        return new ScheduleTaskResponse(
                task.getId(), task.getStageId(), task.getTitle(), task.getStartDate(), task.getEndDate(),
                task.getResponsibleSiteMembershipId(), task.getPercentComplete(), task.getSortOrder(),
                dependsOn);
    }
}
