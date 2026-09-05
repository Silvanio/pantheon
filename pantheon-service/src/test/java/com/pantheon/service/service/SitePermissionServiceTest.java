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
                UUID.randomUUID(), siteId, UUID.randomUUID(), ConstructionFunction.CLIENT, null, Instant.now());
        clientMembership.accept();
        clientAccess = new SiteAccessContext(false, clientMembership);
    }

    @Test
    void companyStaffAlwaysResolveToManage() {
        SiteAccessContext staffAccess = new SiteAccessContext(true, null);

        AccessLevel level = service.resolve(siteId, staffAccess, PermissionCapability.MATERIAL_APPROVAL);

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
        when(overrideRepository.findBySiteMembershipIdAndCapability(clientMembership.getId(), PermissionCapability.MATERIAL_REQUEST))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.CLIENT, PermissionCapability.MATERIAL_REQUEST))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireManage(siteId, clientAccess, PermissionCapability.MATERIAL_REQUEST))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void engineerCanApproveMaterialsByDefault() {
        SiteMembership engineer = SiteMembership.invited(
                UUID.randomUUID(), siteId, UUID.randomUUID(), ConstructionFunction.ENGINEER, null, Instant.now());
        engineer.accept();
        SiteAccessContext engineerAccess = new SiteAccessContext(false, engineer);
        when(overrideRepository.findBySiteMembershipIdAndCapability(engineer.getId(), PermissionCapability.MATERIAL_APPROVAL))
                .thenReturn(Optional.empty());
        when(overrideRepository.findByConstructionSiteIdAndFunctionAndCapability(
                        siteId, ConstructionFunction.ENGINEER, PermissionCapability.MATERIAL_APPROVAL))
                .thenReturn(Optional.empty());

        service.requireManage(siteId, engineerAccess, PermissionCapability.MATERIAL_APPROVAL);
    }
}
