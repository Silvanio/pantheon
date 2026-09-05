package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteAccessServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private CompanyMembershipRepository companyMembershipRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    private SiteAccessService service;

    private UUID companyId;
    private UUID siteId;
    private ConstructionSite site;

    @BeforeEach
    void setUp() {
        service = new SiteAccessService(siteRepository, companyMembershipRepository, siteMembershipRepository);
        companyId = UUID.randomUUID();
        siteId = UUID.randomUUID();
        site = new ConstructionSite(
                siteId, companyId, "Obra Centro", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
    }

    @Test
    void companyStaffAlwaysHaveAccessWithNoSiteMembership() {
        UUID staffUserId = UUID.randomUUID();
        CompanyMembership staff = new CompanyMembership(UUID.randomUUID(), companyId, staffUserId, CompanyRole.MEMBER, Instant.now());
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, staffUserId)).thenReturn(Optional.of(staff));

        SiteAccessContext access = service.requireAccess(siteId, staffUserId);

        assertThat(access.companyStaff()).isTrue();
        assertThat(access.siteMembership()).isNull();
    }

    @Test
    void activeSiteMemberHasAccessWithResolvedFunction() {
        UUID clientUserId = UUID.randomUUID();
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, clientUserId)).thenReturn(Optional.empty());
        SiteMembership membership = SiteMembership.invited(
                UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, "123.456.789-00", Instant.now());
        membership.accept();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, clientUserId))
                .thenReturn(Optional.of(membership));

        SiteAccessContext access = service.requireAccess(siteId, clientUserId);

        assertThat(access.companyStaff()).isFalse();
        assertThat(access.function()).isEqualTo(ConstructionFunction.CLIENT);
    }

    @Test
    void invitedButNotAcceptedSiteMembershipGrantsNoAccess() {
        UUID invitedUserId = UUID.randomUUID();
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, invitedUserId)).thenReturn(Optional.empty());
        SiteMembership stillInvited = SiteMembership.invited(
                UUID.randomUUID(), siteId, invitedUserId, ConstructionFunction.ENGINEER, null, Instant.now());
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, invitedUserId))
                .thenReturn(Optional.of(stillInvited));

        assertThatThrownBy(() -> service.requireAccess(siteId, invitedUserId)).isInstanceOf(NotSiteMemberException.class);
    }

    @Test
    void userWithNoRelationshipHasNoAccess() {
        UUID strangerId = UUID.randomUUID();
        when(companyMembershipRepository.findByCompanyIdAndUserId(companyId, strangerId)).thenReturn(Optional.empty());
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, strangerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireAccess(siteId, strangerId)).isInstanceOf(NotSiteMemberException.class);
    }
}
