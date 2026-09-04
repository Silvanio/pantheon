package com.pantheon.service.service;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteStatus;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionSiteService {

    private final ConstructionSiteRepository siteRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final PlanService planService;

    public ConstructionSiteService(
            ConstructionSiteRepository siteRepository,
            CompanyMembershipRepository membershipRepository,
            PlanService planService) {
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
        this.planService = planService;
    }

    @Transactional
    public ConstructionSite create(UUID companyId, UUID actingUserId, ConstructionSiteRegistrationRequest request) {
        requireAdmin(companyId, actingUserId);
        planService.requireCapacityForNewSite(companyId);

        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(),
                companyId,
                request.name(),
                request.address(),
                request.startDate(),
                request.expectedEndDate(),
                actingUserId,
                Instant.now());
        return siteRepository.save(site);
    }

    @Transactional
    public ConstructionSite updateStatus(UUID siteId, UUID actingUserId, SiteStatus newStatus) {
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        requireAdmin(site.getCompanyId(), actingUserId);

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

    private void requireAdmin(UUID companyId, UUID userId) {
        CompanyMembership membership = membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyAdminException(companyId));
        if (membership.getRole() != CompanyRole.ADMIN) {
            throw new NotCompanyAdminException(companyId);
        }
    }

    private void requireMembership(UUID companyId, UUID userId) {
        membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyMemberException(companyId));
    }
}
