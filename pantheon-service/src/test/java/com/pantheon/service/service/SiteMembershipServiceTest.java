package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.exception.SiteMembershipNotFoundException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.SitePermissionOverrideRepository;
import com.pantheon.service.repository.TaskCardAssigneeRepository;
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
class SiteMembershipServiceTest {

    @Mock
    private SiteMembershipRepository membershipRepository;

    @Mock
    private MembershipInvitationRepository invitationRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private MembershipInvitationIssuer invitationIssuer;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private SitePermissionOverrideRepository permissionOverrideRepository;

    @Mock
    private TaskCardAssigneeRepository taskCardAssigneeRepository;

    @Mock
    private PersonSearchService personSearchService;

    private SiteMembershipService service;

    private UUID siteId;
    private UUID staffUserId;

    @BeforeEach
    void setUp() {
        service = new SiteMembershipService(
                membershipRepository, invitationRepository, userRepository, invitationIssuer, siteAccessService,
                permissionService, permissionOverrideRepository, taskCardAssigneeRepository, personSearchService);
        lenient().when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        siteId = UUID.randomUUID();
        staffUserId = UUID.randomUUID();
        lenient().when(siteAccessService.requireAccess(siteId, staffUserId))
                .thenReturn(new SiteAccessContext(true, null));
        lenient().when(siteAccessService.requireSite(siteId)).thenReturn(new ConstructionSite(
                siteId, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now()));
    }

    @Test
    void addAccountlessServiceProviderRequiresCompanyStaff() {
        UUID notStaffId = UUID.randomUUID();
        SiteMembership clientMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, notStaffId, ConstructionFunction.CLIENT, null, null, Instant.now());
        clientMembership.accept();
        when(siteAccessService.requireAccess(siteId, notStaffId)).thenReturn(new SiteAccessContext(false, clientMembership));

        assertThatThrownBy(() ->
                        service.addAccountlessServiceProvider(siteId, notStaffId, "Joao", "Pedreiro", null, null, null))
                .isInstanceOf(NotSiteMemberException.class);
    }

    @Test
    void addAccountlessServiceProviderCreatesActiveMembershipWithNoAccount() {
        SiteMembership result = service.addAccountlessServiceProvider(
                siteId, staffUserId, "Joao Pedreiro", "Pedreiro", null, null, "11987654321");

        assertThat(result.getFunction()).isEqualTo(ConstructionFunction.SERVICE_PROVIDER);
        assertThat(result.getUserId()).isNull();
        assertThat(result.getServiceProviderTrade()).isEqualTo("Pedreiro");
        assertThat(result.getDisplayName()).isEqualTo("Joao Pedreiro");
        assertThat(result.getPhone()).isEqualTo("11987654321");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void addAccountlessServiceProviderRejectsInvalidCpf() {
        assertThatThrownBy(() -> service.addAccountlessServiceProvider(
                        siteId, staffUserId, "Joao Pedreiro", "Pedreiro", null, "111.111.111-11", null))
                .isInstanceOf(com.pantheon.service.exception.InvalidCpfException.class);
    }

    @Test
    void addAccountlessServiceProviderAcceptsValidCpf() {
        SiteMembership result = service.addAccountlessServiceProvider(
                siteId, staffUserId, "Joao Pedreiro", "Pedreiro", null, "111.444.777-35", null);

        assertThat(result.getCpf()).isEqualTo("111.444.777-35");
    }

    @Test
    void inviteMemberCreatesInvitedMembershipForNewFunction() {
        Instant now = Instant.now();
        AppUser inviter = new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now);
        when(userRepository.findById(staffUserId)).thenReturn(Optional.of(inviter));

        AppUser invitee = new AppUser(UUID.randomUUID(), "engineer@example.com", "Engineer", null, null, now, now);
        when(invitationIssuer.resolveOrCreateUser(
                        org.mockito.ArgumentMatchers.eq("engineer@example.com"), any(), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(invitee, true));
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, invitee.getId())).thenReturn(Optional.empty());

        MembershipInvitation expectedInvitation = new MembershipInvitation(
                UUID.randomUUID(), UUID.randomUUID(), MembershipType.SITE, "engineer@example.com", "hash", staffUserId,
                true, now, now.plusSeconds(3600));
        when(invitationIssuer.issue(
                        any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any(), any()))
                .thenReturn(expectedInvitation);

        MembershipInvitation result = service.inviteMember(
                siteId, staffUserId, ConstructionFunction.ENGINEER, "Engineer Name", "engineer@example.com", null, null);

        assertThat(result).isSameAs(expectedInvitation);
        verify(invitationIssuer).resolveOrCreateUser(eq("engineer@example.com"), eq("Engineer Name"), any());
    }

    @Test
    void inviteMemberRejectsWhenAlreadyActiveOnThisSite() {
        Instant now = Instant.now();
        when(userRepository.findById(staffUserId))
                .thenReturn(Optional.of(new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now)));

        AppUser existingUser = new AppUser(UUID.randomUUID(), "client@example.com", "Client", "hash", null, now, now);
        when(invitationIssuer.resolveOrCreateUser(org.mockito.ArgumentMatchers.eq("client@example.com"), any(), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(existingUser, false));

        SiteMembership activeMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, existingUser.getId(), ConstructionFunction.CLIENT, null, null, now);
        activeMembership.accept();
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, existingUser.getId()))
                .thenReturn(Optional.of(activeMembership));

        assertThatThrownBy(() -> service.inviteMember(
                        siteId, staffUserId, ConstructionFunction.CLIENT, "Client Name", "client@example.com", null, null))
                .isInstanceOf(MemberAlreadyActiveException.class);
    }

    @Test
    void inviteMemberRejectsInvalidCpf() {
        assertThatThrownBy(() -> service.inviteMember(
                        siteId, staffUserId, ConstructionFunction.CLIENT, "Client Name", "client@example.com",
                        "111.111.111-11", null))
                .isInstanceOf(com.pantheon.service.exception.InvalidCpfException.class);
    }

    @Test
    void inviteMemberRejectsPersonAlreadyInAnotherCompany() {
        Instant now = Instant.now();
        when(userRepository.findById(staffUserId))
                .thenReturn(Optional.of(new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now)));
        AppUser existingUser = new AppUser(UUID.randomUUID(), "outsider@example.com", "Outsider", "hash", null, now, now);
        when(invitationIssuer.resolveOrCreateUser(eq("outsider@example.com"), any(), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(existingUser, false));
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, existingUser.getId())).thenReturn(Optional.empty());
        ConstructionSite site = siteAccessService.requireSite(siteId);
        when(personSearchService.belongsToAnotherCompany(existingUser.getId(), site.getCompanyId())).thenReturn(true);

        assertThatThrownBy(() -> service.inviteMember(
                        siteId, staffUserId, ConstructionFunction.ENGINEER, "Outsider", "outsider@example.com", null, null))
                .isInstanceOf(com.pantheon.service.exception.PersonBelongsToAnotherCompanyException.class);
    }

    @Test
    void inviteMemberAllowsPersonAlreadyKnownToTheSameCompany() {
        Instant now = Instant.now();
        when(userRepository.findById(staffUserId))
                .thenReturn(Optional.of(new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now)));
        AppUser existingUser = new AppUser(UUID.randomUUID(), "known@example.com", "Known Person", "hash", null, now, now);
        when(invitationIssuer.resolveOrCreateUser(eq("known@example.com"), any(), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(existingUser, false));
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, existingUser.getId())).thenReturn(Optional.empty());
        ConstructionSite site = siteAccessService.requireSite(siteId);
        when(personSearchService.belongsToAnotherCompany(existingUser.getId(), site.getCompanyId())).thenReturn(false);
        MembershipInvitation expectedInvitation = new MembershipInvitation(
                UUID.randomUUID(), UUID.randomUUID(), MembershipType.SITE, "known@example.com", "hash", staffUserId,
                false, now, now.plusSeconds(3600));
        when(invitationIssuer.issue(
                        any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any(), any()))
                .thenReturn(expectedInvitation);

        MembershipInvitation result = service.inviteMember(
                siteId, staffUserId, ConstructionFunction.ARCHITECT, "Known Person", "known@example.com", null, null);

        assertThat(result).isSameAs(expectedInvitation);
    }

    @Test
    void attachAccountSeedsNewAccountWithTheAccountlessMembershipsKnownName() {
        Instant now = Instant.now();
        SiteMembership membership = SiteMembership.accountless(
                UUID.randomUUID(), siteId, "Pedreiro", "Joao Pedreiro", null, null, null, now);
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));
        when(userRepository.findById(staffUserId))
                .thenReturn(Optional.of(new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now)));
        AppUser newAccount = new AppUser(UUID.randomUUID(), "joao@example.com", "Joao Pedreiro", null, null, now, now);
        when(invitationIssuer.resolveOrCreateUser(eq("joao@example.com"), eq("Joao Pedreiro"), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(newAccount, true));
        when(invitationIssuer.issue(
                        any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any(), any()))
                .thenReturn(new MembershipInvitation(
                        UUID.randomUUID(), membership.getId(), MembershipType.SITE, "joao@example.com", "hash",
                        staffUserId, true, now, now.plusSeconds(3600)));

        service.attachAccount(membership.getId(), staffUserId, "joao@example.com");

        verify(invitationIssuer).resolveOrCreateUser(eq("joao@example.com"), eq("Joao Pedreiro"), any());
    }

    @Test
    void listMembersAllowsAnySiteMemberNotJustStaff() {
        UUID clientUserId = UUID.randomUUID();
        SiteMembership clientMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, null, Instant.now());
        clientMembership.accept();
        when(siteAccessService.requireAccess(siteId, clientUserId)).thenReturn(new SiteAccessContext(false, clientMembership));
        when(membershipRepository.findByConstructionSiteId(siteId)).thenReturn(java.util.List.of(clientMembership));
        when(userRepository.findAllById(any())).thenReturn(java.util.List.of());

        var result = service.listMembers(siteId, clientUserId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).function()).isEqualTo(ConstructionFunction.CLIENT);
    }

    @Test
    void listMembersRejectsMemberHiddenFromTeam() {
        UUID hiddenUserId = UUID.randomUUID();
        SiteMembership membership = SiteMembership.invited(
                UUID.randomUUID(), siteId, hiddenUserId, ConstructionFunction.CLIENT, null, null, Instant.now());
        membership.accept();
        when(siteAccessService.requireAccess(siteId, hiddenUserId)).thenReturn(new SiteAccessContext(false, membership));
        doThrow(new com.pantheon.service.exception.ForbiddenCapabilityException(siteId, PermissionCapability.TEAM_MANAGE))
                .when(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.TEAM_MANAGE));

        assertThatThrownBy(() -> service.listMembers(siteId, hiddenUserId))
                .isInstanceOf(com.pantheon.service.exception.ForbiddenCapabilityException.class);
    }

    @Test
    void findMyFunctionReturnsTheCallersOwnFunction() {
        UUID clientUserId = UUID.randomUUID();
        SiteMembership clientMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, null, Instant.now());
        clientMembership.accept();
        when(siteAccessService.requireAccess(siteId, clientUserId)).thenReturn(new SiteAccessContext(false, clientMembership));
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, clientUserId))
                .thenReturn(Optional.of(clientMembership));

        assertThat(service.findMyFunction(siteId, clientUserId)).isEqualTo(ConstructionFunction.CLIENT);
    }

    @Test
    void findMyFunctionReturnsTheCallersFunctionEvenWhenTheyAreAlsoCompanyStaff() {
        // SiteAccessContext.function() would be null here (company staff resolves with no
        // SiteMembership attached) — findMyFunction must look past that to the real membership.
        UUID staffClientUserId = UUID.randomUUID();
        SiteMembership clientMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, staffClientUserId, ConstructionFunction.CLIENT, null, null, Instant.now());
        clientMembership.accept();
        when(siteAccessService.requireAccess(siteId, staffClientUserId)).thenReturn(new SiteAccessContext(true, null));
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, staffClientUserId))
                .thenReturn(Optional.of(clientMembership));

        assertThat(service.findMyFunction(siteId, staffClientUserId)).isEqualTo(ConstructionFunction.CLIENT);
    }

    @Test
    void findMyFunctionReturnsNullForStaffWithNoSiteMembership() {
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, staffUserId)).thenReturn(Optional.empty());

        assertThat(service.findMyFunction(siteId, staffUserId)).isNull();
    }

    @Test
    void removeMemberDeletesMembershipAndDependents() {
        SiteMembership membership = SiteMembership.admin(UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now());
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));

        service.removeMember(siteId, staffUserId, membership.getId());

        verify(taskCardAssigneeRepository).deleteBySiteMembershipId(membership.getId());
        verify(permissionOverrideRepository).deleteBySiteMembershipId(membership.getId());
        verify(membershipRepository).delete(membership);
    }

    @Test
    void removeMemberRejectsCallerWithoutTeamManagePermission() {
        SiteMembership membership = SiteMembership.admin(UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now());
        doThrow(new com.pantheon.service.exception.ForbiddenCapabilityException(siteId, PermissionCapability.TEAM_MANAGE))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TEAM_MANAGE));

        assertThatThrownBy(() -> service.removeMember(siteId, staffUserId, membership.getId()))
                .isInstanceOf(com.pantheon.service.exception.ForbiddenCapabilityException.class);
    }

    @Test
    void removeMemberRejectsMembershipFromAnotherSite() {
        SiteMembership membership = SiteMembership.admin(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
        when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.removeMember(siteId, staffUserId, membership.getId()))
                .isInstanceOf(SiteMembershipNotFoundException.class);
    }
}
