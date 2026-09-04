package com.pantheon.service.service;

import com.pantheon.service.dto.AcceptInvitationResponse;
import com.pantheon.service.dto.InvitationResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.Project;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.exception.InvitationNotFoundException;
import com.pantheon.service.exception.InvitationNotForCurrentUserException;
import com.pantheon.service.exception.ProjectNotFoundException;
import com.pantheon.service.exception.RegistrationNotApplicableException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.repository.ProjectRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drives the token side of the invitation lifecycle: looking up an invitation by its
 * secret token, completing a pre-registration account from it, and accepting it. Acceptance
 * is always a distinct step and requires the caller to be authenticated as the invited
 * account — so a forwarded link cannot add the wrong person, and even an existing account
 * must consent before joining.
 */
@Service
public class InvitationService {

    private final MembershipInvitationRepository invitationRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public InvitationService(
            MembershipInvitationRepository invitationRepository,
            ProjectMembershipRepository membershipRepository,
            AppUserRepository userRepository,
            ProjectRepository projectRepository,
            PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public InvitationResponse getByToken(String rawToken) {
        MembershipInvitation invitation = requireValidInvitation(rawToken);
        Project project = project(invitation);
        String inviterName = userRepository.findById(invitation.getInvitedBy())
                .map(AppUser::getDisplayName)
                .orElse(null);
        return new InvitationResponse(
                project.getName(),
                inviterName,
                invitation.getEmail(),
                invitation.isRequiresRegistration(),
                invitation.isAccepted());
    }

    /**
     * Sets credentials on the pre-registration account this invitation was issued for and
     * marks it active. Does not accept the invitation — the person must still call
     * {@link #accept}.
     */
    @Transactional
    public AppUser completeRegistration(String rawToken, String rawPassword, String displayName) {
        MembershipInvitation invitation = requireValidInvitation(rawToken);
        ProjectMembership membership = membership(invitation);
        AppUser user = userRepository.findById(membership.getUserId())
                .orElseThrow(InvitationNotFoundException::new);

        if (!invitation.isRequiresRegistration() || !user.isPendingRegistration()) {
            throw new RegistrationNotApplicableException();
        }

        user.completeRegistration(passwordEncoder.encode(rawPassword), displayName);
        return userRepository.save(user);
    }

    /**
     * Flips the invited membership to {@code ACTIVE}. The caller must be the invited
     * account. Idempotent when the membership is already active.
     */
    @Transactional
    public AcceptInvitationResponse accept(String rawToken, UUID actingUserId) {
        MembershipInvitation invitation = requireValidInvitation(rawToken);
        ProjectMembership membership = membership(invitation);

        if (!membership.getUserId().equals(actingUserId)) {
            throw new InvitationNotForCurrentUserException();
        }

        membership.accept();
        membershipRepository.save(membership);
        invitation.markAccepted(Instant.now());
        invitationRepository.save(invitation);

        Project project = projectRepository.findById(membership.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(membership.getProjectId()));
        return new AcceptInvitationResponse(project.getId(), project.getName());
    }

    private MembershipInvitation requireValidInvitation(String rawToken) {
        MembershipInvitation invitation = invitationRepository.findByTokenHash(InvitationToken.hash(rawToken))
                .orElseThrow(InvitationNotFoundException::new);
        if (invitation.isExpired(Instant.now())) {
            throw new InvitationNotFoundException();
        }
        return invitation;
    }

    private ProjectMembership membership(MembershipInvitation invitation) {
        return membershipRepository.findById(invitation.getMembershipId())
                .orElseThrow(InvitationNotFoundException::new);
    }

    private Project project(MembershipInvitation invitation) {
        ProjectMembership membership = membership(invitation);
        return projectRepository.findById(membership.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(membership.getProjectId()));
    }
}
