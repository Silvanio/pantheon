package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.ActivityStatus;
import com.pantheon.service.entity.DailyReportActivity;
public record ActivityResponse(UUID id, String description, String progressNote, ActivityStatus status) {

    public static ActivityResponse from(DailyReportActivity activity) {
        return new ActivityResponse(
                activity.getId(), activity.getDescription(), activity.getProgressNote(), activity.getStatus());
    }
}
