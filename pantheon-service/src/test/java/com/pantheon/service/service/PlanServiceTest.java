package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.PlanCode;
import com.pantheon.service.entity.SiteStatus;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.PlanDowngradeBlockedException;
import com.pantheon.service.exception.SiteLimitExceededException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.PlanRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMembershipRepository companyMembershipRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    private PlanService service;

    @BeforeEach
    void setUp() {
        service = new PlanService(planRepository, companyRepository, companyMembershipRepository, siteRepository);
        lenient().when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Plan plan(PlanCode code, Integer limit) {
        Plan plan;
        try {
            var constructor = Plan.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            plan = constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(plan, "code", code);
        ReflectionTestUtils.setField(plan, "name", code.name());
        ReflectionTestUtils.setField(plan, "activeSiteLimit", limit);
        return plan;
    }

    private static ConstructionSite siteWithStatus(UUID companyId, SiteStatus status) {
        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(), companyId, "Site", "Addr", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
        site.updateStatus(status, Instant.now());
        return site;
    }

    @Test
    void selectOrChangePlanRequiresAdmin() {
        UUID companyId = UUID.randomUUID();
        UUID nonAdminId = UUID.randomUUID();
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, nonAdminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.selectOrChangePlan(companyId, nonAdminId, PlanCode.BASIC))
                .isInstanceOf(NotCompanyAdminException.class);
    }

    @Test
    void selectOrChangePlanSetsChosenPlan() {
        UUID companyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyMembership admin = new CompanyMembership(UUID.randomUUID(), companyId, adminId, CompanyRole.ADMIN, now);
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, adminId)).thenReturn(Optional.of(admin));
        Company company = new Company(companyId, "Construtora", adminId, now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        Plan basic = plan(PlanCode.BASIC, 2);
        when(planRepository.findByCode(PlanCode.BASIC)).thenReturn(Optional.of(basic));
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of());

        Company result = service.selectOrChangePlan(companyId, adminId, PlanCode.BASIC);

        assertThat(result.getPlanId()).isEqualTo(basic.getId());
    }

    @Test
    void selectOrChangePlanBlocksDowngradeBelowActiveSiteCount() {
        UUID companyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyMembership admin = new CompanyMembership(UUID.randomUUID(), companyId, adminId, CompanyRole.ADMIN, now);
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, adminId)).thenReturn(Optional.of(admin));
        Company company = new Company(companyId, "Construtora", adminId, now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        Plan basic = plan(PlanCode.BASIC, 2);
        when(planRepository.findByCode(PlanCode.BASIC)).thenReturn(Optional.of(basic));

        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(
                siteWithStatus(companyId, SiteStatus.IN_PROGRESS),
                siteWithStatus(companyId, SiteStatus.IN_PROGRESS),
                siteWithStatus(companyId, SiteStatus.IN_PROGRESS)));

        assertThatThrownBy(() -> service.selectOrChangePlan(companyId, adminId, PlanCode.BASIC))
                .isInstanceOf(PlanDowngradeBlockedException.class);
    }

    @Test
    void selectOrChangePlanAllowsUnlimitedRegardlessOfSiteCount() {
        UUID companyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyMembership admin = new CompanyMembership(UUID.randomUUID(), companyId, adminId, CompanyRole.ADMIN, now);
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, adminId)).thenReturn(Optional.of(admin));
        Company company = new Company(companyId, "Construtora", adminId, now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        Plan unlimited = plan(PlanCode.ILIMITADO, null);
        when(planRepository.findByCode(PlanCode.ILIMITADO)).thenReturn(Optional.of(unlimited));

        Company result = service.selectOrChangePlan(companyId, adminId, PlanCode.ILIMITADO);

        assertThat(result.getPlanId()).isEqualTo(unlimited.getId());
    }

    @Test
    void requireCapacityForNewSiteBlocksWhenNoPlanSelected() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "Construtora", UUID.randomUUID(), Instant.now());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service.requireCapacityForNewSite(companyId))
                .isInstanceOf(SiteLimitExceededException.class);
    }

    @Test
    void requireCapacityForNewSiteBlocksAtLimit() {
        UUID companyId = UUID.randomUUID();
        Instant now = Instant.now();
        Company company = new Company(companyId, "Construtora", UUID.randomUUID(), now);
        Plan basic = plan(PlanCode.BASIC, 2);
        company.selectPlan(basic.getId(), now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(planRepository.findById(basic.getId())).thenReturn(Optional.of(basic));
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(
                siteWithStatus(companyId, SiteStatus.PLANNING), siteWithStatus(companyId, SiteStatus.PLANNING)));

        assertThatThrownBy(() -> service.requireCapacityForNewSite(companyId))
                .isInstanceOf(SiteLimitExceededException.class);
    }

    @Test
    void requireCapacityForNewSiteAllowsUnderLimit() {
        UUID companyId = UUID.randomUUID();
        Instant now = Instant.now();
        Company company = new Company(companyId, "Construtora", UUID.randomUUID(), now);
        Plan basic = plan(PlanCode.BASIC, 2);
        company.selectPlan(basic.getId(), now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(planRepository.findById(basic.getId())).thenReturn(Optional.of(basic));
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(siteWithStatus(companyId, SiteStatus.PLANNING)));

        service.requireCapacityForNewSite(companyId);
    }

    @Test
    void completedSitesDoNotCountTowardTheLimit() {
        UUID companyId = UUID.randomUUID();
        Instant now = Instant.now();
        Company company = new Company(companyId, "Construtora", UUID.randomUUID(), now);
        Plan basic = plan(PlanCode.BASIC, 1);
        company.selectPlan(basic.getId(), now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(planRepository.findById(basic.getId())).thenReturn(Optional.of(basic));
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(siteWithStatus(companyId, SiteStatus.COMPLETED)));

        service.requireCapacityForNewSite(companyId);
    }
}
