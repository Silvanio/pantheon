package com.pantheon.service.service;

import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.PlanCode;
import com.pantheon.service.entity.SiteStatus;
import com.pantheon.service.exception.CompanyNotFoundException;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.PlanDowngradeBlockedException;
import com.pantheon.service.exception.SiteLimitExceededException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.PlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The registered plan catalog and a company's plan selection/change. */
@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final CompanyRepository companyRepository;
    private final CompanyMembershipRepository companyMembershipRepository;
    private final ConstructionSiteRepository siteRepository;

    public PlanService(
            PlanRepository planRepository,
            CompanyRepository companyRepository,
            CompanyMembershipRepository companyMembershipRepository,
            ConstructionSiteRepository siteRepository) {
        this.planRepository = planRepository;
        this.companyRepository = companyRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.siteRepository = siteRepository;
    }

    public List<Plan> listPlans() {
        return planRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public Company selectOrChangePlan(UUID companyId, UUID actingUserId, PlanCode planCode) {
        requireAdmin(companyId, actingUserId);
        Company company =
                companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        Plan plan = planRepository.findByCode(planCode).orElseThrow(NoSuchElementException::new);

        if (plan.getActiveSiteLimit() != null) {
            long activeSites = countActiveSites(companyId);
            if (activeSites > plan.getActiveSiteLimit()) {
                throw new PlanDowngradeBlockedException(
                        "Company has " + activeSites + " active sites, which exceeds plan " + planCode + "'s limit");
            }
        }

        company.selectPlan(plan.getId(), Instant.now());
        return companyRepository.save(company);
    }

    /** Sites not COMPLETED count toward the plan's active-site limit. */
    long countActiveSites(UUID companyId) {
        return siteRepository.findByCompanyId(companyId).stream()
                .filter(s -> s.getStatus() != SiteStatus.COMPLETED)
                .count();
    }

    /** Throws if the company has no plan, or its plan's active-site limit is already reached. */
    public void requireCapacityForNewSite(UUID companyId) {
        Company company =
                companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        if (company.getPlanId() == null) {
            throw new SiteLimitExceededException("Company has no plan selected: " + companyId);
        }
        Plan plan = planRepository.findById(company.getPlanId()).orElseThrow(NoSuchElementException::new);
        if (plan.getActiveSiteLimit() != null && countActiveSites(companyId) >= plan.getActiveSiteLimit()) {
            throw new SiteLimitExceededException("Company has reached its plan's active-site limit: " + companyId);
        }
    }

    private void requireAdmin(UUID companyId, UUID userId) {
        CompanyMembership membership = companyMembershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyAdminException(companyId));
        if (membership.getRole() != CompanyRole.ADMIN) {
            throw new NotCompanyAdminException(companyId);
        }
    }
}
