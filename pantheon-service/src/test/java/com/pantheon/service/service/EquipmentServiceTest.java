package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.EquipmentRegistrationRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.EquipmentNotFoundException;
import com.pantheon.service.exception.NotConstructionSiteManagerException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.EquipmentRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private EquipmentService equipmentService;

    @BeforeEach
    void setUp() {
        equipmentService = new EquipmentService(equipmentRepository, siteRepository, membershipRepository);
        lenient().when(equipmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ConstructionSite site(UUID projectId) {
        return new ConstructionSite(
                UUID.randomUUID(), projectId, "Torre Norte", "Av. Central, 500", LocalDate.now(), null,
                UUID.randomUUID(), Instant.now());
    }

    @Test
    void createAllowedForAdmin() {
        UUID projectId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership admin =
                new ProjectMembership(UUID.randomUUID(), projectId, adminId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, adminId)).thenReturn(Optional.of(admin));

        Equipment equipment = equipmentService.create(
                site.getId(), adminId, new EquipmentRegistrationRequest("Betoneira", "Concreto", EquipmentStatus.AVAILABLE));

        assertThat(equipment.getName()).isEqualTo("Betoneira");
        assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.AVAILABLE);
    }

    @Test
    void createAllowedForSiteForeman() {
        UUID projectId = UUID.randomUUID();
        UUID foremanId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership foreman = new ProjectMembership(
                UUID.randomUUID(), projectId, foremanId, ProjectRole.MEMBER, ConstructionFunction.SITE_FOREMAN, null,
                Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, foremanId)).thenReturn(Optional.of(foreman));

        Equipment equipment = equipmentService.create(
                site.getId(), foremanId, new EquipmentRegistrationRequest("Betoneira", null, EquipmentStatus.AVAILABLE));

        assertThat(equipment).isNotNull();
    }

    @Test
    void createRejectedForRegularMember() {
        UUID projectId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, memberId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, memberId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> equipmentService.create(
                        site.getId(), memberId, new EquipmentRegistrationRequest("Betoneira", null, EquipmentStatus.AVAILABLE)))
                .isInstanceOf(NotConstructionSiteManagerException.class);
    }

    @Test
    void updateStatusUpdatesEquipment() {
        UUID projectId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        Equipment equipment = new Equipment(
                UUID.randomUUID(), site.getId(), "Betoneira", null, EquipmentStatus.AVAILABLE, adminId, Instant.now());
        when(equipmentRepository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership admin =
                new ProjectMembership(UUID.randomUUID(), projectId, adminId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, adminId)).thenReturn(Optional.of(admin));

        Equipment updated = equipmentService.updateStatus(equipment.getId(), adminId, EquipmentStatus.MAINTENANCE);

        assertThat(updated.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
    }

    @Test
    void updateStatusRejectedWhenEquipmentDoesNotExist() {
        UUID equipmentId = UUID.randomUUID();
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.updateStatus(equipmentId, UUID.randomUUID(), EquipmentStatus.IN_USE))
                .isInstanceOf(EquipmentNotFoundException.class);
    }

    @Test
    void listAllowedForAnyMember() {
        UUID projectId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, memberId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, memberId)).thenReturn(Optional.of(member));
        Equipment equipment = new Equipment(
                UUID.randomUUID(), site.getId(), "Betoneira", null, EquipmentStatus.AVAILABLE, memberId, Instant.now());
        when(equipmentRepository.findByConstructionSiteId(site.getId())).thenReturn(List.of(equipment));

        List<Equipment> equipmentList = equipmentService.list(site.getId(), memberId);

        assertThat(equipmentList).containsExactly(equipment);
    }

    @Test
    void listRejectedForNonMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipmentService.list(site.getId(), userId)).isInstanceOf(NotProjectMemberException.class);
    }
}
