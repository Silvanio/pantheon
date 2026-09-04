package com.pantheon.service.service;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.TeamInvitationCreatedEvent;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Shared invitation-issuing logic used by both company-staff invites and construction-site
 * team invites: the token/email/event mechanics are identical regardless of which membership
 * table the invitation targets (see design.md decision 4).
 */
@Service
public class MembershipInvitationIssuer {

    private static final Duration INVITATION_TTL = Duration.ofDays(14);

    private final AppUserRepository userRepository;
    private final MembershipInvitationRepository invitationRepository;
    private final EventPublisher eventPublisher;

    public MembershipInvitationIssuer(
            AppUserRepository userRepository,
            MembershipInvitationRepository invitationRepository,
            EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.eventPublisher = eventPublisher;
    }

    public record ResolvedUser(AppUser user, boolean preRegistered) {
    }

    /** Finds the account for {@code email}, or creates a pre-registration one if none exists. */
    public ResolvedUser resolveOrCreateUser(String email, Instant now) {
        AppUser existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            return new ResolvedUser(existing, false);
        }
        AppUser preRegistered = userRepository.save(AppUser.preRegistration(UUID.randomUUID(), email, now));
        return new ResolvedUser(preRegistered, true);
    }

    /** Creates a brand-new invitation for a freshly created membership and publishes the event. */
    public MembershipInvitation issue(
            MembershipType type,
            UUID membershipId,
            String email,
            UUID actingUserId,
            boolean requiresRegistration,
            UUID targetId,
            String targetName,
            String inviterName,
            Instant now) {
        String rawToken = InvitationToken.generate();
        MembershipInvitation invitation = invitationRepository.save(new MembershipInvitation(
                UUID.randomUUID(),
                membershipId,
                type,
                email,
                InvitationToken.hash(rawToken),
                actingUserId,
                requiresRegistration,
                now,
                now.plus(INVITATION_TTL)));
        publish(invitation, rawToken, targetId, targetName, inviterName);
        return invitation;
    }

    /** Re-issues (fresh token/expiry) an invitation for a membership that is still INVITED. */
    public MembershipInvitation reissue(
            MembershipInvitation invitation, UUID targetId, String targetName, String inviterName, Instant now) {
        String rawToken = InvitationToken.generate();
        invitation.reissue(InvitationToken.hash(rawToken), now, now.plus(INVITATION_TTL));
        invitationRepository.save(invitation);
        publish(invitation, rawToken, targetId, targetName, inviterName);
        return invitation;
    }

    private void publish(MembershipInvitation invitation, String rawToken, UUID targetId, String targetName, String inviterName) {
        eventPublisher.publish(
                TeamInvitationCreatedEvent.TYPE,
                new TeamInvitationCreatedEvent(
                        invitation.getId(),
                        invitation.getEmail(),
                        rawToken,
                        targetId,
                        targetName,
                        invitation.getMembershipType().name(),
                        inviterName,
                        invitation.isRequiresRegistration()));
    }
}
