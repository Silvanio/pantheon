package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MoveTaskCardRequest(@NotNull UUID columnId, @NotNull Integer sortOrder) {
}
