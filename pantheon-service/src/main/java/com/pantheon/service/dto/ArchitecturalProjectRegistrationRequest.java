package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ArchitecturalProjectRegistrationRequest(
        @NotBlank String name,
        String description,
        UUID constructionSiteId) {
}
