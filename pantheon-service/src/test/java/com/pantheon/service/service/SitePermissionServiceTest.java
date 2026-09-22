package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SitePermissionOverride;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.SitePermissionOverrideRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SitePermissionServiceTest {

    @Mock
    private SitePermissionOverrideRepository overrideRepository;

    private SitePermissionService service;

    private UUID siteId;
    private SiteMembership clientMembership;
    private SiteAccessContext clientAccess;

    @BeforeEach
    void setUp() {
        service = new SitePermissionService(overrideRepository);
        siteId = UUID.randomUUID();
        clientMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, UUID.randomUUID(), ConstructionFunction.CLIENT, null, null, Instant.now());
        clientMembership.accept();
        clientAccess = new SiteAccessContext(false, clientMembership);
    }

    @Test
    void companyStaffAlwaysResolveToManage() {
        SiteAccessContext staffAccess = new SiteAccessContext(true, null);

        AccessLevel level = service.resolve(siteId, staffAccess, PermissionCapability.ORCAMENTO_MANAGE);

        assertThat(level).isEqualTo(AccessLevel.MANAGE);
    }

    @Test
    void fallsBackToHardcodedDefaultWhenNoOverridesExist() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());

        AccessLevel level = service.resolve(siteId, clientAccess, PermissionCapability.DAILY_REPORT);

        assertThat(level).isEqualTo(AccessLevel.VIEW);
    }

    @Test
    void functionOverrideWinsOverDefault() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.of(SitePermissionOverride.forFunction(
                        UUID.randomUUID(), siteId, ConstructionFunction.CLIENT, PermissionCapability.DAILY_REPORT,
                        AccessLevel.MANAGE)));

        AccessLevel level = service.resolve(siteId, clientAccess, PermissionCapability.DAILY_REPORT);

        assertThat(level).isEqualTo(AccessLevel.MANAGE);
    }

    @Test
    void memberOverrideWinsOverFunctionOverrideAndDefault() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.of(SitePermissionOverride.forMember(
                        UUID.randomUUID(), siteId, clientMembership.getId(), PermissionCapability.DAILY_REPORT,
                        AccessLevel.MANAGE)));

        AccessLevel level = service.resolve(siteId, clientAccess, PermissionCapability.DAILY_REPORT);

        assertThat(level).isEqualTo(AccessLevel.MANAGE);
    }

    @Test
    void requireManageThrowsWhenResolvedAccessIsViewOnly() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireManage(siteId, clientAccess, PermissionCapability.DAILY_REPORT))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void clientDefaultsToViewAndApproveOnPurchaseRequest() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.empty());

        AccessLevel level = service.resolve(siteId, clientAccess, PermissionCapability.PURCHASE_REQUEST);

        assertThat(level).isEqualTo(AccessLevel.VIEW_AND_APPROVE);
    }

    @Test
    void requireManageThrowsWhenResolvedAccessIsViewAndApprove() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireManage(siteId, clientAccess, PermissionCapability.PURCHASE_REQUEST))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void viewAndApproveOverrideRoundTripsThroughResolve() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.of(SitePermissionOverride.forMember(
                        UUID.randomUUID(), siteId, clientMembership.getId(), PermissionCapability.PURCHASE_REQUEST,
                        AccessLevel.VIEW_AND_APPROVE)));

        AccessLevel level = service.resolve(siteId, clientAccess, PermissionCapability.PURCHASE_REQUEST);

        assertThat(level).isEqualTo(AccessLevel.VIEW_AND_APPROVE);
    }

    @Test
    void engineerCanManageOrcamentoByDefault() {
        SiteMembership engineer = SiteMembership.invited(
                UUID.randomUUID(), siteId, UUID.randomUUID(), ConstructionFunction.ENGINEER, null, null, Instant.now());
        engineer.accept();
        SiteAccessContext engineerAccess = new SiteAccessContext(false, engineer);
        when(overrideRepository.findBySiteMembershipIdAndCapability(engineer.getId(), PermissionCapability.ORCAMENTO_MANAGE))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.ENGINEER, PermissionCapability.ORCAMENTO_MANAGE))
                .thenReturn(Optional.empty());

        service.requireManage(siteId, engineerAccess, PermissionCapability.ORCAMENTO_MANAGE);
    }

    @Test
    void requireApprovePassesForViewAndApprove() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.PURCHASE_REQUEST))
                .thenReturn(Optional.empty());

        service.requireApprove(siteId, clientAccess, PermissionCapability.PURCHASE_REQUEST);
    }

    @Test
    void requireApproveThrowsForViewOnly() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.DAILY_REPORT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireApprove(siteId, clientAccess, PermissionCapability.DAILY_REPORT))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void requireVisibleThrowsWhenResolvedAccessIsHidden() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.TASKS))
                .thenReturn(Optional.of(SitePermissionOverride.forMember(
                        UUID.randomUUID(), siteId, clientMembership.getId(), PermissionCapability.TASKS, AccessLevel.HIDDEN)));

        assertThatThrownBy(() -> service.requireVisible(siteId, clientAccess, PermissionCapability.TASKS))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void requireVisiblePassesForViewAndManage() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.TASKS))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.TASKS))
                .thenReturn(Optional.empty());

        service.requireVisible(siteId, clientAccess, PermissionCapability.TASKS);
    }

    @Test
    void requireVisibleNeverThrowsForCompanyStaff() {
        SiteAccessContext staffAccess = new SiteAccessContext(true, null);

        service.requireVisible(siteId, staffAccess, PermissionCapability.TASKS);
    }

    @Test
    void siteForemanCanManageScheduleByDefault() {
        SiteMembership foreman = SiteMembership.invited(
                UUID.randomUUID(), siteId, UUID.randomUUID(), ConstructionFunction.SITE_FOREMAN, null, null, Instant.now());
        foreman.accept();
        SiteAccessContext foremanAccess = new SiteAccessContext(false, foreman);
        when(overrideRepository.findBySiteMembershipIdAndCapability(foreman.getId(), PermissionCapability.SCHEDULE))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.SITE_FOREMAN, PermissionCapability.SCHEDULE))
                .thenReturn(Optional.empty());

        service.requireManage(siteId, foremanAccess, PermissionCapability.SCHEDULE);
    }

    @Test
    void clientDefaultsToViewOnSchedule() {
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.SCHEDULE))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.SCHEDULE))
                .thenReturn(Optional.empty());

        AccessLevel level = service.resolve(siteId, clientAccess, PermissionCapability.SCHEDULE);

        assertThat(level).isEqualTo(AccessLevel.VIEW);
    }

    @Test
    void resolveAllReturnsEveryCapability() {
        SiteAccessContext staffAccess = new SiteAccessContext(true, null);

        var resolved = service.resolveAll(siteId, staffAccess);

        assertThat(resolved).hasSize(PermissionCapability.values().length);
        assertThat(resolved.values()).allMatch(level -> level == AccessLevel.MANAGE);
    }
}
