package com.pantheon.service.dto;

/**
 * Public details of an invitation, looked up by token. {@code accepted} is true once the
 * invited person has accepted; {@code requiresRegistration} is true when the invited email
 * had no account and must set a password before accepting.
 */
public record InvitationResponse(
        String projectName,
        String inviterName,
        String email,
        boolean requiresRegistration,
        boolean accepted) {
}
