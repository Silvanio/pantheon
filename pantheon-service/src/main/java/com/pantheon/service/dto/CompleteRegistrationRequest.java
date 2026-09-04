package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteRegistrationRequest(
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotBlank String displayName) {
}
