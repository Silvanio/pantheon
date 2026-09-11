package com.pantheon.service.service;

import com.pantheon.service.dto.EquipmentRegistrationRequest;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.EquipmentNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.EquipmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.equipmentRepository = equipmentRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    @Transactional
    public Equipment create(UUID siteId, UUID actingUserId, EquipmentRegistrationRequest request) {
        requireSite(siteId);
        requireManage(siteId, actingUserId);

        Equipment equipment = new Equipment(
                UUID.randomUUID(), siteId, request.name(), request.type(), request.status(), actingUserId,
                Instant.now());
        return equipmentRepository.save(equipment);
    }

    @Transactional
    public Equipment updateStatus(UUID equipmentId, UUID actingUserId, EquipmentStatus newStatus) {
        Equipment equipment =
                equipmentRepository.findById(equipmentId).orElseThrow(() -> new EquipmentNotFoundException(equipmentId));
        requireManage(equipment.getConstructionSiteId(), actingUserId);

        equipment.updateStatus(newStatus, Instant.now());
        return equipmentRepository.save(equipment);
    }

    public List<Equipment> list(UUID siteId, UUID actingUserId) {
        requireSite(siteId);
        siteAccessService.requireAccess(siteId, actingUserId);
        return equipmentRepository.findByConstructionSiteId(siteId);
    }

    private void requireSite(UUID siteId) {
        siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.EQUIPMENT);
    }
}
