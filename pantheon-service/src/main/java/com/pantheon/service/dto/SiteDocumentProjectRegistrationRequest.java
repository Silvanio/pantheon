package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SiteDocumentProjectRegistrationRequest(@NotBlank String name, UUID parentId, UUID taskCardId) {
}
