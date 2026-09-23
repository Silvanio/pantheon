package com.pantheon.service.dto;

import java.time.LocalDate;

public record DailyReportSummaryResponse(long total, LocalDate lastReportDate) {
}
