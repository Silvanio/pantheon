package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ConstructionSiteRegistrationRequest(
        @NotBlank String name,
        @NotBlank String address,
        @NotNull LocalDate startDate,
        LocalDate expectedEndDate) {
}
