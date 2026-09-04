package com.pantheon.service.dto;

import java.util.UUID;

/**
 * Result of adding a member: the created (or re-issued) invitation. The raw token is never
 * included here — it is delivered only in the invitation email.
 */
public record MemberInvitationResponse(
        UUID invitationId,
        UUID membershipId,
        String email,
        boolean requiresRegistration) {
}
