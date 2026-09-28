package com.pantheon.service.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportStatus;
public record DailyReportResponse(
        UUID id,
        UUID constructionSiteId,
        LocalDate reportDate,
        int sequenceNo,
        DailyReportStatus status,
        String weatherConditionMorning,
        String weatherConditionAfternoon,
        Boolean weatherBlockedTasks,
        LocalTime workHoursStart,
        LocalTime workHoursEnd,
        String comments,
        Instant submittedAt) {

    public static DailyReportResponse from(DailyReport report) {
        return new DailyReportResponse(
                report.getId(),
                report.getConstructionSiteId(),
                report.getReportDate(),
                report.getSequenceNo(),
                report.getStatus(),
                report.getWeatherConditionMorning(),
                report.getWeatherConditionAfternoon(),
                report.getWeatherBlockedTasks(),
                report.getWorkHoursStart(),
                report.getWorkHoursEnd(),
                report.getComments(),
                report.getSubmittedAt());
    }
}
