package com.pantheon.service.service;

import com.pantheon.service.dto.CompanyMemberResponse;
import com.pantheon.service.dto.CompanyMembershipResponse;
import com.pantheon.service.dto.OnboardingStatusResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.CompanyNotFoundException;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company creation, its staff (a company's internal team, distinct from a construction site's
 * own team — see {@code SiteMembershipService}), and onboarding-status reporting.
 */
@Service
public class CompanyService {

    private static final String DEFAULT_LABEL_NAME = "Urgente";
    private static final String DEFAULT_LABEL_COLOR = "#EF4444";

    private final CompanyRepository companyRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final MembershipInvitationRepository invitationRepository;
    private final MembershipInvitationIssuer invitationIssuer;
    private final SiteMembershipRepository siteMembershipRepository;
    private final TaskLabelRepository taskLabelRepository;
    private final PlatformAdminService platformAdminService;

    public CompanyService(
            CompanyRepository companyRepository,
            CompanyMembershipRepository membershipRepository,
            AppUserRepository userRepository,
            MembershipInvitationRepository invitationRepository,
            MembershipInvitationIssuer invitationIssuer,
            SiteMembershipRepository siteMembershipRepository,
            TaskLabelRepository taskLabelRepository,
            PlatformAdminService platformAdminService) {
        this.companyRepository = companyRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.invitationIssuer = invitationIssuer;
        this.siteMembershipRepository = siteMembershipRepository;
        this.taskLabelRepository = taskLabelRepository;
        this.platformAdminService = platformAdminService;
    }

    @Transactional
    public Company create(UUID creatorId, String companyName) {
        Instant now = Instant.now();
        Company company = new Company(UUID.randomUUID(), companyName, creatorId, now);
        companyRepository.save(company);

        membershipRepository.save(new CompanyMembership(UUID.randomUUID(), company.getId(), creatorId, CompanyRole.ADMIN, now));

        taskLabelRepository.save(
                TaskLabel.predefined(UUID.randomUUID(), company.getId(), DEFAULT_LABEL_NAME, DEFAULT_LABEL_COLOR, now));

        return company;
    }

    @Transactional
    public Company completeProfile(
            UUID companyId, UUID actingUserId, String legalName, String tradeName, String cnpj, String address,
            String logoObjectKey) {
        requireAdmin(companyId, actingUserId);
        Company company =
                companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        String resolvedLogoKey = logoObjectKey != null ? logoObjectKey : company.getLogoObjectKey();
        company.completeProfile(legalName, tradeName, cnpj, address, resolvedLogoKey, Instant.now());
        return companyRepository.save(company);
    }

    public Company get(UUID companyId, UUID actingUserId) {
        requireMembership(companyId, actingUserId);
        return companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    /**
     * Adds a company staff member by email as an invitation, following the same lifecycle as
     * a construction site's team invites (see {@code SiteMembershipService}).
     */
    @Transactional
    public MembershipInvitation addStaffMember(UUID companyId, UUID actingUserId, String memberEmail) {
        requireAdmin(companyId, actingUserId);
        Company company =
                companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        AppUser inviter =
                userRepository.findById(actingUserId).orElseThrow(() -> new NotCompanyAdminException(companyId));

        Instant now = Instant.now();
        var resolved = invitationIssuer.resolveOrCreateUser(memberEmail, now);

        Optional<CompanyMembership> existing =
                membershipRepository.findByCompanyIdAndUserId(companyId, resolved.user().getId());
        if (existing.isPresent()) {
            CompanyMembership membership = existing.get();
            if (membership.isActive()) {
                throw new MemberAlreadyActiveException(memberEmail);
            }
            MembershipInvitation existingInvitation = invitationRepository
                    .findByMembershipId(membership.getId())
                    .orElseThrow(() -> new IllegalStateException("INVITED membership without an invitation"));
            return invitationIssuer.reissue(
                    existingInvitation, company.getId(), company.getName(), inviter.getDisplayName(), now);
        }

        CompanyMembership membership = membershipRepository.save(
                CompanyMembership.invited(UUID.randomUUID(), companyId, resolved.user().getId(), now));

        return invitationIssuer.issue(
                MembershipType.COMPANY, membership.getId(), memberEmail, actingUserId, resolved.preRegistered(),
                company.getId(), company.getName(), inviter.getDisplayName(), now);
    }

    public List<CompanyMemberResponse> listStaff(UUID companyId, UUID actingUserId) {
        requireMembership(companyId, actingUserId);

        List<CompanyMembership> memberships = membershipRepository.findByCompanyId(companyId);
        Map<UUID, AppUser> usersById = userRepository
                .findAllById(memberships.stream().map(CompanyMembership::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(AppUser::getId, u -> u));

        return memberships.stream()
                .map(m -> {
                    AppUser user = usersById.get(m.getUserId());
                    return new CompanyMemberResponse(
                            m.getId(), user.getId(), user.getEmail(), user.getDisplayName(), m.getRole(),
                            m.getStatus(), !m.isActive());
                })
                .toList();
    }

    public OnboardingStatusResponse getOnboardingStatus(UUID userId) {
        List<CompanyMembership> memberships = membershipRepository.findByUserId(userId).stream()
                .filter(CompanyMembership::isActive)
                .toList();
        List<UUID> siteIds = siteMembershipRepository.findByUserId(userId).stream()
                .filter(SiteMembership::isActive)
                .map(SiteMembership::getConstructionSiteId)
                .toList();
        if (memberships.isEmpty()) {
            return new OnboardingStatusResponse(false, List.of(), siteIds);
        }

        Map<UUID, Company> companiesById = companiesById(memberships);
        List<CompanyMembershipResponse> companies = memberships.stream()
                .map(m -> {
                    Company company = companiesById.get(m.getCompanyId());
                    return new CompanyMembershipResponse(
                            company.getId(), company.getName(), m.getRole(), company.getOnboardingStatus());
                })
                .toList();
        return new OnboardingStatusResponse(true, companies, siteIds);
    }

    public List<CompanyMembershipResponse> listMyCompanies(UUID userId) {
        return getOnboardingStatus(userId).companies();
    }

    /** System-wide, paginated, optionally name-filtered company listing — superadmin only. */
    public Page<Company> listAllCompanies(UUID actingUserId, String search, Pageable pageable) {
        platformAdminService.requireSuperAdmin(actingUserId);
        if (search != null && !search.isBlank()) {
            return companyRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        }
        return companyRepository.findAll(pageable);
    }

    private void requireAdmin(UUID companyId, UUID userId) {
        if (platformAdminService.isSuperAdmin(userId)) {
            return;
        }
        CompanyMembership membership = membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyAdminException(companyId));
        if (membership.getRole() != CompanyRole.ADMIN) {
            throw new NotCompanyAdminException(companyId);
        }
    }

    private void requireMembership(UUID companyId, UUID userId) {
        if (platformAdminService.isSuperAdmin(userId)) {
            return;
        }
        membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyMemberException(companyId));
    }

    private Map<UUID, Company> companiesById(List<CompanyMembership> memberships) {
        List<UUID> companyIds = memberships.stream().map(CompanyMembership::getCompanyId).toList();
        return companyRepository.findAllById(companyIds).stream().collect(Collectors.toMap(Company::getId, c -> c));
    }
}
