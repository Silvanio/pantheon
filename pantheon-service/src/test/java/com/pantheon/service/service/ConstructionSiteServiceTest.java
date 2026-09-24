package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SiteStatus;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConstructionSiteServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private CompanyMembershipRepository membershipRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PlanService planService;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private PlatformAdminService platformAdminService;

    @Mock
    private SitePermissionService permissionService;

    private ConstructionSiteService service;

    private UUID companyId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        service = new ConstructionSiteService(
                siteRepository, membershipRepository, siteMembershipRepository, companyRepository, planService,
                siteAccessService, scheduleService, platformAdminService, permissionService);
        companyId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        lenient().when(scheduleService.computeProgress(any())).thenReturn(null);
        lenient().when(platformAdminService.isSuperAdmin(any())).thenReturn(false);

        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, adminUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, adminUserId, CompanyRole.ADMIN, Instant.now())));
        lenient().when(siteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteMembershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createEnrollsCreatingAdminAsAdminSiteMember() {
        ConstructionSite site = service.create(
                companyId, adminUserId, new ConstructionSiteRegistrationRequest("Obra", "Endereco", LocalDate.now(), null));

        ArgumentCaptor<SiteMembership> captor = ArgumentCaptor.forClass(SiteMembership.class);
        verify(siteMembershipRepository).save(captor.capture());
        SiteMembership membership = captor.getValue();
        assertThat(membership.getConstructionSiteId()).isEqualTo(site.getId());
        assertThat(membership.getUserId()).isEqualTo(adminUserId);
        assertThat(membership.getFunction()).isEqualTo(ConstructionFunction.ADMIN);
        assertThat(membership.isActive()).isTrue();
    }

    @Test
    void listMineAggregatesActiveSiteMembershipsAcrossCompaniesWithCompanyNames() {
        UUID userId = UUID.randomUUID();
        UUID companyA = UUID.randomUUID();
        UUID companyB = UUID.randomUUID();
        UUID siteA = UUID.randomUUID();
        UUID siteB = UUID.randomUUID();
        UUID inactiveSite = UUID.randomUUID();

        SiteMembership activeOnA = com.pantheon.service.entity.SiteMembership.invited(
                UUID.randomUUID(), siteA, userId, ConstructionFunction.CLIENT, null, null, Instant.now());
        activeOnA.accept();
        SiteMembership activeOnB = com.pantheon.service.entity.SiteMembership.invited(
                UUID.randomUUID(), siteB, userId, ConstructionFunction.ENGINEER, null, null, Instant.now());
        activeOnB.accept();
        SiteMembership stillInvited = com.pantheon.service.entity.SiteMembership.invited(
                UUID.randomUUID(), inactiveSite, userId, ConstructionFunction.ARCHITECT, null, null, Instant.now());

        when(siteMembershipRepository.findByUserId(userId)).thenReturn(java.util.List.of(activeOnA, activeOnB, stillInvited));
        when(siteRepository.findAllById(java.util.List.of(siteA, siteB))).thenReturn(java.util.List.of(
                new ConstructionSite(siteA, companyA, "Obra A", "Endereco A", LocalDate.now(), null, userId, Instant.now()),
                new ConstructionSite(siteB, companyB, "Obra B", "Endereco B", LocalDate.now(), null, userId, Instant.now())));
        when(companyRepository.findAllById(any())).thenReturn(java.util.List.of(
                newCompany(companyA, "Empresa A"), newCompany(companyB, "Empresa B")));

        var result = service.listMine(userId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("companyName").containsExactlyInAnyOrder("Empresa A", "Empresa B");
    }

    @Test
    void getCompanyLogoObjectKeyResolvesThroughTheSitesOwnCompany() {
        UUID siteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConstructionSite site =
                new ConstructionSite(siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, userId, Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        com.pantheon.service.entity.Company company = newCompany(companyId, "Empresa");
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        String logoKey = service.getCompanyLogoObjectKey(siteId, userId);

        assertThat(logoKey).isEqualTo(company.getLogoObjectKey());
        verify(siteAccessService).requireAccess(siteId, userId);
    }

    @Test
    void updateStatusSavesWhenPermittedToManageSiteStatus() {
        UUID siteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConstructionSite site =
                new ConstructionSite(siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, userId, Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        SiteAccessContext access = new SiteAccessContext(false, null);
        when(siteAccessService.requireAccess(siteId, userId)).thenReturn(access);

        ConstructionSite result = service.updateStatus(siteId, userId, SiteStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(SiteStatus.IN_PROGRESS);
        verify(permissionService).requireManage(siteId, access, PermissionCapability.SITE_STATUS);
        verify(siteRepository).save(site);
    }

    @Test
    void updateStatusRejectsWhenNotPermittedToManageSiteStatus() {
        UUID siteId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConstructionSite site =
                new ConstructionSite(siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, userId, Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        SiteAccessContext access = new SiteAccessContext(false, null);
        when(siteAccessService.requireAccess(siteId, userId)).thenReturn(access);
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.SITE_STATUS))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.SITE_STATUS));

        assertThatThrownBy(() -> service.updateStatus(siteId, userId, SiteStatus.IN_PROGRESS))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(siteRepository, never()).save(any());
    }

    private com.pantheon.service.entity.Company newCompany(UUID id, String name) {
        com.pantheon.service.entity.Company company = new com.pantheon.service.entity.Company(id, name, UUID.randomUUID(), Instant.now());
        return company;
    }
}
