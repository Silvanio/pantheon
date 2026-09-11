package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.TaskCommentRequest;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskComment;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskCommentServiceTest {

    @Mock
    private TaskCommentRepository commentRepository;

    @Mock
    private TaskCardRepository cardRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private TaskCommentService service;

    private UUID siteId;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        service = new TaskCommentService(commentRepository, cardRepository, siteAccessService, permissionService);
        siteId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        lenient().when(cardRepository.findById(cardId)).thenReturn(Optional.of(
                new TaskCard(cardId, siteId, UUID.randomUUID(), "Card", null, 0, UUID.randomUUID(), Instant.now(), Instant.now())));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    @Test
    void addPersistsCommentAndRequiresManage() {
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskComment comment = service.add(cardId, UUID.randomUUID(), new TaskCommentRequest("Aguardando entrega"));

        assertThat(comment.getCardId()).isEqualTo(cardId);
        assertThat(comment.getBody()).isEqualTo("Aguardando entrega");
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }

    @Test
    void listOnlyRequiresSiteAccessNotManage() {
        when(commentRepository.findByCardIdOrderByCreatedAtAsc(cardId)).thenReturn(List.of());

        var result = service.list(cardId, UUID.randomUUID());

        verify(permissionService, never()).requireManage(any(), any(), any());
        assertThat(result).isEmpty();
    }
}
