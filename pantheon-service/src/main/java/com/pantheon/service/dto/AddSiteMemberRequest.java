package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import jakarta.validation.constraints.NotNull;

/**
 * {@code email} is required for CLIENT/ARCHITECT/ENGINEER/SITE_FOREMAN (invite-driven) and
 * optional for SERVICE_PROVIDER (accountless by default). {@code trade} is only meaningful for
 * SERVICE_PROVIDER; {@code cpf} only for CLIENT; {@code displayName}/{@code contactEmail} are
 * used for an accountless SERVICE_PROVIDER (no {@code email}/invite involved).
 */
public record AddSiteMemberRequest(
        @NotNull ConstructionFunction function,
        String email,
        String cpf,
        String trade,
        String displayName,
        String contactEmail) {
}
