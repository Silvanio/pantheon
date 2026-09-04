package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.AcceptInvitationResponse;
import com.pantheon.service.dto.InvitationResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipStatus;
import com.pantheon.service.entity.Project;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.exception.InvitationNotFoundException;
import com.pantheon.service.exception.InvitationNotForCurrentUserException;
import com.pantheon.service.exception.RegistrationNotApplicableException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.repository.ProjectRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private MembershipInvitationRepository invitationRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private InvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationService = new InvitationService(
                invitationRepository, membershipRepository, userRepository, projectRepository, passwordEncoder);
        lenient().when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
    }

    private static final String RAW_TOKEN = "raw-token-value";

    private Scenario scenario(boolean requiresRegistration, MembershipStatus membershipStatus) {
        UUID projectId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        Project project = new Project(
                projectId, "Obra Centro", inviterId, Instant.now(), Instant.now().plus(3, ChronoUnit.DAYS));
        ProjectMembership membership = membershipStatus == MembershipStatus.INVITED
                ? ProjectMembership.invited(
                        UUID.randomUUID(), projectId, inviteeId, ConstructionFunction.OTHER, null, Instant.now())
                : new ProjectMembership(
                        UUID.randomUUID(), projectId, inviteeId,
                        com.pantheon.service.entity.ProjectRole.MEMBER, Instant.now());
        MembershipInvitation invitation = new MembershipInvitation(
                UUID.randomUUID(), membership.getId(), "invitee@example.com", InvitationToken.hash(RAW_TOKEN),
                inviterId, requiresRegistration, Instant.now(), Instant.now().plus(14, ChronoUnit.DAYS));

        lenient().when(invitationRepository.findByTokenHash(InvitationToken.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(invitation));
        lenient().when(membershipRepository.findById(membership.getId())).thenReturn(Optional.of(membership));
        lenient().when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        lenient().when(userRepository.findById(inviterId))
                .thenReturn(Optional.of(new AppUser(inviterId, "inviter@example.com", "Maria", "h", null,
                        Instant.now(), Instant.now())));
        return new Scenario(projectId, inviteeId, invitation, membership);
    }

    private record Scenario(UUID projectId, UUID inviteeId, MembershipInvitation invitation, ProjectMembership membership) {}

    @Test
    void getByTokenReturnsPublicDetails() {
        Scenario s = scenario(true, MembershipStatus.INVITED);

        InvitationResponse response = invitationService.getByToken(RAW_TOKEN);

        assertThat(response.projectName()).isEqualTo("Obra Centro");
        assertThat(response.inviterName()).isEqualTo("Maria");
        assertThat(response.email()).isEqualTo("invitee@example.com");
        assertThat(response.requiresRegistration()).isTrue();
        assertThat(response.accepted()).isFalse();
    }

    @Test
    void getByTokenRejectsUnknownToken() {
        when(invitationRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.getByToken("nope"))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void getByTokenRejectsExpiredInvitation() {
        MembershipInvitation expired = new MembershipInvitation(
                UUID.randomUUID(), UUID.randomUUID(), "invitee@example.com", InvitationToken.hash(RAW_TOKEN),
                UUID.randomUUID(), false, Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now().minus(16, ChronoUnit.DAYS));
        when(invitationRepository.findByTokenHash(InvitationToken.hash(RAW_TOKEN))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> invitationService.getByToken(RAW_TOKEN))
                .isInstanceOf(InvitationNotFoundException.class);
    }

    @Test
    void completeRegistrationSetsCredentialsOnPendingAccount() {
        Scenario s = scenario(true, MembershipStatus.INVITED);
        AppUser pending = AppUser.preRegistration(s.inviteeId(), "invitee@example.com", Instant.now());
        when(userRepository.findById(s.inviteeId())).thenReturn(Optional.of(pending));

        AppUser result = invitationService.completeRegistration(RAW_TOKEN, "password123", "Novo Membro");

        assertThat(result.isPendingRegistration()).isFalse();
        assertThat(result.getPasswordHash()).isEqualTo("encoded");
        assertThat(result.getDisplayName()).isEqualTo("Novo Membro");
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void completeRegistrationRejectedWhenInvitationDoesNotRequireIt() {
        Scenario s = scenario(false, MembershipStatus.INVITED);
        when(userRepository.findById(s.inviteeId()))
                .thenReturn(Optional.of(new AppUser(s.inviteeId(), "invitee@example.com", "X", "hash", null,
                        Instant.now(), Instant.now())));

        assertThatThrownBy(() -> invitationService.completeRegistration(RAW_TOKEN, "password123", "X"))
                .isInstanceOf(RegistrationNotApplicableException.class);
    }

    @Test
    void acceptFlipsMembershipToActive() {
        Scenario s = scenario(false, MembershipStatus.INVITED);

        AcceptInvitationResponse response = invitationService.accept(RAW_TOKEN, s.inviteeId());

        assertThat(response.projectId()).isEqualTo(s.projectId());
        assertThat(response.projectName()).isEqualTo("Obra Centro");
        assertThat(s.membership().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(s.invitation().isAccepted()).isTrue();
    }

    @Test
    void acceptRejectedForDifferentAccount() {
        Scenario s = scenario(false, MembershipStatus.INVITED);

        assertThatThrownBy(() -> invitationService.accept(RAW_TOKEN, UUID.randomUUID()))
                .isInstanceOf(InvitationNotForCurrentUserException.class);
        assertThat(s.membership().getStatus()).isEqualTo(MembershipStatus.INVITED);
    }

    @Test
    void acceptIsIdempotentWhenAlreadyActive() {
        Scenario s = scenario(false, MembershipStatus.ACTIVE);

        AcceptInvitationResponse response = invitationService.accept(RAW_TOKEN, s.inviteeId());

        assertThat(response.projectId()).isEqualTo(s.projectId());
        assertThat(s.membership().getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }
}
