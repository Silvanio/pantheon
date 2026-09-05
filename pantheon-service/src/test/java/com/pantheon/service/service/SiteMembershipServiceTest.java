package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipType;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
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

    private SiteMembershipService service;

    private UUID siteId;
    private UUID staffUserId;

    @BeforeEach
    void setUp() {
        service = new SiteMembershipService(membershipRepository, invitationRepository, userRepository, invitationIssuer, siteAccessService);
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
                UUID.randomUUID(), siteId, notStaffId, ConstructionFunction.CLIENT, null, Instant.now());
        clientMembership.accept();
        when(siteAccessService.requireAccess(siteId, notStaffId)).thenReturn(new SiteAccessContext(false, clientMembership));

        assertThatThrownBy(() -> service.addAccountlessServiceProvider(siteId, notStaffId, "Joao", "Pedreiro", null))
                .isInstanceOf(NotSiteMemberException.class);
    }

    @Test
    void addAccountlessServiceProviderCreatesActiveMembershipWithNoAccount() {
        SiteMembership result = service.addAccountlessServiceProvider(siteId, staffUserId, "Joao Pedreiro", "Pedreiro", null);

        assertThat(result.getFunction()).isEqualTo(ConstructionFunction.SERVICE_PROVIDER);
        assertThat(result.getUserId()).isNull();
        assertThat(result.getServiceProviderTrade()).isEqualTo("Pedreiro");
        assertThat(result.getDisplayName()).isEqualTo("Joao Pedreiro");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void inviteMemberCreatesInvitedMembershipForNewFunction() {
        Instant now = Instant.now();
        AppUser inviter = new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now);
        when(userRepository.findById(staffUserId)).thenReturn(Optional.of(inviter));

        AppUser invitee = new AppUser(UUID.randomUUID(), "engineer@example.com", "Engineer", null, null, now, now);
        when(invitationIssuer.resolveOrCreateUser(org.mockito.ArgumentMatchers.eq("engineer@example.com"), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(invitee, true));
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, invitee.getId())).thenReturn(Optional.empty());

        MembershipInvitation expectedInvitation = new MembershipInvitation(
                UUID.randomUUID(), UUID.randomUUID(), MembershipType.SITE, "engineer@example.com", "hash", staffUserId,
                true, now, now.plusSeconds(3600));
        when(invitationIssuer.issue(
                        any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any(), any()))
                .thenReturn(expectedInvitation);

        MembershipInvitation result =
                service.inviteMember(siteId, staffUserId, ConstructionFunction.ENGINEER, "engineer@example.com", null);

        assertThat(result).isSameAs(expectedInvitation);
    }

    @Test
    void inviteMemberRejectsWhenAlreadyActiveOnThisSite() {
        Instant now = Instant.now();
        when(userRepository.findById(staffUserId))
                .thenReturn(Optional.of(new AppUser(staffUserId, "staff@example.com", "Staff", "hash", null, now, now)));

        AppUser existingUser = new AppUser(UUID.randomUUID(), "client@example.com", "Client", "hash", null, now, now);
        when(invitationIssuer.resolveOrCreateUser(org.mockito.ArgumentMatchers.eq("client@example.com"), any()))
                .thenReturn(new MembershipInvitationIssuer.ResolvedUser(existingUser, false));

        SiteMembership activeMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, existingUser.getId(), ConstructionFunction.CLIENT, null, now);
        activeMembership.accept();
        when(membershipRepository.findByConstructionSiteIdAndUserId(siteId, existingUser.getId()))
                .thenReturn(Optional.of(activeMembership));

        assertThatThrownBy(() ->
                        service.inviteMember(siteId, staffUserId, ConstructionFunction.CLIENT, "client@example.com", null))
                .isInstanceOf(MemberAlreadyActiveException.class);
    }

    @Test
    void listMembersAllowsAnySiteMemberNotJustStaff() {
        UUID clientUserId = UUID.randomUUID();
        SiteMembership clientMembership = SiteMembership.invited(
                UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, Instant.now());
        clientMembership.accept();
        when(siteAccessService.requireAccess(siteId, clientUserId)).thenReturn(new SiteAccessContext(false, clientMembership));
        when(membershipRepository.findByConstructionSiteId(siteId)).thenReturn(java.util.List.of(clientMembership));
        when(userRepository.findAllById(any())).thenReturn(java.util.List.of());

        var result = service.listMembers(siteId, clientUserId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).function()).isEqualTo(ConstructionFunction.CLIENT);
    }
}
