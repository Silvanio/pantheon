package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameSiteDocumentProjectRequest(@NotBlank String name) {
}
