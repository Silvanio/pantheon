package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.TaskLabelNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskLabelServiceTest {

    @Mock
    private TaskLabelRepository labelRepository;

    @Mock
    private TaskCardRepository cardRepository;

    @Mock
    private TaskCardLabelRepository cardLabelRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private TaskLabelService service;

    private UUID siteId;
    private UUID companyId;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        service = new TaskLabelService(
                labelRepository, cardRepository, cardLabelRepository, siteRepository, siteAccessService, permissionService);
        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        lenient().when(cardRepository.findById(cardId)).thenReturn(Optional.of(
                new TaskCard(cardId, siteId, UUID.randomUUID(), "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now())));
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(new ConstructionSite(
                siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(cardLabelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createCustomPersistsCardScopedLabelAndAutoAttachesIt() {
        TaskLabel label = service.createCustom(cardId, UUID.randomUUID(), new TaskLabelRequest("Prioridade", "#FF0000"));

        assertThat(label.isCustom()).isTrue();
        assertThat(label.getCardId()).isEqualTo(cardId);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
        verify(cardLabelRepository).save(any());
    }

    @Test
    void customLabelFromOneCardIsNeverInAnotherCardsCatalog() {
        TaskLabel custom = service.createCustom(cardId, UUID.randomUUID(), new TaskLabelRequest("Prioridade", "#FF0000"));

        assertThat(custom.isPredefined()).isFalse();
        // A custom label is only ever reachable via its own card's TaskCardLabel join rows —
        // it is never returned by the predefined-catalog query (TaskLabelRepository.findByCompanyIdOrderByNameAsc),
        // which filters on companyId and this label's companyId is null.
        assertThat(custom.getCompanyId()).isNull();
    }

    @Test
    void attachPredefinedSkipsDuplicateWhenAlreadyAttached() {
        UUID labelId = UUID.randomUUID();
        when(labelRepository.findById(labelId))
                .thenReturn(Optional.of(TaskLabel.predefined(labelId, companyId, "Urgente", "#FF0000", Instant.now())));
        when(cardLabelRepository.findByCardIdAndLabelId(cardId, labelId))
                .thenReturn(Optional.of(new TaskCardLabel(UUID.randomUUID(), cardId, labelId)));

        service.attachPredefined(cardId, labelId, UUID.randomUUID());

        verify(cardLabelRepository, never()).save(any());
    }

    @Test
    void attachPredefinedCreatesLinkWhenNotYetAttached() {
        UUID labelId = UUID.randomUUID();
        when(labelRepository.findById(labelId))
                .thenReturn(Optional.of(TaskLabel.predefined(labelId, companyId, "Urgente", "#FF0000", Instant.now())));
        when(cardLabelRepository.findByCardIdAndLabelId(cardId, labelId)).thenReturn(Optional.empty());

        service.attachPredefined(cardId, labelId, UUID.randomUUID());

        verify(cardLabelRepository).save(any());
    }

    @Test
    void attachPredefinedRejectsLabelFromAnotherCompany() {
        UUID labelId = UUID.randomUUID();
        when(labelRepository.findById(labelId)).thenReturn(
                Optional.of(TaskLabel.predefined(labelId, UUID.randomUUID(), "Urgente", "#FF0000", Instant.now())));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.attachPredefined(cardId, labelId, UUID.randomUUID()))
                .isInstanceOf(TaskLabelNotFoundException.class);
    }

    @Test
    void detachRemovesExistingLink() {
        UUID labelId = UUID.randomUUID();
        TaskCardLabel link = new TaskCardLabel(UUID.randomUUID(), cardId, labelId);
        when(cardLabelRepository.findByCardIdAndLabelId(cardId, labelId)).thenReturn(Optional.of(link));

        service.detach(cardId, labelId, UUID.randomUUID());

        verify(cardLabelRepository).delete(link);
    }
}
