package com.pantheon.service.service;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Single entry point for "does this user have access to this construction site". A platform
 * superadmin ({@link PlatformAdminService}) always has access, treated exactly like company
 * staff. Company staff (any {@code CompanyMembership} on the site's company) always have access;
 * otherwise the user needs an {@code ACTIVE} (or accountless {@code NONE}) {@link SiteMembership}
 * on that site. Every site-scoped service should resolve access through this instead of querying
 * membership repositories directly.
 */
@Service
public class SiteAccessService {

    private final ConstructionSiteRepository siteRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final PlatformAdminService platformAdminService;

    public SiteAccessService(
            ConstructionSiteRepository siteRepository,
            CompanyMembershipRepository companyMembershipRepository,
            SiteMembershipRepository siteMembershipRepository,
            PlatformAdminService platformAdminService) {
        this.siteRepository = siteRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.platformAdminService = platformAdminService;
    }

    public ConstructionSite requireSite(UUID constructionSiteId) {
        return siteRepository
                .findById(constructionSiteId)
                .orElseThrow(() -> new ConstructionSiteNotFoundException(constructionSiteId));
    }

    public Optional<SiteAccessContext> resolve(UUID constructionSiteId, UUID userId) {
        ConstructionSite site = requireSite(constructionSiteId);

        if (platformAdminService.isSuperAdmin(userId)) {
            return Optional.of(new SiteAccessContext(true, null));
        }

        boolean isStaff = companyMembershipRepository
                .findByCompanyIdAndUserId(site.getCompanyId(), userId)
                .filter(m -> m.isActive())
                .isPresent();
        if (isStaff) {
            return Optional.of(new SiteAccessContext(true, null));
        }

        Optional<SiteMembership> membership = siteMembershipRepository
                .findByConstructionSiteIdAndUserId(constructionSiteId, userId)
                .filter(SiteMembership::isActive);
        return membership.map(m -> new SiteAccessContext(false, m));
    }

    public SiteAccessContext requireAccess(UUID constructionSiteId, UUID userId) {
        return resolve(constructionSiteId, userId).orElseThrow(() -> new NotSiteMemberException(constructionSiteId));
    }
}
