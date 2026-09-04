package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.MaterialRegistrationRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.NotConstructionSiteManagerException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRepository;
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
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private MaterialService materialService;

    @BeforeEach
    void setUp() {
        materialService = new MaterialService(materialRepository, siteRepository, membershipRepository);
        lenient().when(materialRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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

        Material material = materialService.create(site.getId(), adminId, new MaterialRegistrationRequest("Cimento", "sc"));

        assertThat(material.getName()).isEqualTo("Cimento");
        assertThat(material.getUnit()).isEqualTo("sc");
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

        Material material =
                materialService.create(site.getId(), foremanId, new MaterialRegistrationRequest("Cimento", "sc"));

        assertThat(material).isNotNull();
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

        assertThatThrownBy(() -> materialService.create(site.getId(), memberId, new MaterialRegistrationRequest("Cimento", "sc")))
                .isInstanceOf(NotConstructionSiteManagerException.class);
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
        Material material = new Material(UUID.randomUUID(), site.getId(), "Cimento", "sc", memberId, Instant.now());
        when(materialRepository.findByConstructionSiteId(site.getId())).thenReturn(List.of(material));

        List<Material> materials = materialService.list(site.getId(), memberId);

        assertThat(materials).containsExactly(material);
    }

    @Test
    void listRejectedForNonMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConstructionSite site = site(projectId);
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.list(site.getId(), userId)).isInstanceOf(NotProjectMemberException.class);
    }
}
