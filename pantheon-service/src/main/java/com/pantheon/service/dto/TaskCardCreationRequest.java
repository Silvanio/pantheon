package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TaskCardCreationRequest(@NotNull UUID columnId, @NotBlank String title, String description) {
}
