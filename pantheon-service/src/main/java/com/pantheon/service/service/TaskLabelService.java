package com.pantheon.service.service;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.exception.TaskLabelNotFoundException;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Obra-scoped, reusable labels attachable to that obra's {@link TaskCard}s. See {@code obra-tasks-board}. */
@Service
public class TaskLabelService {

    private final TaskLabelRepository labelRepository;
    private final TaskCardRepository cardRepository;
    private final TaskCardLabelRepository cardLabelRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public TaskLabelService(
            TaskLabelRepository labelRepository, TaskCardRepository cardRepository,
            TaskCardLabelRepository cardLabelRepository, SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.labelRepository = labelRepository;
        this.cardRepository = cardRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public List<TaskLabel> list(UUID siteId, UUID actingUserId) {
        siteAccessService.requireAccess(siteId, actingUserId);
        return labelRepository.findByConstructionSiteIdOrderByNameAsc(siteId);
    }

    @Transactional
    public TaskLabel create(UUID siteId, UUID actingUserId, TaskLabelRequest request) {
        requireManage(siteId, actingUserId);
        return labelRepository.save(
                new TaskLabel(UUID.randomUUID(), siteId, request.name(), request.colorHex(), Instant.now()));
    }

    @Transactional
    public void attach(UUID cardId, UUID labelId, UUID actingUserId) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);
        requireLabelOnSite(labelId, card.getConstructionSiteId());

        if (cardLabelRepository.findByCardIdAndLabelId(cardId, labelId).isEmpty()) {
            cardLabelRepository.save(new TaskCardLabel(UUID.randomUUID(), cardId, labelId));
        }
    }

    @Transactional
    public void detach(UUID cardId, UUID labelId, UUID actingUserId) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);

        cardLabelRepository.findByCardIdAndLabelId(cardId, labelId).ifPresent(cardLabelRepository::delete);
    }

    private void requireLabelOnSite(UUID labelId, UUID siteId) {
        TaskLabel label = labelRepository.findById(labelId).orElseThrow(() -> new TaskLabelNotFoundException(labelId));
        if (!label.getConstructionSiteId().equals(siteId)) {
            throw new TaskLabelNotFoundException(labelId);
        }
    }

    private TaskCard requireCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new TaskCardNotFoundException(cardId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.TASKS);
    }
}
