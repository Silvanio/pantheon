package com.pantheon.service.service;

import com.pantheon.service.dto.MaterialRegistrationRequest;
import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public MaterialService(
            MaterialRepository materialRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.materialRepository = materialRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    @Transactional
    public Material create(UUID siteId, UUID actingUserId, MaterialRegistrationRequest request) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireManage(siteId, access, PermissionCapability.EQUIPMENT_MATERIAL);

        Material material =
                new Material(UUID.randomUUID(), siteId, request.name(), request.unit(), actingUserId, Instant.now());
        return materialRepository.save(material);
    }

    public List<Material> list(UUID siteId, UUID actingUserId) {
        requireSite(siteId);
        siteAccessService.requireAccess(siteId, actingUserId);
        return materialRepository.findByConstructionSiteId(siteId);
    }

    private void requireSite(UUID siteId) {
        siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }
}
