package com.pantheon.service.messaging;

import java.util.UUID;

/**
 * Payload of the {@code team-invitation-created} event. Carries the raw invitation token
 * (stored only as a hash in this service) so pantheon-message can build the invitation
 * link for the email. {@code targetId}/{@code targetName} identify the company or
 * construction site being invited to, depending on {@code membershipType}.
 */
public record TeamInvitationCreatedEvent(
        UUID invitationId,
        String email,
        String token,
        UUID targetId,
        String targetName,
        String membershipType,
        String inviterName,
        boolean requiresRegistration) {

    public static final String TYPE = "team-invitation-created";
}
