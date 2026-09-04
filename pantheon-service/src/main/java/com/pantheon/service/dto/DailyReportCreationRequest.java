package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DailyReportCreationRequest(@NotNull LocalDate reportDate) {
}
