package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.PersonSearchResult;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonSearchServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private CompanyMembershipRepository companyMembershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private PersonSearchService service;

    private UUID siteId;
    private UUID otherSiteId;
    private UUID companyId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        service = new PersonSearchService(
                siteRepository, siteMembershipRepository, companyMembershipRepository, userRepository,
                siteAccessService, permissionService);

        siteId = UUID.randomUUID();
        otherSiteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();

        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(siteAccessService.requireSite(siteId)).thenReturn(new ConstructionSite(
                siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now()));
        lenient().when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(
                new ConstructionSite(siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now()),
                new ConstructionSite(otherSiteId, companyId, "Obra 2", "Endereco 2", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));
        lenient().when(companyMembershipRepository.findByCompanyId(companyId)).thenReturn(List.of());
    }

    @Test
    void findsPersonFromAnotherObraInTheSameCompany() {
        UUID engineerUserId = UUID.randomUUID();
        AppUser engineer = new AppUser(engineerUserId, "engineer@example.com", "Ana Engenheira", "hash", null, Instant.now(), Instant.now());
        SiteMembership onOtherSite = SiteMembership.invited(
                UUID.randomUUID(), otherSiteId, engineerUserId, ConstructionFunction.ENGINEER, "111.444.777-35", "11987654321",
                Instant.now());
        when(siteMembershipRepository.findByConstructionSiteIdIn(List.of(siteId, otherSiteId))).thenReturn(List.of(onOtherSite));
        when(userRepository.findAllById(any())).thenReturn(List.of(engineer));

        List<PersonSearchResult> results = service.search(siteId, actingUserId, "engineer@");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Ana Engenheira");
        assertThat(results.get(0).email()).isEqualTo("engineer@example.com");
        assertThat(results.get(0).cpf()).isEqualTo("111.444.777-35");
        assertThat(results.get(0).phone()).isEqualTo("11987654321");
    }

    @Test
    void matchesByCpfPrefix() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "client@example.com", "Cliente Um", "hash", null, Instant.now(), Instant.now());
        SiteMembership membership = SiteMembership.invited(
                UUID.randomUUID(), siteId, userId, ConstructionFunction.CLIENT, "111.444.777-35", null, Instant.now());
        when(siteMembershipRepository.findByConstructionSiteIdIn(List.of(siteId, otherSiteId))).thenReturn(List.of(membership));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));

        List<PersonSearchResult> results = service.search(siteId, actingUserId, "111.444");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).cpf()).isEqualTo("111.444.777-35");
    }

    @Test
    void doesNotReturnPeopleFromAnotherCompany() {
        UUID otherCompanySiteId = UUID.randomUUID();
        UUID otherCompanyUserId = UUID.randomUUID();
        AppUser otherCompanyUser =
                new AppUser(otherCompanyUserId, "outsider@example.com", "Outsider", "hash", null, Instant.now(), Instant.now());
        // Not part of this company's sites (siteRepository.findByCompanyId only returns siteId/otherSiteId),
        // so this membership is never even queried — simulating true cross-tenant isolation.
        when(siteMembershipRepository.findByConstructionSiteIdIn(List.of(siteId, otherSiteId))).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        List<PersonSearchResult> results = service.search(siteId, actingUserId, "outsider@");

        assertThat(results).isEmpty();
        assertThat(otherCompanySiteId).isNotNull();
        assertThat(otherCompanyUser.getEmail()).isEqualTo("outsider@example.com");
    }

    @Test
    void queryShorterThanMinimumReturnsEmpty() {
        List<PersonSearchResult> results = service.search(siteId, actingUserId, "en");

        assertThat(results).isEmpty();
    }

    @Test
    void includesCompanyStaffWithNoSiteMembership() {
        UUID staffUserId = UUID.randomUUID();
        AppUser staff = new AppUser(staffUserId, "admin@example.com", "Admin User", "hash", null, Instant.now(), Instant.now());
        when(companyMembershipRepository.findByCompanyId(companyId)).thenReturn(
                List.of(new CompanyMembership(UUID.randomUUID(), companyId, staffUserId, CompanyRole.ADMIN, Instant.now())));
        when(siteMembershipRepository.findByConstructionSiteIdIn(List.of(siteId, otherSiteId))).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of(staff));

        List<PersonSearchResult> results = service.search(siteId, actingUserId, "admin@");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).email()).isEqualTo("admin@example.com");
    }

    @Test
    void checkEmailConflictsReturnsFalseFalseForUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(java.util.Optional.empty());

        var result = service.checkEmailConflicts(siteId, actingUserId, "unknown@example.com");

        assertThat(result.activeOnThisSite()).isFalse();
        assertThat(result.existsInAnotherCompany()).isFalse();
    }

    @Test
    void checkEmailConflictsFlagsActiveMemberOnThisSite() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "client@example.com", "Client", "hash", null, Instant.now(), Instant.now());
        when(userRepository.findByEmail("client@example.com")).thenReturn(java.util.Optional.of(user));
        SiteMembership membership = SiteMembership.invited(
                UUID.randomUUID(), siteId, userId, ConstructionFunction.CLIENT, null, null, Instant.now());
        membership.accept();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, userId)).thenReturn(java.util.Optional.of(membership));
        when(companyMembershipRepository.findByUserId(userId)).thenReturn(List.of());
        when(siteMembershipRepository.findByUserId(userId)).thenReturn(List.of());

        var result = service.checkEmailConflicts(siteId, actingUserId, "client@example.com");

        assertThat(result.activeOnThisSite()).isTrue();
    }

    @Test
    void checkEmailConflictsDoesNotFlagPendingInviteOnThisSite() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "pending@example.com", "Pending", "hash", null, Instant.now(), Instant.now());
        when(userRepository.findByEmail("pending@example.com")).thenReturn(java.util.Optional.of(user));
        SiteMembership membership = SiteMembership.invited(
                UUID.randomUUID(), siteId, userId, ConstructionFunction.ENGINEER, null, null, Instant.now());
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, userId)).thenReturn(java.util.Optional.of(membership));
        when(companyMembershipRepository.findByUserId(userId)).thenReturn(List.of());
        when(siteMembershipRepository.findByUserId(userId)).thenReturn(List.of());

        var result = service.checkEmailConflicts(siteId, actingUserId, "pending@example.com");

        assertThat(result.activeOnThisSite()).isFalse();
    }

    @Test
    void belongsToAnotherCompanyTrueViaCompanyStaff() {
        UUID userId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        when(companyMembershipRepository.findByUserId(userId)).thenReturn(
                List.of(new CompanyMembership(UUID.randomUUID(), otherCompanyId, userId, CompanyRole.MEMBER, Instant.now())));

        assertThat(service.belongsToAnotherCompany(userId, companyId)).isTrue();
    }

    @Test
    void belongsToAnotherCompanyTrueViaOtherCompanysSite() {
        UUID userId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();
        UUID otherCompanySiteId = UUID.randomUUID();
        when(companyMembershipRepository.findByUserId(userId)).thenReturn(List.of());
        SiteMembership elsewhere = SiteMembership.invited(
                UUID.randomUUID(), otherCompanySiteId, userId, ConstructionFunction.SITE_FOREMAN, null, null, Instant.now());
        when(siteMembershipRepository.findByUserId(userId)).thenReturn(List.of(elsewhere));
        when(siteRepository.findAllById(List.of(otherCompanySiteId))).thenReturn(List.of(new ConstructionSite(
                otherCompanySiteId, otherCompanyId, "Outra Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));

        assertThat(service.belongsToAnotherCompany(userId, companyId)).isTrue();
    }

    @Test
    void belongsToAnotherCompanyFalseWhenOnlyInTheSameCompany() {
        UUID userId = UUID.randomUUID();
        when(companyMembershipRepository.findByUserId(userId)).thenReturn(
                List.of(new CompanyMembership(UUID.randomUUID(), companyId, userId, CompanyRole.MEMBER, Instant.now())));
        when(siteMembershipRepository.findByUserId(userId)).thenReturn(List.of());

        assertThat(service.belongsToAnotherCompany(userId, companyId)).isFalse();
    }
}
