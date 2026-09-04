package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.pantheon.service.entity.ActivityStatus;
public record ActivityRequest(
        @NotBlank String description, @NotBlank String progressNote, @NotNull ActivityStatus status) {
}
