package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteMembership;
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
    private PlanService planService;

    @Mock
    private SiteAccessService siteAccessService;

    private ConstructionSiteService service;

    private UUID companyId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() {
        service = new ConstructionSiteService(
                siteRepository, membershipRepository, siteMembershipRepository, planService, siteAccessService);
        companyId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();

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
}
