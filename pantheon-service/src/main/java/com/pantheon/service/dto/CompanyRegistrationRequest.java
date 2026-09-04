package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

/** Company creation is name-only; plan selection and profile completion are separate steps. */
public record CompanyRegistrationRequest(@NotBlank String companyName) {
}
