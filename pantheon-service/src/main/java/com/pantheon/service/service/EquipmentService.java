package com.pantheon.service.service;

import com.pantheon.service.dto.EquipmentRegistrationRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.EquipmentNotFoundException;
import com.pantheon.service.exception.NotConstructionSiteManagerException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.EquipmentRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ConstructionSiteRepository siteRepository;
    private final ProjectMembershipRepository membershipRepository;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            ConstructionSiteRepository siteRepository,
            ProjectMembershipRepository membershipRepository) {
        this.equipmentRepository = equipmentRepository;
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public Equipment create(UUID siteId, UUID actingUserId, EquipmentRegistrationRequest request) {
        ConstructionSite site = requireSite(siteId);
        requireManager(site.getProjectId(), actingUserId);

        Equipment equipment = new Equipment(
                UUID.randomUUID(), siteId, request.name(), request.type(), request.status(), actingUserId,
                Instant.now());
        return equipmentRepository.save(equipment);
    }

    @Transactional
    public Equipment updateStatus(UUID equipmentId, UUID actingUserId, EquipmentStatus newStatus) {
        Equipment equipment =
                equipmentRepository.findById(equipmentId).orElseThrow(() -> new EquipmentNotFoundException(equipmentId));
        ConstructionSite site = requireSite(equipment.getConstructionSiteId());
        requireManager(site.getProjectId(), actingUserId);

        equipment.updateStatus(newStatus, Instant.now());
        return equipmentRepository.save(equipment);
    }

    public List<Equipment> list(UUID siteId, UUID actingUserId) {
        ConstructionSite site = requireSite(siteId);
        requireMembership(site.getProjectId(), actingUserId);
        return equipmentRepository.findByConstructionSiteId(siteId);
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
