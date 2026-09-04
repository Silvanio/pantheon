package com.pantheon.service.service;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.entity.SiteStatus;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NotProjectAdminException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionSiteService {

    private final ConstructionSiteRepository siteRepository;
    private final ProjectMembershipRepository membershipRepository;

    public ConstructionSiteService(
            ConstructionSiteRepository siteRepository, ProjectMembershipRepository membershipRepository) {
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ConstructionSite create(UUID projectId, UUID actingUserId, ConstructionSiteRegistrationRequest request) {
        requireAdmin(projectId, actingUserId);

        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(),
                projectId,
                request.name(),
                request.address(),
                request.startDate(),
                request.expectedEndDate(),
                actingUserId,
                Instant.now());
        return siteRepository.save(site);
    }

    @Transactional
    public ConstructionSite updateStatus(UUID siteId, UUID actingUserId, SiteStatus newStatus) {
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        requireAdmin(site.getProjectId(), actingUserId);

        site.updateStatus(newStatus, Instant.now());
        return siteRepository.save(site);
    }

    public List<ConstructionSite> list(UUID projectId, UUID actingUserId) {
        requireMembership(projectId, actingUserId);
        return siteRepository.findByProjectId(projectId);
    }

    private void requireAdmin(UUID projectId, UUID userId) {
        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotProjectAdminException(projectId));
        if (membership.getRole() != ProjectRole.ADMIN) {
            throw new NotProjectAdminException(projectId);
        }
    }

    private void requireMembership(UUID projectId, UUID userId) {
        membershipRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotProjectMemberException(projectId));
    }
}
