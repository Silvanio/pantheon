package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteDocumentProjectAttachmentRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskCardAssigneeRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import com.pantheon.service.repository.TaskLabelRepository;
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
class GlobalTaskBoardServiceTest {

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private TaskColumnRepository columnRepository;

    @Mock
    private TaskCardRepository cardRepository;

    @Mock
    private TaskCardLabelRepository cardLabelRepository;

    @Mock
    private TaskLabelRepository labelRepository;

    @Mock
    private CompanyMembershipRepository membershipRepository;

    @Mock
    private PlatformAdminService platformAdminService;

    @Mock
    private TaskCardAssigneeRepository assigneeRepository;

    @Mock
    private TaskCommentRepository commentRepository;

    @Mock
    private SiteDocumentProjectAttachmentRepository attachmentRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private AppUserRepository userRepository;

    private GlobalTaskBoardService service;

    private UUID companyId;
    private UUID adminUserId;
    private UUID memberUserId;

    @BeforeEach
    void setUp() {
        service = new GlobalTaskBoardService(
                siteRepository, columnRepository, cardRepository, cardLabelRepository, labelRepository,
                membershipRepository, platformAdminService, assigneeRepository, commentRepository,
                attachmentRepository, siteMembershipRepository, userRepository);
        companyId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        memberUserId = UUID.randomUUID();

        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, adminUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, adminUserId, CompanyRole.ADMIN, Instant.now())));
        lenient().when(membershipRepository.findByCompanyIdAndUserId(companyId, memberUserId)).thenReturn(
                Optional.of(new CompanyMembership(UUID.randomUUID(), companyId, memberUserId, CompanyRole.MEMBER, Instant.now())));
        lenient().when(platformAdminService.isSuperAdmin(any())).thenReturn(false);
        lenient().when(assigneeRepository.findByCardIdIn(any())).thenReturn(List.of());
        lenient().when(commentRepository.countByCardIdIn(any())).thenReturn(List.of());
        lenient().when(attachmentRepository.countByTaskCardIdIn(any())).thenReturn(List.of());
        lenient().when(siteMembershipRepository.findAllById(any())).thenReturn(List.of());
    }

    @Test
    void superAdminCanAccessGlobalBoardWithoutMembership() {
        UUID superAdminId = UUID.randomUUID();
        when(platformAdminService.isSuperAdmin(superAdminId)).thenReturn(true);
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        when(cardRepository.findByConstructionSiteIdInOrderBySortOrderAsc(List.of())).thenReturn(List.of());
        when(cardLabelRepository.findByCardIdIn(List.of())).thenReturn(List.of());
        when(labelRepository.findByCompanyIdOrderByNameAsc(companyId)).thenReturn(List.of());

        var result = service.build(companyId, superAdminId);

        assertThat(result.columns()).isEmpty();
    }

    @Test
    void nonAdminBlockedFromGlobalBoard() {
        assertThatThrownBy(() -> service.build(companyId, memberUserId)).isInstanceOf(NotCompanyAdminException.class);
    }

    @Test
    void adminSeesCardsFromEverySite() {
        UUID siteAId = UUID.randomUUID();
        UUID siteBId = UUID.randomUUID();
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(
                site(siteAId, companyId, "Obra A"), site(siteBId, companyId, "Obra B")));
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        TaskCard cardA = new TaskCard(UUID.randomUUID(), siteAId, UUID.randomUUID(), "Card A", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        TaskCard cardB = new TaskCard(UUID.randomUUID(), siteBId, UUID.randomUUID(), "Card B", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findByConstructionSiteIdInOrderBySortOrderAsc(any())).thenReturn(List.of(cardA, cardB));
        when(cardLabelRepository.findByCardIdIn(any())).thenReturn(List.of());
        when(labelRepository.findByCompanyIdOrderByNameAsc(companyId)).thenReturn(List.of());

        var board = service.build(companyId, adminUserId);

        assertThat(board.cards()).containsExactly(cardA, cardB);
        assertThat(board.siteById()).containsKeys(siteAId, siteBId);
    }

    @Test
    void boardIncludesLabelCatalogAcrossSites() {
        UUID siteAId = UUID.randomUUID();
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(site(siteAId, companyId, "Obra A")));
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        when(cardRepository.findByConstructionSiteIdInOrderBySortOrderAsc(any())).thenReturn(List.of());
        when(cardLabelRepository.findByCardIdIn(any())).thenReturn(List.of());
        var label = com.pantheon.service.entity.TaskLabel.predefined(
                UUID.randomUUID(), companyId, "Urgente", "#EF4444", Instant.now());
        when(labelRepository.findByCompanyIdOrderByNameAsc(companyId)).thenReturn(List.of(label));

        var board = service.build(companyId, adminUserId);

        assertThat(board.labels()).containsExactly(label);
    }

    @Test
    void boardResolvesAssigneesCommentsAndAttachmentsAcrossSites() {
        UUID siteAId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID assigneeUserId = UUID.randomUUID();
        when(siteRepository.findByCompanyId(companyId)).thenReturn(List.of(site(siteAId, companyId, "Obra A")));
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        TaskCard card = new TaskCard(UUID.randomUUID(), siteAId, UUID.randomUUID(), "Card A", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findByConstructionSiteIdInOrderBySortOrderAsc(any())).thenReturn(List.of(card));
        when(cardLabelRepository.findByCardIdIn(any())).thenReturn(List.of());
        when(labelRepository.findByCompanyIdOrderByNameAsc(companyId)).thenReturn(List.of());

        var assignee = new com.pantheon.service.entity.TaskCardAssignee(UUID.randomUUID(), card.getId(), membershipId, Instant.now());
        when(assigneeRepository.findByCardIdIn(any())).thenReturn(List.of(assignee));

        var membership = com.pantheon.service.entity.SiteMembership.invited(
                membershipId, siteAId, assigneeUserId, com.pantheon.service.entity.ConstructionFunction.ENGINEER, null, null, Instant.now());
        membership.accept();
        when(siteMembershipRepository.findAllById(List.of(membershipId))).thenReturn(List.of(membership));

        var assigneeUser = new com.pantheon.service.entity.AppUser(
                assigneeUserId, "eng@example.com", "Engenheiro", null, null, Instant.now(), Instant.now());
        when(userRepository.findAllById(List.of(assigneeUserId))).thenReturn(List.of(assigneeUser));

        var board = service.build(companyId, adminUserId);

        assertThat(board.assigneeIdsByCard().get(card.getId())).containsExactly(membershipId);
        assertThat(board.assignees()).hasSize(1);
        assertThat(board.assignees().get(0).displayName()).isEqualTo("Engenheiro");
    }

    @Test
    void colorForIsStableAcrossCalls() {
        UUID siteId = UUID.randomUUID();

        assertThat(GlobalTaskBoardService.colorFor(siteId)).isEqualTo(GlobalTaskBoardService.colorFor(siteId));
    }

    private ConstructionSite site(UUID id, UUID companyId, String name) {
        return new ConstructionSite(id, companyId, name, "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }
}
