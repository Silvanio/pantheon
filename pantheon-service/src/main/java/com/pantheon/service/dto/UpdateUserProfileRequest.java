package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank String cnpjCpf, @NotBlank String legalName, @NotBlank String address, @NotBlank String postalCode) {
}
