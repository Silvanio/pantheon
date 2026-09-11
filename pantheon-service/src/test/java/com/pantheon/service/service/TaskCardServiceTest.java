package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.MoveTaskCardRequest;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.TaskColumnNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
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
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private TaskCardService service;

    private UUID siteId;
    private UUID companyId;
    private UUID columnId;

    @BeforeEach
    void setUp() {
        service = new TaskCardService(
                cardRepository, columnRepository, cardLabelRepository, siteRepository, siteAccessService, permissionService);

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

        TaskCard card = service.createCard(siteId, UUID.randomUUID(), new TaskCardCreationRequest(columnId, "Entregar telhas", null));

        assertThat(card.getConstructionSiteId()).isEqualTo(siteId);
        assertThat(card.getColumnId()).isEqualTo(columnId);
        assertThat(card.getSortOrder()).isEqualTo(2);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }

    @Test
    void createCardRejectsColumnFromAnotherCompany() {
        UUID otherCompanyColumnId = UUID.randomUUID();
        when(columnRepository.findById(otherCompanyColumnId))
                .thenReturn(Optional.of(new TaskColumn(otherCompanyColumnId, UUID.randomUUID(), "Outra", 0, Instant.now())));

        assertThatThrownBy(() -> service.createCard(
                siteId, UUID.randomUUID(), new TaskCardCreationRequest(otherCompanyColumnId, "Card", null)))
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

        TaskCardService.TaskBoard board = service.getBoard(otherSiteId, UUID.randomUUID());

        assertThat(board.cards()).isEmpty();
    }

    @Test
    void moveCardUpdatesColumnAndSortOrder() {
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, columnId, "Card", null, 0, UUID.randomUUID(), Instant.now(), Instant.now());
        when(cardRepository.findById(card.getId())).thenReturn(Optional.of(card));
        UUID newColumnId = UUID.randomUUID();
        when(columnRepository.findById(newColumnId))
                .thenReturn(Optional.of(new TaskColumn(newColumnId, companyId, "Concluido", 1, Instant.now())));

        TaskCard moved = service.moveCard(card.getId(), UUID.randomUUID(), new MoveTaskCardRequest(newColumnId, 3));

        assertThat(moved.getColumnId()).isEqualTo(newColumnId);
        assertThat(moved.getSortOrder()).isEqualTo(3);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }
}
