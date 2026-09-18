package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectPurchaseRequestRequest(@NotBlank String reason) {
}
