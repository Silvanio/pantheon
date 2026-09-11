package com.pantheon.service.service;

import com.pantheon.service.dto.TaskCommentRequest;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskComment;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comments on a {@link TaskCard}. Reading only requires site access; posting requires {@code MANAGE} on {@code TASKS}. */
@Service
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskCardRepository cardRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public TaskCommentService(
            TaskCommentRepository commentRepository, TaskCardRepository cardRepository,
            SiteAccessService siteAccessService, SitePermissionService permissionService) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public List<TaskComment> list(UUID cardId, UUID actingUserId) {
        TaskCard card = requireCard(cardId);
        siteAccessService.requireAccess(card.getConstructionSiteId(), actingUserId);
        return commentRepository.findByCardIdOrderByCreatedAtAsc(cardId);
    }

    @Transactional
    public TaskComment add(UUID cardId, UUID actingUserId, TaskCommentRequest request) {
        TaskCard card = requireCard(cardId);
        var access = siteAccessService.requireAccess(card.getConstructionSiteId(), actingUserId);
        permissionService.requireManage(card.getConstructionSiteId(), access, PermissionCapability.TASKS);

        return commentRepository.save(
                new TaskComment(UUID.randomUUID(), cardId, actingUserId, request.body(), Instant.now()));
    }

    private TaskCard requireCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new TaskCardNotFoundException(cardId));
    }
}
