package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.ArchitecturalProjectRegistrationRequest;
import com.pantheon.service.entity.ArchitecturalProject;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ArchitecturalProjectRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
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
class ArchitecturalProjectServiceTest {

    @Mock
    private ArchitecturalProjectRepository architecturalProjectRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private ArchitecturalProjectService architecturalProjectService;

    @BeforeEach
    void setUp() {
        architecturalProjectService =
                new ArchitecturalProjectService(architecturalProjectRepository, membershipRepository);
        lenient()
                .when(architecturalProjectRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createPersistsProjectWithoutSiteLink() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        ArchitecturalProject created = architecturalProjectService.create(
                projectId, userId, new ArchitecturalProjectRegistrationRequest("Projeto Fase 1", "Descrição", null));

        assertThat(created.getProjectId()).isEqualTo(projectId);
        assertThat(created.getConstructionSiteId()).isNull();
        assertThat(created.getName()).isEqualTo("Projeto Fase 1");
    }

    @Test
    void createPersistsProjectWithSiteLink() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID siteId = UUID.randomUUID();
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));

        ArchitecturalProject created = architecturalProjectService.create(
                projectId, userId, new ArchitecturalProjectRegistrationRequest("Projeto Fase 1", null, siteId));

        assertThat(created.getConstructionSiteId()).isEqualTo(siteId);
    }

    @Test
    void createRejectedForNonMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> architecturalProjectService.create(
                        projectId, userId, new ArchitecturalProjectRegistrationRequest("Projeto Fase 1", null, null)))
                .isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    void listReturnsProjectsForMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));
        ArchitecturalProject project = new ArchitecturalProject(
                UUID.randomUUID(), projectId, null, "Projeto Fase 1", null, userId, Instant.now());
        when(architecturalProjectRepository.findByProjectId(projectId)).thenReturn(List.of(project));

        List<ArchitecturalProject> projects = architecturalProjectService.list(projectId, userId);

        assertThat(projects).containsExactly(project);
    }
}
