package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.MoveTaskCardRequest;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.dto.UpdateTaskCardDueDateRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardAssignee;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.SiteMembershipNotFoundException;
import com.pantheon.service.exception.TaskColumnNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskCardAssigneeRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import com.pantheon.service.sse.SseEventPublisher;
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
class TaskCardServiceTest {

    @Mock
    private TaskCardRepository cardRepository;

    @Mock
    private TaskColumnRepository columnRepository;

    @Mock
    private TaskCardLabelRepository cardLabelRepository;

    @Mock
    private TaskLabelRepository labelRepository;

    @Mock
    private TaskCommentRepository commentRepository;

    @Mock
    private TaskCardAssigneeRepository assigneeRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private SseEventPublisher sseEventPublisher;

    private TaskCardService service;

    private UUID siteId;
    private UUID companyId;
    private UUID columnId;

    @BeforeEach
    void setUp() {
        service = new TaskCardService(
                cardRepository, columnRepository, cardLabelRepository, labelRepository, commentRepository,
                assigneeRepository, siteRepository, siteMembershipRepository, siteAccessService, permissionService,
                sseEventPublisher);

        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        columnId = UUID.randomUUID();

        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(new ConstructionSite(
                siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));
        lenient().when(columnRepository.findById(columnId))
                .thenReturn(Optional.of(new TaskColumn(columnId, companyId, "A Fazer", 0, Instant.now())));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(cardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createCardPersistsInColumnWithEndOfColumnSortOrder() {
        when(cardRepository.countByConstructionSiteIdAndColumnId(siteId, columnId)).thenReturn(2L);

        TaskCard card = service.createCard(
                siteId, UUID.randomUUID(), new TaskCardCreationRequest(columnId, "Entregar telhas", null, null));

        assertThat(card.getConstructionSiteId()).isEqualTo(siteId);
        assertThat(card.getColumnId()).isEqualTo(columnId);
        assertThat(card.getSortOrder()).isEqualTo(2);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }

    @Test
    void createCardPersistsDueDate() {
        when(cardRepository.countByConstructionSiteIdAndColumnId(siteId, columnId)).thenReturn(0L);
        LocalDate dueDate = LocalDate.now().plusDays(7);

        TaskCard card = service.createCard(
                siteId, UUID.randomUUID(), new TaskCardCreationRequest(columnId, "Entregar telhas", null, dueDate));

        assertThat(card.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void createCardRejectsColumnFromAnotherCompany() {
        UUID otherCompanyColumnId = UUID.randomUUID();
        when(columnRepository.findById(otherCompanyColumnId))
                .thenReturn(Optional.of(new TaskColumn(otherCompanyColumnId, UUID.randomUUID(), "Outra", 0, Instant.now())));

        assertThatThrownBy(() -> service.createCard(
                siteId, UUID.randomUUID(), new TaskCardCreationRequest(otherCompanyColumnId, "Card", null, null)))
                .isInstanceOf(TaskColumnNotFoundException.class);
    }

    @Test
    void cardCreatedInOneSiteIsInvisibleWhenBoardingAnotherSite() {
        UUID otherSiteId = UUID.randomUUID();
        when(siteRepository.findById(otherSiteId)).thenReturn(Optional.of(new ConstructionSite(
                otherSiteId, companyId, "Outra Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));
        when(siteAccessService.requireAccess(eq(otherSiteId), any())).thenReturn(new SiteAccessContext(true, null));
        when(cardRepository.findByConstructionSiteIdOrderBySortOrderAsc(otherSiteId)).thenReturn(List.of());
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        when(cardLabelRepository.findByCardIdIn(List.of())).thenReturn(List.of());
        when(assigneeRepository.findByCardIdIn(List.of())).thenReturn(List.of());
        when(commentRepository.countByCardIdIn(List.of())).thenReturn(List.of());

        TaskCardService.TaskBoard board = service.getBoard(otherSiteId, UUID.randomUUID());

        assertThat(board.cards()).isEmpty();
    }

    @Test
    void moveCardUpdatesColumnAndSortOrder() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        UUID newColumnId = UUID.randomUUID();
        when(columnRepository.findById(newColumnId))
                .thenReturn(Optional.of(new TaskColumn(newColumnId, companyId, "Concluido", 1, Instant.now())));

        TaskCard moved = service.moveCard(card.getId(), UUID.randomUUID(), new MoveTaskCardRequest(newColumnId, 3));

        assertThat(moved.getColumnId()).isEqualTo(newColumnId);
        assertThat(moved.getSortOrder()).isEqualTo(3);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
        verify(sseEventPublisher).publishToCompany(eq(companyId), eq("task-card-moved"), any());
    }

    @Test
    void updateDueDateSetsNewDate() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        LocalDate dueDate = LocalDate.now().plusDays(3);

        TaskCard updated = service.updateDueDate(card.getId(), UUID.randomUUID(), new UpdateTaskCardDueDateRequest(dueDate));

        assertThat(updated.getDueDate()).isEqualTo(dueDate);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }

    @Test
    void updateDueDateClearsExistingDate() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, LocalDate.now().plusDays(1), 0, UUID.randomUUID(),
                Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        TaskCard updated = service.updateDueDate(card.getId(), UUID.randomUUID(), new UpdateTaskCardDueDateRequest(null));

        assertThat(updated.getDueDate()).isNull();
    }

    @Test
    void updateDueDateRejectsCallerWithoutManage() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.TASKS))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));

        assertThatThrownBy(() -> service.updateDueDate(
                card.getId(), UUID.randomUUID(), new UpdateTaskCardDueDateRequest(LocalDate.now())))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void boardIncludesCommentCountsAndAssigneesPerCard() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        UUID membershipId = UUID.randomUUID();
        when(cardRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId)).thenReturn(List.of(card));
        when(columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId)).thenReturn(List.of());
        when(cardLabelRepository.findByCardIdIn(List.of(card.getId()))).thenReturn(List.of());
        when(assigneeRepository.findByCardIdIn(List.of(card.getId())))
                .thenReturn(List.of(new TaskCardAssignee(UUID.randomUUID(), card.getId(), membershipId, Instant.now())));
        TaskCommentRepository.CardCommentCount count = new TaskCommentRepository.CardCommentCount() {
            public UUID getCardId() {
                return card.getId();
            }

            public long getCommentCount() {
                return 3L;
            }
        };
        when(commentRepository.countByCardIdIn(List.of(card.getId()))).thenReturn(List.of(count));

        TaskCardService.TaskBoard board = service.getBoard(siteId, UUID.randomUUID());

        assertThat(board.commentCountByCard()).containsEntry(card.getId(), 3L);
        assertThat(board.assigneeIdsByCard()).containsEntry(card.getId(), List.of(membershipId));
    }

    @Test
    void assignPersistsAssigneeAndRejectsMembershipFromAnotherSite() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        UUID membershipId = UUID.randomUUID();
        SiteMembership membership = SiteMembership.accountless(membershipId, siteId, "Eletricista", "Fulano", null, Instant.now());
        when(siteMembershipRepository.findById(membershipId)).thenReturn(Optional.of(membership));
        when(assigneeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assign(card.getId(), UUID.randomUUID(), membershipId);

        verify(assigneeRepository).save(any());
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));

        UUID otherSiteMembershipId = UUID.randomUUID();
        SiteMembership otherSiteMembership = SiteMembership.accountless(
                otherSiteMembershipId, UUID.randomUUID(), "Pedreiro", "Ciclano", null, Instant.now());
        when(siteMembershipRepository.findById(otherSiteMembershipId)).thenReturn(Optional.of(otherSiteMembership));

        assertThatThrownBy(() -> service.assign(card.getId(), UUID.randomUUID(), otherSiteMembershipId))
                .isInstanceOf(SiteMembershipNotFoundException.class);
    }

    @Test
    void unassignRemovesAssignee() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        UUID membershipId = UUID.randomUUID();
        TaskCardAssignee assignee = new TaskCardAssignee(UUID.randomUUID(), card.getId(), membershipId, Instant.now());
        when(assigneeRepository.findByCardIdAndSiteMembershipId(card.getId(), membershipId)).thenReturn(Optional.of(assignee));

        service.unassign(card.getId(), UUID.randomUUID(), membershipId);

        verify(assigneeRepository).delete(assignee);
    }

    @Test
    void deleteCardRemovesCardAndDependents() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));

        service.deleteCard(card.getId(), UUID.randomUUID());

        verify(cardLabelRepository).deleteByCardId(card.getId());
        verify(labelRepository).deleteByCardId(card.getId());
        verify(commentRepository).deleteByCardId(card.getId());
        verify(assigneeRepository).deleteByCardId(card.getId());
        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCardRejectsCallerWithoutManage() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.TASKS))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));

        assertThatThrownBy(() -> service.deleteCard(card.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }
}
