package com.pantheon.service.messaging;

import java.util.UUID;

/**
 * Payload of the {@code team-invitation-created} event. Carries the raw invitation token
 * (stored only as a hash in this service) so pantheon-message can build the invitation
 * link for the email.
 */
public record TeamInvitationCreatedEvent(
        UUID invitationId,
        String email,
        String token,
        UUID projectId,
        String projectName,
        String inviterName,
        boolean requiresRegistration) {

    public static final String TYPE = "team-invitation-created";
}
