package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record SiteDocumentProjectRegistrationRequest(@NotBlank String name) {
}
