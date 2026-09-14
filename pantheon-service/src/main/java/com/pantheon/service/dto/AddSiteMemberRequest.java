package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code displayName} (the person's name) is required for every function. {@code email} is
 * required for every function except SERVICE_PROVIDER, which is accountless by default when
 * {@code email} is omitted (enforced in the service, since it depends on {@code function}, not
 * just presence). {@code cpf} and {@code contactEmail}/{@code phone} are optional for every
 * function; {@code trade} is only meaningful for SERVICE_PROVIDER. A supplied {@code cpf} is
 * format/check-digit validated; a supplied {@code email} is format-validated.
 */
public record AddSiteMemberRequest(
        @NotNull ConstructionFunction function,
        @NotBlank String displayName,
        @Email String email,
        String cpf,
        String phone,
        String trade,
        String contactEmail) {
}
