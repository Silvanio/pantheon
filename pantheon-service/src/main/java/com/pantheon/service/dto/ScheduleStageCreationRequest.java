package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ScheduleStageCreationRequest(
        @NotBlank String name, @NotBlank String color, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {
}
