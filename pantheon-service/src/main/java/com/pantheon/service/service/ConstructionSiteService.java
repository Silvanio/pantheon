package com.pantheon.service.service;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.dto.MySiteResponse;
import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SiteStatus;
import com.pantheon.service.exception.CompanyNotFoundException;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionSiteService {

    private final ConstructionSiteRepository siteRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final CompanyRepository companyRepository;
    private final PlanService planService;
    private final SiteAccessService siteAccessService;
    private final ScheduleService scheduleService;
    private final PlatformAdminService platformAdminService;
    private final SitePermissionService permissionService;

    public ConstructionSiteService(
            ConstructionSiteRepository siteRepository,
            CompanyMembershipRepository membershipRepository,
            SiteMembershipRepository siteMembershipRepository,
            CompanyRepository companyRepository,
            PlanService planService,
            SiteAccessService siteAccessService,
            ScheduleService scheduleService,
            PlatformAdminService platformAdminService,
            SitePermissionService permissionService) {
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.companyRepository = companyRepository;
        this.planService = planService;
        this.siteAccessService = siteAccessService;
        this.scheduleService = scheduleService;
        this.platformAdminService = platformAdminService;
        this.permissionService = permissionService;
    }

    @Transactional
    public ConstructionSite create(UUID companyId, UUID actingUserId, ConstructionSiteRegistrationRequest request) {
        requireAdmin(companyId, actingUserId);
        planService.requireCapacityForNewSite(companyId);

        Instant now = Instant.now();
        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(),
                companyId,
                request.name(),
                request.address(),
                request.startDate(),
                request.expectedEndDate(),
                actingUserId,
                now);
        siteRepository.save(site);

        siteMembershipRepository.save(SiteMembership.admin(UUID.randomUUID(), site.getId(), actingUserId, now));

        return site;
    }

    /**
     * Gated by the {@code SITE_STATUS} {@link PermissionCapability} (configurable per site, like
     * every other capability) rather than company-admin — defaults to {@code MANAGE} for the
     * {@code ADMIN} and {@code ENGINEER} site functions only, see {@code SitePermissionService}.
     */
    @Transactional
    public ConstructionSite updateStatus(UUID siteId, UUID actingUserId, SiteStatus newStatus) {
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireManage(siteId, access, PermissionCapability.SITE_STATUS);

        site.updateStatus(newStatus, Instant.now());
        return siteRepository.save(site);
    }

    @Transactional
    public ConstructionSite updatePhoto(UUID siteId, UUID actingUserId, String photoObjectKey) {
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        requireAdmin(site.getCompanyId(), actingUserId);

        site.updatePhoto(photoObjectKey, Instant.now());
        return siteRepository.save(site);
    }

    public List<ConstructionSite> list(UUID companyId, UUID actingUserId) {
        requireMembership(companyId, actingUserId);
        return siteRepository.findByCompanyId(companyId);
    }

    public ConstructionSite get(UUID siteId, UUID actingUserId) {
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        siteAccessService.requireAccess(siteId, actingUserId);
        return site;
    }

    /**
     * Every obra {@code actingUserId} has active site membership on, across every company —
     * for a site-only member (client, architect, engineer, site foreman, service provider) with
     * no {@code CompanyMembership} of their own, this is the only way to see all their obras at
     * once. See design.md (add-site-only-member-dashboard) decision 1.
     */
    public List<MySiteResponse> listMine(UUID actingUserId) {
        List<UUID> siteIds = siteMembershipRepository.findByUserId(actingUserId).stream()
                .filter(SiteMembership::isActive)
                .map(SiteMembership::getConstructionSiteId)
                .distinct()
                .toList();
        List<ConstructionSite> sites = siteRepository.findAllById(siteIds);

        List<UUID> companyIds = sites.stream().map(ConstructionSite::getCompanyId).distinct().toList();
        Map<UUID, String> companyNamesById = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));

        return sites.stream()
                .map(site -> MySiteResponse.from(
                        site, companyNamesById.get(site.getCompanyId()), scheduleService.computeProgress(site.getId())))
                .toList();
    }

    /** Resolves a site's own company's logo, gated by site access rather than company staff membership. */
    public String getCompanyLogoObjectKey(UUID siteId, UUID actingUserId) {
        ConstructionSite site = get(siteId, actingUserId);
        Company company = companyRepository.findById(site.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(site.getCompanyId()));
        return company.getLogoObjectKey();
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
}
