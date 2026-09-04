package com.pantheon.service.service;

import com.pantheon.service.dto.SiteMemberResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A construction site's own team: clients, architects, engineers, and site foremen (invite-
 * driven, like company staff) plus service providers (accountless by default). Distinct from
 * {@code CompanyService}'s staff list — see design.md decision 3.
 */
@Service
public class SiteMembershipService {

    private final SiteMembershipRepository membershipRepository;
    private final MembershipInvitationRepository invitationRepository;
    private final AppUserRepository userRepository;
    private final MembershipInvitationIssuer invitationIssuer;
    private final SiteAccessService siteAccessService;

    public SiteMembershipService(
            SiteMembershipRepository membershipRepository,
            MembershipInvitationRepository invitationRepository,
            AppUserRepository userRepository,
            MembershipInvitationIssuer invitationIssuer,
            SiteAccessService siteAccessService) {
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.invitationIssuer = invitationIssuer;
        this.siteAccessService = siteAccessService;
    }

    @Transactional
    public SiteMembership addAccountlessServiceProvider(
            UUID constructionSiteId, UUID actingUserId, String displayName, String trade, String contactEmail) {
        requireCompanyStaff(constructionSiteId, actingUserId);
        SiteMembership membership = SiteMembership.accountless(
                UUID.randomUUID(), constructionSiteId, trade, displayName, contactEmail, Instant.now());
        return membershipRepository.save(membership);
    }

    @Transactional
    public MembershipInvitation inviteMember(
            UUID constructionSiteId, UUID actingUserId, ConstructionFunction function, String email, String cpf) {
        requireCompanyStaff(constructionSiteId, actingUserId);
        ConstructionSite site = siteAccessService.requireSite(constructionSiteId);
        AppUser inviter = userRepository.findById(actingUserId).orElseThrow(() -> new NotSiteMemberException(constructionSiteId));

        Instant now = Instant.now();
        var resolved = invitationIssuer.resolveOrCreateUser(email, now);

        Optional<SiteMembership> existing =
                membershipRepository.findByConstructionSiteIdAndUserId(constructionSiteId, resolved.user().getId());
        if (existing.isPresent()) {
            SiteMembership membership = existing.get();
            if (membership.isActive()) {
                throw new MemberAlreadyActiveException(email);
            }
            MembershipInvitation existingInvitation = invitationRepository
                    .findByMembershipId(membership.getId())
                    .orElseThrow(() -> new IllegalStateException("INVITED membership without an invitation"));
            return invitationIssuer.reissue(existingInvitation, site.getId(), site.getName(), inviter.getDisplayName(), now);
        }

        SiteMembership membership = membershipRepository.save(
                SiteMembership.invited(UUID.randomUUID(), constructionSiteId, resolved.user().getId(), function, cpf, now));

        return invitationIssuer.issue(
                MembershipType.SITE, membership.getId(), email, actingUserId, resolved.preRegistered(),
                site.getId(), site.getName(), inviter.getDisplayName(), now);
    }

    /** Attaches a real account to a previously accountless service-provider membership, starting its invite. */
    @Transactional
    public MembershipInvitation attachAccount(UUID membershipId, UUID actingUserId, String email) {
        SiteMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new NotSiteMemberException(membershipId));
        requireCompanyStaff(membership.getConstructionSiteId(), actingUserId);
        ConstructionSite site = siteAccessService.requireSite(membership.getConstructionSiteId());
        AppUser inviter = userRepository.findById(actingUserId)
                .orElseThrow(() -> new NotSiteMemberException(membership.getConstructionSiteId()));

        Instant now = Instant.now();
        var resolved = invitationIssuer.resolveOrCreateUser(email, now);
        membership.attachAccount(resolved.user().getId());
        membershipRepository.save(membership);

        return invitationIssuer.issue(
                MembershipType.SITE, membership.getId(), email, actingUserId, resolved.preRegistered(), site.getId(),
                site.getName(), inviter.getDisplayName(), now);
    }

    public List<SiteMemberResponse> listMembers(UUID constructionSiteId, UUID actingUserId) {
        siteAccessService.requireAccess(constructionSiteId, actingUserId);

        List<SiteMembership> memberships = membershipRepository.findByConstructionSiteId(constructionSiteId);
        List<UUID> userIds = memberships.stream().map(SiteMembership::getUserId).filter(id -> id != null).toList();
        Map<UUID, AppUser> usersById =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(AppUser::getId, u -> u));

        return memberships.stream()
                .map(m -> {
                    AppUser user = m.getUserId() != null ? usersById.get(m.getUserId()) : null;
                    return new SiteMemberResponse(
                            m.getId(),
                            m.getUserId(),
                            user != null ? user.getEmail() : m.getContactEmail(),
                            user != null ? user.getDisplayName() : m.getDisplayName(),
                            m.getFunction(),
                            m.getServiceProviderTrade(),
                            m.getClientCpf(),
                            m.getStatus(),
                            !m.isActive());
                })
                .toList();
    }

    private void requireCompanyStaff(UUID constructionSiteId, UUID userId) {
        SiteAccessContext access = siteAccessService.requireAccess(constructionSiteId, userId);
        if (!access.companyStaff()) {
            throw new NotSiteMemberException(constructionSiteId);
        }
    }
}
