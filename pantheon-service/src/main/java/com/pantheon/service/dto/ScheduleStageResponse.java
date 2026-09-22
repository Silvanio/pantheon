package com.pantheon.service.dto;

import com.pantheon.service.entity.ScheduleStage;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleStageResponse(
        UUID id,
        UUID constructionSiteId,
        String name,
        String color,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        int percentComplete,
        List<ScheduleTaskResponse> tasks) {

    /** {@code percentComplete} is always derived from {@code tasks} — {@code 0} when there are none. */
    public static ScheduleStageResponse from(ScheduleStage stage, List<ScheduleTaskResponse> tasks) {
        int percentComplete = tasks.isEmpty()
                ? 0
                : (int) Math.round(tasks.stream().mapToInt(ScheduleTaskResponse::percentComplete).average().orElse(0));
        return new ScheduleStageResponse(
                stage.getId(), stage.getConstructionSiteId(), stage.getName(), stage.getColor(), stage.getStartDate(),
                stage.getEndDate(), stage.getSortOrder(), percentComplete, tasks);
    }
}
