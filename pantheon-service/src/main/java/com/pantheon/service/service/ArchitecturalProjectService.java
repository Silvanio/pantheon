package com.pantheon.service.service;

import com.pantheon.service.dto.ArchitecturalProjectRegistrationRequest;
import com.pantheon.service.entity.ArchitecturalProject;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ArchitecturalProjectRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArchitecturalProjectService {

    private final ArchitecturalProjectRepository architecturalProjectRepository;
    private final ProjectMembershipRepository membershipRepository;

    public ArchitecturalProjectService(
            ArchitecturalProjectRepository architecturalProjectRepository,
            ProjectMembershipRepository membershipRepository) {
        this.architecturalProjectRepository = architecturalProjectRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ArchitecturalProject create(
            UUID projectId, UUID actingUserId, ArchitecturalProjectRegistrationRequest request) {
        requireMembership(projectId, actingUserId);

        ArchitecturalProject architecturalProject = new ArchitecturalProject(
                UUID.randomUUID(),
                projectId,
                request.constructionSiteId(),
                request.name(),
                request.description(),
                actingUserId,
                Instant.now());
        return architecturalProjectRepository.save(architecturalProject);
    }

    public List<ArchitecturalProject> list(UUID projectId, UUID actingUserId) {
        requireMembership(projectId, actingUserId);
        return architecturalProjectRepository.findByProjectId(projectId);
    }

    private void requireMembership(UUID projectId, UUID userId) {
        membershipRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotProjectMemberException(projectId));
    }
}
