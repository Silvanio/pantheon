package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.TaskCommentRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskComment;
import com.pantheon.service.repository.AppUserRepository;
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
    private AppUserRepository userRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private TaskCardService taskCardService;

    private TaskCommentService service;

    private UUID siteId;
    private UUID cardId;

    @BeforeEach
    void setUp() {
        service = new TaskCommentService(
                commentRepository, cardRepository, userRepository, siteAccessService, permissionService, taskCardService);
        siteId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        lenient().when(cardRepository.findById(cardId)).thenReturn(Optional.of(
                new TaskCard(cardId, siteId, UUID.randomUUID(), "Card", null, null, 0, UUID.randomUUID(), Instant.now(), Instant.now())));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    @Test
    void addPersistsCommentAndRequiresManage() {
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TaskComment comment = service.add(cardId, UUID.randomUUID(), new TaskCommentRequest("Aguardando entrega"));

        assertThat(comment.getCardId()).isEqualTo(cardId);
        assertThat(comment.getBody()).isEqualTo("Aguardando entrega");
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
        verify(taskCardService).publishCardUpdated(cardId);
    }

    @Test
    void listOnlyRequiresSiteAccessNotManage() {
        when(commentRepository.findByCardIdOrderByCreatedAtAsc(cardId)).thenReturn(List.of());

        var result = service.list(cardId, UUID.randomUUID());

        verify(permissionService, never()).requireManage(any(), any(), any());
        assertThat(result).isEmpty();
    }

    @Test
    void authorNamesForResolvesDisplayNameFallingBackToEmail() {
        UUID authorWithName = UUID.randomUUID();
        UUID authorWithoutName = UUID.randomUUID();
        var comment1 = new TaskComment(UUID.randomUUID(), cardId, authorWithName, "Ok", Instant.now());
        var comment2 = new TaskComment(UUID.randomUUID(), cardId, authorWithoutName, "Ok", Instant.now());
        when(userRepository.findAllById(List.of(authorWithName, authorWithoutName))).thenReturn(List.of(
                new AppUser(authorWithName, "joao@example.com", "João Silva", "hash", null, Instant.now(), Instant.now()),
                new AppUser(authorWithoutName, "maria@example.com", null, "hash", null, Instant.now(), Instant.now())));

        var names = service.authorNamesFor(List.of(comment1, comment2));

        assertThat(names).containsEntry(authorWithName, "João Silva");
        assertThat(names).containsEntry(authorWithoutName, "maria@example.com");
    }
}
