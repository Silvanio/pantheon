package com.pantheon.service.service;

import com.pantheon.service.dto.ActiveProjectResponse;
import com.pantheon.service.dto.OnboardingStatusResponse;
import com.pantheon.service.dto.ProjectMemberResponse;
import com.pantheon.service.dto.ProjectMembershipResponse;
import com.pantheon.service.dto.ProjectRegistrationRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.Project;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.MemberAlreadyActiveException;
import com.pantheon.service.exception.NotProjectAdminException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.exception.ProjectLimitExceededException;
import com.pantheon.service.exception.ProjectNotFoundException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.TeamInvitationCreatedEvent;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.MembershipInvitationRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.repository.ProjectRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private static final Duration TRIAL_DURATION = Duration.ofDays(3);
    private static final long PLAN_VALIDITY_DAYS = 365L;
    private static final Duration INVITATION_TTL = Duration.ofDays(14);

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final MembershipInvitationRepository invitationRepository;
    private final UserProfileService userProfileService;
    private final EventPublisher eventPublisher;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMembershipRepository membershipRepository,
            AppUserRepository userRepository,
            MembershipInvitationRepository invitationRepository,
            UserProfileService userProfileService,
            EventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.userProfileService = userProfileService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Project create(UUID creatorId, ProjectRegistrationRequest data) {
        assertWithinPlanLimits(creatorId);
        userProfileService.upsert(creatorId, data.cnpjCpf(), data.legalName(), data.address(), data.postalCode());

        Instant now = Instant.now();
        Project project = new Project(UUID.randomUUID(), data.projectName(), creatorId, now, now.plus(TRIAL_DURATION));
        projectRepository.save(project);

        membershipRepository.save(
                new ProjectMembership(UUID.randomUUID(), project.getId(), creatorId, ProjectRole.ADMIN, now));

        return project;
    }

    private void assertWithinPlanLimits(UUID creatorId) {
        Map<Plan, Long> countsByPlan = projectRepository.findByCreatedBy(creatorId).stream()
                .filter(p -> p.getPlan() != null)
                .collect(Collectors.groupingBy(Project::getPlan, Collectors.counting()));

        for (Map.Entry<Plan, Long> entry : countsByPlan.entrySet()) {
            Integer limit = entry.getKey().getProjectLimit();
            if (limit != null && entry.getValue() >= limit) {
                throw new ProjectLimitExceededException(entry.getKey());
            }
        }
    }

    @Transactional
    public Project confirmPlan(UUID projectId, UUID actingUserId, Plan plan) {
        requireAdmin(projectId, actingUserId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Instant now = Instant.now();
        project.confirmPlan(plan, now, now.plus(PLAN_VALIDITY_DAYS, ChronoUnit.DAYS));
        return projectRepository.save(project);
    }

    /**
     * Adds a team member by email as an <em>invitation</em>. The membership is created
     * immediately in the {@code INVITED} state and confers no project access until the
     * invited person accepts. If the email has no account, a pre-registration account is
     * created for it. Publishes a {@code team-invitation-created} event so the invitation
     * email is sent.
     */
    @Transactional
    public MembershipInvitation addMember(
            UUID projectId, UUID actingUserId, String memberEmail, ConstructionFunction function, String specialty) {
        requireAdmin(projectId, actingUserId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        AppUser inviter = userRepository.findById(actingUserId)
                .orElseThrow(() -> new NotProjectAdminException(projectId));

        Instant now = Instant.now();

        AppUser existingUser = userRepository.findByEmail(memberEmail).orElse(null);
        boolean preRegistered = existingUser == null;
        final AppUser member = preRegistered
                ? userRepository.save(AppUser.preRegistration(UUID.randomUUID(), memberEmail, now))
                : existingUser;

        Optional<ProjectMembership> existing =
                membershipRepository.findByProjectIdAndUserId(projectId, member.getId());
        if (existing.isPresent()) {
            ProjectMembership membership = existing.get();
            if (membership.isActive()) {
                throw new MemberAlreadyActiveException(memberEmail);
            }
            MembershipInvitation invitation = invitationRepository.findByMembershipId(membership.getId())
                    .orElseGet(() -> new MembershipInvitation(
                            UUID.randomUUID(),
                            membership.getId(),
                            memberEmail,
                            "",
                            actingUserId,
                            member.isPendingRegistration(),
                            now,
                            now.plus(INVITATION_TTL)));
            String rawToken = InvitationToken.generate();
            invitation.reissue(InvitationToken.hash(rawToken), now, now.plus(INVITATION_TTL));
            invitationRepository.save(invitation);
            publishInvitationCreated(invitation, rawToken, project, inviter);
            return invitation;
        }

        ConstructionFunction resolvedFunction = function != null ? function : ConstructionFunction.OTHER;
        String resolvedSpecialty = resolvedFunction == ConstructionFunction.SERVICE_PROVIDER ? specialty : null;

        ProjectMembership membership = membershipRepository.save(ProjectMembership.invited(
                UUID.randomUUID(), projectId, member.getId(), resolvedFunction, resolvedSpecialty, now));

        String rawToken = InvitationToken.generate();
        MembershipInvitation invitation = invitationRepository.save(new MembershipInvitation(
                UUID.randomUUID(),
                membership.getId(),
                memberEmail,
                InvitationToken.hash(rawToken),
                actingUserId,
                preRegistered,
                now,
                now.plus(INVITATION_TTL)));

        publishInvitationCreated(invitation, rawToken, project, inviter);
        return invitation;
    }

    private void publishInvitationCreated(
            MembershipInvitation invitation, String rawToken, Project project, AppUser inviter) {
        eventPublisher.publish(
                TeamInvitationCreatedEvent.TYPE,
                new TeamInvitationCreatedEvent(
                        invitation.getId(),
                        invitation.getEmail(),
                        rawToken,
                        project.getId(),
                        project.getName(),
                        inviter.getDisplayName(),
                        invitation.isRequiresRegistration()));
    }

    public List<ProjectMemberResponse> listMembers(UUID projectId, UUID actingUserId) {
        requireMembership(projectId, actingUserId);

        List<ProjectMembership> memberships = membershipRepository.findByProjectId(projectId);
        Map<UUID, AppUser> usersById = userRepository
                .findAllById(memberships.stream().map(ProjectMembership::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(AppUser::getId, u -> u));

        return memberships.stream()
                .map(m -> {
                    AppUser user = usersById.get(m.getUserId());
                    return new ProjectMemberResponse(
                            m.getId(),
                            user.getId(),
                            user.getEmail(),
                            user.getDisplayName(),
                            m.getRole(),
                            m.getFunction(),
                            m.getSpecialty(),
                            m.getStatus(),
                            !m.isActive());
                })
                .toList();
    }

    public List<ProjectMembershipResponse> listMyProjects(UUID userId) {
        List<ProjectMembership> memberships = membershipRepository.findByUserId(userId).stream()
                .filter(ProjectMembership::isActive)
                .toList();
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<UUID, Project> projectsById = projectsById(memberships);
        Instant now = Instant.now();
        return memberships.stream()
                .map(m -> {
                    Project project = projectsById.get(m.getProjectId());
                    return new ProjectMembershipResponse(
                            project.getId(),
                            project.getName(),
                            m.getRole(),
                            project.isActive(now),
                            m.getFunction(),
                            m.getSpecialty());
                })
                .toList();
    }

    public OnboardingStatusResponse getOnboardingStatus(UUID userId) {
        List<ProjectMembership> memberships = membershipRepository.findByUserId(userId).stream()
                .filter(ProjectMembership::isActive)
                .toList();
        if (memberships.isEmpty()) {
            return new OnboardingStatusResponse(false, null, false, null);
        }

        Map<UUID, Project> projectsById = projectsById(memberships);
        Instant now = Instant.now();

        for (ProjectMembership membership : memberships) {
            Project project = projectsById.get(membership.getProjectId());
            if (project.isActive(now)) {
                return new OnboardingStatusResponse(
                        true, new ActiveProjectResponse(project.getId(), membership.getRole()), false, null);
            }
        }

        UUID expiredAdminProjectId = memberships.stream()
                .filter(m -> m.getRole() == ProjectRole.ADMIN)
                .map(ProjectMembership::getProjectId)
                .findFirst()
                .orElse(null);

        return new OnboardingStatusResponse(true, null, true, expiredAdminProjectId);
    }

    private void requireAdmin(UUID projectId, UUID userId) {
        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(ProjectMembership::isActive)
                .orElseThrow(() -> new NotProjectAdminException(projectId));
        if (membership.getRole() != ProjectRole.ADMIN) {
            throw new NotProjectAdminException(projectId);
        }
    }

    private void requireMembership(UUID projectId, UUID userId) {
        membershipRepository
                .findByProjectIdAndUserId(projectId, userId)
                .filter(ProjectMembership::isActive)
                .orElseThrow(() -> new NotProjectMemberException(projectId));
    }

    private Map<UUID, Project> projectsById(List<ProjectMembership> memberships) {
        List<UUID> projectIds = memberships.stream().map(ProjectMembership::getProjectId).toList();
        return projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p));
    }
}
