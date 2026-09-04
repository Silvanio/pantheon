package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.UserProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pantheon.service.dto.OnboardingStatusResponse;
import com.pantheon.service.dto.ProjectRegistrationRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.MembershipStatus;
import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.Project;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotProjectAdminException;
import com.pantheon.service.exception.ProjectLimitExceededException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.TeamInvitationCreatedEvent;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.repository.ProjectRepository;
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private MembershipInvitationRepository invitationRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private EventPublisher eventPublisher;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                projectRepository,
                membershipRepository,
                userRepository,
                invitationRepository,
                new UserProfileService(userProfileRepository),
                eventPublisher);
        lenient().when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(membershipRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(invitationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ProjectRegistrationRequest registrationRequest() {
        return new ProjectRegistrationRequest(
                "Obra Centro", "12.345.678/0001-90", "Acme Ltda", "Rua Um, 100", "01000-000");
    }

    @Test
    void createSetsThreeDayTrialAndAdminMembership() {
        UUID creatorId = UUID.randomUUID();
        when(projectRepository.findByCreatedBy(creatorId)).thenReturn(List.of());
        when(userProfileRepository.findByUserId(creatorId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        Project project = projectService.create(creatorId, registrationRequest());
        Instant after = Instant.now();

        assertThat(project.getName()).isEqualTo("Obra Centro");
        assertThat(project.getPlan()).isNull();
        assertThat(project.getCreatedBy()).isEqualTo(creatorId);
        assertThat(project.getTrialExpiresAt()).isBetween(
                before.plus(3, ChronoUnit.DAYS), after.plus(3, ChronoUnit.DAYS));
        assertThat(project.isActive(Instant.now())).isTrue();
    }

    @Test
    void createBlockedWhenAdminAlreadyAtPlanLimit() {
        UUID creatorId = UUID.randomUUID();
        Project first = projectWithPlan(creatorId, Plan.BASIC);
        Project second = projectWithPlan(creatorId, Plan.BASIC);
        when(projectRepository.findByCreatedBy(creatorId)).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> projectService.create(creatorId, registrationRequest()))
                .isInstanceOf(ProjectLimitExceededException.class);
    }

    @Test
    void createAllowedWhenBelowPlanLimit() {
        UUID creatorId = UUID.randomUUID();
        Project first = projectWithPlan(creatorId, Plan.BASIC);
        when(projectRepository.findByCreatedBy(creatorId)).thenReturn(List.of(first));
        when(userProfileRepository.findByUserId(creatorId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.create(creatorId, registrationRequest());

        assertThat(created).isNotNull();
    }

    @Test
    void onboardingStatusReportsNoProjectForUserWithNoMemberships() {
        UUID userId = UUID.randomUUID();
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of());

        OnboardingStatusResponse status = projectService.getOnboardingStatus(userId);

        assertThat(status.hasProject()).isFalse();
        assertThat(status.needsPlanSelection()).isFalse();
        assertThat(status.activeProject()).isNull();
    }

    @Test
    void onboardingStatusIgnoresInvitedOnlyMemberships() {
        UUID userId = UUID.randomUUID();
        ProjectMembership invited = ProjectMembership.invited(
                UUID.randomUUID(), UUID.randomUUID(), userId, ConstructionFunction.ENGINEER, null, Instant.now());
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of(invited));

        OnboardingStatusResponse status = projectService.getOnboardingStatus(userId);

        assertThat(status.hasProject()).isFalse();
    }

    @Test
    void onboardingStatusReportsActiveProject() {
        UUID userId = UUID.randomUUID();
        Project project = new Project(
                UUID.randomUUID(), "Obra Centro", userId, Instant.now(), Instant.now().plus(3, ChronoUnit.DAYS));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), project.getId(), userId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of(membership));
        when(projectRepository.findAllById(List.of(project.getId()))).thenReturn(List.of(project));

        OnboardingStatusResponse status = projectService.getOnboardingStatus(userId);

        assertThat(status.hasProject()).isTrue();
        assertThat(status.needsPlanSelection()).isFalse();
        assertThat(status.activeProject().id()).isEqualTo(project.getId());
        assertThat(status.activeProject().role()).isEqualTo(ProjectRole.ADMIN);
    }

    @Test
    void onboardingStatusNeedsPlanSelectionWhenOnlyExpiredProjectsExist() {
        UUID userId = UUID.randomUUID();
        Project expired = new Project(
                UUID.randomUUID(), "Obra Centro", userId,
                Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().minus(7, ChronoUnit.DAYS));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), expired.getId(), userId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByUserId(userId)).thenReturn(List.of(membership));
        when(projectRepository.findAllById(List.of(expired.getId()))).thenReturn(List.of(expired));

        OnboardingStatusResponse status = projectService.getOnboardingStatus(userId);

        assertThat(status.hasProject()).isTrue();
        assertThat(status.needsPlanSelection()).isTrue();
        assertThat(status.expiredProjectId()).isEqualTo(expired.getId());
    }

    @Test
    void confirmPlanSetsOneYearValidity() {
        UUID adminId = UUID.randomUUID();
        Project project = new Project(
                UUID.randomUUID(), "Obra Centro", adminId,
                Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().minus(7, ChronoUnit.DAYS));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), project.getId(), adminId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(project.getId(), adminId))
                .thenReturn(Optional.of(membership));
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        Instant before = Instant.now();
        Project confirmed = projectService.confirmPlan(project.getId(), adminId, Plan.PRO);
        Instant after = Instant.now();

        assertThat(confirmed.getPlan()).isEqualTo(Plan.PRO);
        assertThat(confirmed.getPlanValidUntil()).isBetween(
                before.plus(365, ChronoUnit.DAYS), after.plus(365, ChronoUnit.DAYS));
        assertThat(confirmed.isActive(Instant.now())).isTrue();
    }

    @Test
    void confirmPlanRejectedForNonAdmin() {
        UUID projectId = UUID.randomUUID();
        UUID nonAdminId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(projectId, nonAdminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.confirmPlan(projectId, nonAdminId, Plan.BASIC))
                .isInstanceOf(NotProjectAdminException.class);
    }

    @Test
    void addMemberInvitesRegisteredUserAsPendingMembership() {
        Fixture f = adminFixture();
        AppUser existing = activeUser("member@example.com");
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(existing));
        when(membershipRepository.findByProjectIdAndUserId(f.projectId, existing.getId())).thenReturn(Optional.empty());

        MembershipInvitation invitation = projectService.addMember(
                f.projectId, f.adminId, "member@example.com", ConstructionFunction.ENGINEER, null);

        ProjectMembership saved = captureSavedMembership();
        assertThat(saved.getRole()).isEqualTo(ProjectRole.MEMBER);
        assertThat(saved.getStatus()).isEqualTo(MembershipStatus.INVITED);
        assertThat(saved.getUserId()).isEqualTo(existing.getId());
        assertThat(saved.getFunction()).isEqualTo(ConstructionFunction.ENGINEER);
        assertThat(invitation.isRequiresRegistration()).isFalse();
        verify(invitationRepository).save(any(MembershipInvitation.class));
        verify(eventPublisher).publish(eq(TeamInvitationCreatedEvent.TYPE), any());
    }

    @Test
    void addMemberCreatesPreRegistrationForUnknownEmail() {
        Fixture f = adminFixture();
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        MembershipInvitation invitation =
                projectService.addMember(f.projectId, f.adminId, "novo@example.com", null, null);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isPendingRegistration()).isTrue();
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("novo@example.com");

        ProjectMembership saved = captureSavedMembership();
        assertThat(saved.getStatus()).isEqualTo(MembershipStatus.INVITED);
        assertThat(saved.getFunction()).isEqualTo(ConstructionFunction.OTHER);
        assertThat(invitation.isRequiresRegistration()).isTrue();
    }

    @Test
    void addMemberKeepsSpecialtyForServiceProvider() {
        Fixture f = adminFixture();
        AppUser existing = activeUser("member@example.com");
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(existing));
        when(membershipRepository.findByProjectIdAndUserId(f.projectId, existing.getId())).thenReturn(Optional.empty());

        projectService.addMember(
                f.projectId, f.adminId, "member@example.com", ConstructionFunction.SERVICE_PROVIDER, "Pintor");

        ProjectMembership saved = captureSavedMembership();
        assertThat(saved.getFunction()).isEqualTo(ConstructionFunction.SERVICE_PROVIDER);
        assertThat(saved.getSpecialty()).isEqualTo("Pintor");
    }

    @Test
    void addMemberRejectsEmailThatIsAlreadyActiveMember() {
        Fixture f = adminFixture();
        AppUser existing = activeUser("member@example.com");
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(existing));
        ProjectMembership active = new ProjectMembership(
                UUID.randomUUID(), f.projectId, existing.getId(), ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(f.projectId, existing.getId()))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> projectService.addMember(f.projectId, f.adminId, "member@example.com", null, null))
                .isInstanceOf(MemberAlreadyActiveException.class);
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void addMemberReissuesInvitationForAlreadyInvitedEmail() {
        Fixture f = adminFixture();
        AppUser existing = activeUser("member@example.com");
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(existing));
        ProjectMembership invited = ProjectMembership.invited(
                UUID.randomUUID(), f.projectId, existing.getId(), ConstructionFunction.OTHER, null, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(f.projectId, existing.getId()))
                .thenReturn(Optional.of(invited));
        MembershipInvitation prior = new MembershipInvitation(
                UUID.randomUUID(), invited.getId(), "member@example.com", "old-hash", f.adminId, false,
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(13, ChronoUnit.DAYS));
        when(invitationRepository.findByMembershipId(invited.getId())).thenReturn(Optional.of(prior));

        projectService.addMember(f.projectId, f.adminId, "member@example.com", null, null);

        assertThat(prior.getTokenHash()).isNotEqualTo("old-hash");
        verify(invitationRepository).save(prior);
        verify(membershipRepository, never()).save(any());
        verify(eventPublisher).publish(eq(TeamInvitationCreatedEvent.TYPE), any());
    }

    @Test
    void addMemberRejectedForNonAdmin() {
        UUID projectId = UUID.randomUUID();
        UUID nonAdminId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(projectId, nonAdminId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.addMember(projectId, nonAdminId, "member@example.com", null, null))
                .isInstanceOf(NotProjectAdminException.class);
    }

    @Test
    void listMembersReturnsInvitationFlagForEachMember() {
        UUID projectId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMembership adminMembership = new ProjectMembership(
                UUID.randomUUID(), projectId, adminId, ProjectRole.ADMIN, Instant.now());
        ProjectMembership invitedMembership = ProjectMembership.invited(
                UUID.randomUUID(), projectId, memberId, ConstructionFunction.SITE_FOREMAN, null, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, adminId)).thenReturn(Optional.of(adminMembership));
        when(membershipRepository.findByProjectId(projectId))
                .thenReturn(List.of(adminMembership, invitedMembership));
        AppUser admin = activeUser("admin@example.com", adminId);
        AppUser member = new AppUser(memberId, "member@example.com", "Member", "hash", null, Instant.now(), Instant.now());
        when(userRepository.findAllById(List.of(adminId, memberId))).thenReturn(List.of(admin, member));

        var members = projectService.listMembers(projectId, adminId);

        assertThat(members).hasSize(2);
        assertThat(members).anySatisfy(m -> {
            assertThat(m.email()).isEqualTo("member@example.com");
            assertThat(m.invited()).isTrue();
            assertThat(m.status()).isEqualTo(MembershipStatus.INVITED);
        });
        assertThat(members).anySatisfy(m -> {
            assertThat(m.email()).isEqualTo("admin@example.com");
            assertThat(m.invited()).isFalse();
        });
    }

    @Test
    void listMembersRejectedForInvitedOnlyUser() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMembership invited = ProjectMembership.invited(
                UUID.randomUUID(), projectId, userId, ConstructionFunction.OTHER, null, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(invited));

        assertThatThrownBy(() -> projectService.listMembers(projectId, userId))
                .isInstanceOf(com.pantheon.service.exception.NotProjectMemberException.class);
    }

    private record Fixture(UUID projectId, UUID adminId) {}

    private Fixture adminFixture() {
        UUID projectId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ProjectMembership adminMembership =
                new ProjectMembership(UUID.randomUUID(), projectId, adminId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, adminId)).thenReturn(Optional.of(adminMembership));
        Project project = new Project(
                projectId, "Obra Centro", adminId, Instant.now(), Instant.now().plus(3, ChronoUnit.DAYS));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(activeUser("admin@example.com", adminId)));
        return new Fixture(projectId, adminId);
    }

    private ProjectMembership captureSavedMembership() {
        ArgumentCaptor<ProjectMembership> captor = ArgumentCaptor.forClass(ProjectMembership.class);
        verify(membershipRepository).save(captor.capture());
        return captor.getValue();
    }

    private AppUser activeUser(String email) {
        return activeUser(email, UUID.randomUUID());
    }

    private AppUser activeUser(String email, UUID id) {
        return new AppUser(id, email, email.substring(0, email.indexOf('@')), "hash", null, Instant.now(), Instant.now());
    }

    private Project projectWithPlan(UUID creatorId, Plan plan) {
        Project project = new Project(
                UUID.randomUUID(), "Obra Centro", creatorId,
                Instant.now().minus(400, ChronoUnit.DAYS), Instant.now().minus(397, ChronoUnit.DAYS));
        project.confirmPlan(plan, Instant.now().minus(390, ChronoUnit.DAYS), Instant.now().plus(300, ChronoUnit.DAYS));
        return project;
    }
}
