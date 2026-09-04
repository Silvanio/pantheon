package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record MaterialRegistrationRequest(@NotBlank String name, @NotBlank String unit) {
}
