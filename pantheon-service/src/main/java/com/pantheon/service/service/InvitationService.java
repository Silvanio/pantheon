package com.pantheon.service.service;

import com.pantheon.service.dto.AcceptInvitationResponse;
import com.pantheon.service.dto.InvitationResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.CompanyNotFoundException;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.InvitationNotFoundException;
import com.pantheon.service.exception.InvitationNotForCurrentUserException;
import com.pantheon.service.exception.RegistrationNotApplicableException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Drives the token side of the invitation lifecycle for both company-staff and construction-
 * site-team invitations, branching on {@link MembershipInvitation#getMembershipType()}.
 * Acceptance is always a distinct step and requires the caller to be authenticated as the
 * invited account — so a forwarded link cannot add the wrong person, and even an existing
 * account must consent before joining.
 */
@Service
public class InvitationService {

    private final MembershipInvitationRepository invitationRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final AppUserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final ConstructionSiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;

    public InvitationService(
            MembershipInvitationRepository invitationRepository,
            CompanyMembershipRepository companyMembershipRepository,
            SiteMembershipRepository siteMembershipRepository,
            AppUserRepository userRepository,
            CompanyRepository companyRepository,
            ConstructionSiteRepository siteRepository,
            PasswordEncoder passwordEncoder) {
        this.invitationRepository = invitationRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.siteRepository = siteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public InvitationResponse getByToken(String rawToken) {
        MembershipInvitation invitation = requireValidInvitation(rawToken);
        String targetName = targetName(invitation);
        String inviterName = userRepository.findById(invitation.getInvitedBy())
                .map(AppUser::getDisplayName)
                .orElse(null);
        return new InvitationResponse(
                targetName,
                invitation.getMembershipType().name(),
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
        UUID targetUserId = membershipUserId(invitation);
        AppUser user = userRepository.findById(targetUserId).orElseThrow(InvitationNotFoundException::new);

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
        UUID targetUserId = membershipUserId(invitation);

        if (!targetUserId.equals(actingUserId)) {
            throw new InvitationNotForCurrentUserException();
        }

        if (invitation.getMembershipType() == MembershipType.COMPANY) {
            CompanyMembership membership = companyMembershipRepository
                    .findById(invitation.getMembershipId())
                    .orElseThrow(InvitationNotFoundException::new);
            membership.accept();
            companyMembershipRepository.save(membership);
        } else {
            SiteMembership membership = siteMembershipRepository
                    .findById(invitation.getMembershipId())
                    .orElseThrow(InvitationNotFoundException::new);
            membership.accept();
            siteMembershipRepository.save(membership);
        }

        invitation.markAccepted(Instant.now());
        invitationRepository.save(invitation);

        return new AcceptInvitationResponse(targetId(invitation), targetName(invitation), invitation.getMembershipType().name());
    }

    private MembershipInvitation requireValidInvitation(String rawToken) {
        MembershipInvitation invitation = invitationRepository.findByTokenHash(InvitationToken.hash(rawToken))
                .orElseThrow(InvitationNotFoundException::new);
        if (invitation.isExpired(Instant.now())) {
            throw new InvitationNotFoundException();
        }
        return invitation;
    }

    private UUID membershipUserId(MembershipInvitation invitation) {
        if (invitation.getMembershipType() == MembershipType.COMPANY) {
            return companyMembershipRepository
                    .findById(invitation.getMembershipId())
                    .map(CompanyMembership::getUserId)
                    .orElseThrow(InvitationNotFoundException::new);
        }
        return siteMembershipRepository
                .findById(invitation.getMembershipId())
                .map(SiteMembership::getUserId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private UUID targetId(MembershipInvitation invitation) {
        if (invitation.getMembershipType() == MembershipType.COMPANY) {
            return companyMembershipRepository
                    .findById(invitation.getMembershipId())
                    .map(CompanyMembership::getCompanyId)
                    .orElseThrow(InvitationNotFoundException::new);
        }
        return siteMembershipRepository
                .findById(invitation.getMembershipId())
                .map(SiteMembership::getConstructionSiteId)
                .orElseThrow(InvitationNotFoundException::new);
    }

    private String targetName(MembershipInvitation invitation) {
        if (invitation.getMembershipType() == MembershipType.COMPANY) {
            UUID companyId = targetId(invitation);
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new CompanyNotFoundException(companyId));
            return company.getName();
        }
        UUID siteId = targetId(invitation);
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        return site.getName();
    }
}
