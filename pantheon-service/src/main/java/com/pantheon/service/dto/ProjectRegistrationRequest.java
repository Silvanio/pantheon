package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

import com.pantheon.service.entity.UserProfile;
/**
 * cnpjCpf/legalName/address/postalCode describe the requesting user (stored as their
 * {@link com.pantheon.service.entity.UserProfile}, upserted on each project creation), not the
 * project itself — the project only has a name.
 */
public record ProjectRegistrationRequest(
        @NotBlank String projectName,
        @NotBlank String cnpjCpf,
        @NotBlank String legalName,
        @NotBlank String address,
        @NotBlank String postalCode) {
}
