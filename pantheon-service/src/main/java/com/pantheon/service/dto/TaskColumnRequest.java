package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskColumnRequest(@NotBlank String name) {
}
