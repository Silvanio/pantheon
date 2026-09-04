package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
class ConstructionSiteServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private ConstructionSiteService constructionSiteService;

    @BeforeEach
    void setUp() {
        constructionSiteService = new ConstructionSiteService(siteRepository, membershipRepository);
        lenient().when(siteRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ConstructionSiteRegistrationRequest registrationRequest() {
        return new ConstructionSiteRegistrationRequest(
                "Edifício Aurora", "Rua das Flores, 200", LocalDate.now(), null);
    }

    @Test
    void createPersistsSiteInPlanningStatus() {
        UUID projectId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ProjectMembership admin =
                new ProjectMembership(UUID.randomUUID(), projectId, adminId, ProjectRole.ADMIN, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, adminId)).thenReturn(Optional.of(admin));

        ConstructionSite site = constructionSiteService.create(projectId, adminId, registrationRequest());

        assertThat(site.getProjectId()).isEqualTo(projectId);
        assertThat(site.getName()).isEqualTo("Edifício Aurora");
        assertThat(site.getStatus()).isEqualTo(SiteStatus.PLANNING);
    }

    @Test
    void createRejectedForNonAdmin() {
        UUID projectId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, memberId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, memberId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> constructionSiteService.create(projectId, memberId, registrationRequest()))
                .isInstanceOf(NotProjectAdminException.class);
    }

    @Test
    void updateStatusTransitionsToSubmittedValue() {
        UUID projectId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Edifício Aurora", "Rua das Flores, 200", LocalDate.now(), null,
                adminId, Instant.now());
        ProjectMembership admin =
                new ProjectMembership(UUID.randomUUID(), projectId, adminId, ProjectRole.ADMIN, Instant.now());
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        when(membershipRepository.findByProjectIdAndUserId(projectId, adminId)).thenReturn(Optional.of(admin));

        ConstructionSite updated =
                constructionSiteService.updateStatus(site.getId(), adminId, SiteStatus.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(SiteStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusRejectedWhenSiteDoesNotExist() {
        UUID siteId = UUID.randomUUID();
        when(siteRepository.findById(siteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> constructionSiteService.updateStatus(siteId, UUID.randomUUID(), SiteStatus.PAUSED))
                .isInstanceOf(ConstructionSiteNotFoundException.class);
    }

    @Test
    void listRejectedForNonMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> constructionSiteService.list(projectId, userId))
                .isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    void listReturnsSitesForMember() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectMembership member =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(member));
        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Edifício Aurora", "Rua das Flores, 200", LocalDate.now(), null,
                userId, Instant.now());
        when(siteRepository.findByProjectId(projectId)).thenReturn(List.of(site));

        List<ConstructionSite> sites = constructionSiteService.list(projectId, userId);

        assertThat(sites).containsExactly(site);
    }
}
