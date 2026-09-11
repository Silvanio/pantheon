package com.pantheon.service.service;

import com.pantheon.service.dto.TaskCommentRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskComment;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comments on a {@link TaskCard}. Reading only requires site access; posting requires {@code MANAGE} on {@code TASKS}. */
@Service
public class TaskCommentService {

    private final TaskCommentRepository commentRepository;
    private final TaskCardRepository cardRepository;
    private final AppUserRepository userRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public TaskCommentService(
            TaskCommentRepository commentRepository, TaskCardRepository cardRepository,
            AppUserRepository userRepository, SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.commentRepository = commentRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
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

    /** Resolves each comment's author id to a display name (falling back to email), for rendering. */
    public Map<UUID, String> authorNamesFor(List<TaskComment> comments) {
        List<UUID> authorIds = comments.stream().map(TaskComment::getAuthorId).distinct().toList();
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(AppUser::getId, TaskCommentService::displayNameOrEmail));
    }

    private static String displayNameOrEmail(AppUser user) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getEmail();
    }

    private TaskCard requireCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new TaskCardNotFoundException(cardId));
    }
}
