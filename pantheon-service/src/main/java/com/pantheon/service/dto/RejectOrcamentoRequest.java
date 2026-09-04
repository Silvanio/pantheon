package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectOrcamentoRequest(@NotBlank String reason) {
}
