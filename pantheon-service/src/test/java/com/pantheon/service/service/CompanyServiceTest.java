package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyOnboardingStatus;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.CompanyRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMembershipRepository membershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private MembershipInvitationRepository invitationRepository;

    @Mock
    private MembershipInvitationIssuer invitationIssuer;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(
                companyRepository, membershipRepository, userRepository, invitationRepository, invitationIssuer,
                siteMembershipRepository);
        lenient().when(companyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteMembershipRepository.findByUserId(any())).thenReturn(List.of());
    }

    @Test
    void createPersistsCompanyAndActiveAdminMembership() {
        UUID creatorId = UUID.randomUUID();

        Company company = service.create(creatorId, "Construtora Teste");

        assertThat(company.getName()).isEqualTo("Construtora Teste");
        assertThat(company.getPlanId()).isNull();
        assertThat(company.getOnboardingStatus()).isEqualTo(CompanyOnboardingStatus.PLAN_PENDING);
    }

    @Test
    void addStaffMemberRejectsWhenAlreadyActive() {
        UUID companyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID existingUserId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyMembership adminMembership = new CompanyMembership(UUID.randomUUID(), companyId, adminId, CompanyRole.ADMIN, now);
        when(membershipRepository.findByCompanyIdAndUserId(companyId, adminId)).thenReturn(Optional.of(adminMembership));
        when(companyRepository.findById(companyId))
                .thenReturn(Optional.of(new Company(companyId, "Construtora", adminId, now)));
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(new AppUser(adminId, "admin@example.com", "Admin", "hash", null, now, now)));

        AppUser existingActiveUser = new AppUser(existingUserId, "staff@example.com", "Staff", "hash", null, now, now);
        when(invitationIssuer.resolveOrCreateUser(org.mockito.ArgumentMatchers.eq("staff@example.com"), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(existingActiveUser, false));

        CompanyMembership existingActiveMembership =
                new CompanyMembership(UUID.randomUUID(), companyId, existingUserId, CompanyRole.MEMBER, now);
        when(membershipRepository.findByCompanyIdAndUserId(companyId, existingUserId))
                .thenReturn(Optional.of(existingActiveMembership));

        assertThatThrownBy(() -> service.addStaffMember(companyId, adminId, "staff@example.com"))
                .isInstanceOf(MemberAlreadyActiveException.class);
    }

    @Test
    void addStaffMemberReissuesInvitationForStillInvitedMembership() {
        UUID companyId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID invitedUserId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyMembership adminMembership = new CompanyMembership(UUID.randomUUID(), companyId, adminId, CompanyRole.ADMIN, now);
        when(membershipRepository.findByCompanyIdAndUserId(companyId, adminId)).thenReturn(Optional.of(adminMembership));
        Company company = new Company(companyId, "Construtora", adminId, now);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(new AppUser(adminId, "admin@example.com", "Admin", "hash", null, now, now)));

        AppUser invitedUser = new AppUser(invitedUserId, "invited@example.com", "Invited", null, null, now, now);
        when(invitationIssuer.resolveOrCreateUser(org.mockito.ArgumentMatchers.eq("invited@example.com"), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(invitedUser, false));

        CompanyMembership invitedMembership =
                CompanyMembership.invited(UUID.randomUUID(), companyId, invitedUserId, now);
        when(membershipRepository.findByCompanyIdAndUserId(companyId, invitedUserId))
                .thenReturn(Optional.of(invitedMembership));

        MembershipInvitation existingInvitation = new MembershipInvitation(
                UUID.randomUUID(), invitedMembership.getId(), MembershipType.COMPANY, "invited@example.com", "hash",
                adminId, false, now, now.plusSeconds(3600));
        when(invitationRepository.findByMembershipId(invitedMembership.getId())).thenReturn(Optional.of(existingInvitation));
        when(invitationIssuer.reissue(any(), any(), any(), any(), any())).thenReturn(existingInvitation);

        MembershipInvitation result = service.addStaffMember(companyId, adminId, "invited@example.com");

        assertThat(result).isSameAs(existingInvitation);
    }

    @Test
    void addStaffMemberRequiresAdmin() {
        UUID companyId = UUID.randomUUID();
        UUID nonAdminId = UUID.randomUUID();
        when(membershipRepository.findByCompanyIdAndUserId(companyId, nonAdminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addStaffMember(companyId, nonAdminId, "someone@example.com"))
                .isInstanceOf(NotCompanyAdminException.class);
    }

    @Test
    void getOnboardingStatusReportsNoCompanyWhenNoActiveMemberships() {
        UUID userId = UUID.randomUUID();
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of());

        var status = service.getOnboardingStatus(userId);

        assertThat(status.hasCompany()).isFalse();
        assertThat(status.companies()).isEmpty();
    }

    @Test
    void getOnboardingStatusReportsEachCompanysStatus() {
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Instant now = Instant.now();

        CompanyMembership membership = new CompanyMembership(UUID.randomUUID(), companyId, userId, CompanyRole.ADMIN, now);
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of(membership));

        Company company = new Company(companyId, "Construtora", userId, now);
        when(companyRepository.findAllById(List.of(companyId))).thenReturn(List.of(company));

        var status = service.getOnboardingStatus(userId);

        assertThat(status.hasCompany()).isTrue();
        assertThat(status.companies()).hasSize(1);
        assertThat(status.companies().get(0).onboardingStatus()).isEqualTo(CompanyOnboardingStatus.PLAN_PENDING);
        assertThat(status.companies().get(0).role()).isEqualTo(CompanyRole.ADMIN);
    }

    @Test
    void listStaffRequiresMembership() {
        UUID companyId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        when(membershipRepository.findByCompanyIdAndUserId(companyId, outsiderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listStaff(companyId, outsiderId)).isInstanceOf(NotCompanyMemberException.class);
    }
}
