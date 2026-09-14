package com.pantheon.service.service;

import com.pantheon.service.dto.EmailConflictCheck;
import com.pantheon.service.dto.PersonSearchResult;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Looks up people already known to a company — its staff, and every construction site's team
 * members, account-holding or not — so an admin adding a new team member can reuse their data
 * instead of retyping it. Deliberately scoped to one company: CPF and contact details are
 * sensitive, and a cross-company lookup would leak one company's people to another. See
 * design.md decisions 1-2.
 */
@Service
public class PersonSearchService {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULTS = 8;

    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final AppUserRepository userRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public PersonSearchService(
            ConstructionSiteRepository siteRepository, SiteMembershipRepository siteMembershipRepository,
            CompanyMembershipRepository companyMembershipRepository, AppUserRepository userRepository,
            SiteAccessService siteAccessService, SitePermissionService permissionService) {
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.userRepository = userRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public List<PersonSearchResult> search(UUID constructionSiteId, UUID actingUserId, String query) {
        var access = siteAccessService.requireAccess(constructionSiteId, actingUserId);
        permissionService.requireManage(constructionSiteId, access, PermissionCapability.TEAM_MANAGE);

        if (query == null || query.trim().length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        String digitsQuery = query.replaceAll("\\D", "");

        ConstructionSite site = siteAccessService.requireSite(constructionSiteId);
        List<UUID> companySiteIds = siteRepository.findByCompanyId(site.getCompanyId()).stream()
                .map(ConstructionSite::getId)
                .toList();
        List<SiteMembership> companyMemberships = siteMembershipRepository.findByConstructionSiteIdIn(companySiteIds);

        Set<UUID> staffUserIds = companyMembershipRepository.findByCompanyId(site.getCompanyId()).stream()
                .map(CompanyMembership::getUserId)
                .collect(Collectors.toCollection(HashSet::new));
        companyMemberships.stream().map(SiteMembership::getUserId).filter(id -> id != null).forEach(staffUserIds::add);
        Map<UUID, AppUser> usersById = new HashMap<>();
        userRepository.findAllById(staffUserIds).forEach(u -> usersById.put(u.getId(), u));

        Map<UUID, PersonSearchResult> byUserId = new HashMap<>();
        List<PersonSearchResult> accountless = new ArrayList<>();

        for (SiteMembership membership : companyMemberships) {
            if (membership.getUserId() != null) {
                AppUser user = usersById.get(membership.getUserId());
                if (user == null) {
                    continue;
                }
                // A person can have a membership per obra; keep the first one that actually carries
                // cpf/phone, so those aren't lost just because an earlier membership omitted them.
                byUserId.merge(
                        user.getId(),
                        new PersonSearchResult(user.getDisplayName(), user.getEmail(), membership.getCpf(), membership.getPhone()),
                        (a, b) -> a.cpf() != null || a.phone() != null ? a : b);
            } else {
                accountless.add(new PersonSearchResult(
                        membership.getDisplayName(), membership.getContactEmail(), membership.getCpf(), membership.getPhone()));
            }
        }
        for (UUID staffUserId : staffUserIds) {
            AppUser user = usersById.get(staffUserId);
            if (user != null) {
                byUserId.putIfAbsent(user.getId(), new PersonSearchResult(user.getDisplayName(), user.getEmail(), null, null));
            }
        }

        List<PersonSearchResult> candidates = new ArrayList<>(byUserId.values());
        candidates.addAll(accountless);

        return candidates.stream()
                .filter(p -> matches(p, normalizedQuery, digitsQuery))
                .distinct()
                .limit(MAX_RESULTS)
                .toList();
    }

    /**
     * Whether {@code email} should block adding it to {@code constructionSiteId}: already an
     * active member there, or already tied to a person with a membership in a different company.
     * See design.md (block-cross-company-and-duplicate-site-members) decisions 1-4.
     */
    public EmailConflictCheck checkEmailConflicts(UUID constructionSiteId, UUID actingUserId, String email) {
        var access = siteAccessService.requireAccess(constructionSiteId, actingUserId);
        permissionService.requireManage(constructionSiteId, access, PermissionCapability.TEAM_MANAGE);

        if (email == null || email.isBlank()) {
            return new EmailConflictCheck(false, false);
        }
        AppUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new EmailConflictCheck(false, false);
        }

        ConstructionSite site = siteAccessService.requireSite(constructionSiteId);
        boolean activeOnThisSite = siteMembershipRepository
                .findByConstructionSiteIdAndUserId(constructionSiteId, user.getId())
                .map(SiteMembership::isActive)
                .orElse(false);

        return new EmailConflictCheck(activeOnThisSite, belongsToAnotherCompany(user.getId(), site.getCompanyId()));
    }

    /** True if {@code userId} has a company-staff or any-obra membership in a company other than {@code companyId}. */
    boolean belongsToAnotherCompany(UUID userId, UUID companyId) {
        boolean viaCompanyStaff = companyMembershipRepository.findByUserId(userId).stream()
                .anyMatch(m -> !m.getCompanyId().equals(companyId));
        if (viaCompanyStaff) {
            return true;
        }

        List<SiteMembership> userSiteMemberships = siteMembershipRepository.findByUserId(userId);
        List<UUID> siteIds = userSiteMemberships.stream().map(SiteMembership::getConstructionSiteId).distinct().toList();
        Map<UUID, UUID> companyIdBySiteId = siteRepository.findAllById(siteIds).stream()
                .collect(Collectors.toMap(ConstructionSite::getId, ConstructionSite::getCompanyId));
        return userSiteMemberships.stream()
                .map(m -> companyIdBySiteId.get(m.getConstructionSiteId()))
                .anyMatch(cid -> cid != null && !cid.equals(companyId));
    }

    private boolean matches(PersonSearchResult person, String normalizedQuery, String digitsQuery) {
        boolean cpfMatch = !digitsQuery.isEmpty() && person.cpf() != null
                && person.cpf().replaceAll("\\D", "").startsWith(digitsQuery);
        boolean emailMatch = person.email() != null && person.email().toLowerCase(Locale.ROOT).startsWith(normalizedQuery);
        boolean nameMatch = person.name() != null && person.name().toLowerCase(Locale.ROOT).contains(normalizedQuery);
        return cpfMatch || emailMatch || nameMatch;
    }
}
