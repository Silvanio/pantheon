package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;

public record TaskColumnReorderRequest(@NotNull Integer sortOrder) {
}
