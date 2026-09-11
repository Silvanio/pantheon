package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TaskLabelRequest(
        @NotBlank String name, @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String colorHex) {
}
