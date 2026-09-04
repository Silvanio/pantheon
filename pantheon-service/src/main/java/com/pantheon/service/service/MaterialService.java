package com.pantheon.service.service;

import com.pantheon.service.dto.MaterialRegistrationRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NotConstructionSiteManagerException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ConstructionSiteRepository siteRepository;
    private final ProjectMembershipRepository membershipRepository;

    public MaterialService(
            MaterialRepository materialRepository,
            ConstructionSiteRepository siteRepository,
            ProjectMembershipRepository membershipRepository) {
        this.materialRepository = materialRepository;
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public Material create(UUID siteId, UUID actingUserId, MaterialRegistrationRequest request) {
        ConstructionSite site = requireSite(siteId);
        requireManager(site.getProjectId(), actingUserId);

        Material material =
                new Material(UUID.randomUUID(), siteId, request.name(), request.unit(), actingUserId, Instant.now());
        return materialRepository.save(material);
    }

    public List<Material> list(UUID siteId, UUID actingUserId) {
        ConstructionSite site = requireSite(siteId);
        requireMembership(site.getProjectId(), actingUserId);
        return materialRepository.findByConstructionSiteId(siteId);
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireManager(UUID projectId, UUID userId) {
        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotConstructionSiteManagerException(projectId));
        boolean isManager =
                membership.getRole() == ProjectRole.ADMIN || membership.getFunction() == ConstructionFunction.SITE_FOREMAN;
        if (!isManager) {
            throw new NotConstructionSiteManagerException(projectId);
        }
    }

    private void requireMembership(UUID projectId, UUID userId) {
        membershipRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotProjectMemberException(projectId));
    }
}
