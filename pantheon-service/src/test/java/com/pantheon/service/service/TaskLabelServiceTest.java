package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.time.Instant;
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
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private TaskLabelService service;

    private UUID siteId;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        service = new TaskLabelService(labelRepository, cardRepository, cardLabelRepository, siteAccessService, permissionService);
        siteId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        lenient().when(cardRepository.findById(cardId)).thenReturn(Optional.of(
                new TaskCard(cardId, siteId, UUID.randomUUID(), "Card", null, 0, UUID.randomUUID(), Instant.now(), Instant.now())));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(labelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createLabelRequiresManageAccess() {
        service.create(siteId, UUID.randomUUID(), new TaskLabelRequest("Urgente", "#FF0000"));

        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }

    @Test
    void attachSkipsDuplicateWhenAlreadyAttached() {
        UUID labelId = UUID.randomUUID();
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(new TaskLabel(labelId, siteId, "Urgente", "#FF0000", Instant.now())));
        when(cardLabelRepository.findByCardIdAndLabelId(cardId, labelId))
                .thenReturn(Optional.of(new TaskCardLabel(UUID.randomUUID(), cardId, labelId)));

        service.attach(cardId, labelId, UUID.randomUUID());

        verify(cardLabelRepository, never()).save(any());
    }

    @Test
    void attachCreatesLinkWhenNotYetAttached() {
        UUID labelId = UUID.randomUUID();
        when(labelRepository.findById(labelId)).thenReturn(Optional.of(new TaskLabel(labelId, siteId, "Urgente", "#FF0000", Instant.now())));
        when(cardLabelRepository.findByCardIdAndLabelId(cardId, labelId)).thenReturn(Optional.empty());
        when(cardLabelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.attach(cardId, labelId, UUID.randomUUID());

        verify(cardLabelRepository).save(any());
    }

    @Test
    void detachRemovesExistingLink() {
        UUID labelId = UUID.randomUUID();
        TaskCardLabel link = new TaskCardLabel(UUID.randomUUID(), cardId, labelId);
        when(cardLabelRepository.findByCardIdAndLabelId(cardId, labelId)).thenReturn(Optional.of(link));

        service.detach(cardId, labelId, UUID.randomUUID());

        verify(cardLabelRepository).delete(link);
    }

    @Test
    void listOnlyRequiresSiteAccessNotManage() {
        when(labelRepository.findByConstructionSiteIdOrderByNameAsc(siteId)).thenReturn(java.util.List.of());

        var result = service.list(siteId, UUID.randomUUID());

        verify(permissionService, never()).requireManage(any(), any(), any());
        assertThat(result).isEmpty();
    }
}
