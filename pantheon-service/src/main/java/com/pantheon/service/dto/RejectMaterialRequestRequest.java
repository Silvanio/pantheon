package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectMaterialRequestRequest(@NotBlank String reason) {
}
