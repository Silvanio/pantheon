package com.pantheon.service.service;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.exception.TaskLabelNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Card-facing label operations: attaching/detaching a company's predefined labels (see
 * {@code company-task-labels}) to a card, and creating a card-only custom label. See
 * {@code obra-tasks-board}.
 */
@Service
public class TaskLabelService {

    private final TaskLabelRepository labelRepository;
    private final TaskCardRepository cardRepository;
    private final TaskCardLabelRepository cardLabelRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public TaskLabelService(
            TaskLabelRepository labelRepository, TaskCardRepository cardRepository,
            TaskCardLabelRepository cardLabelRepository, ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService, SitePermissionService permissionService) {
        this.labelRepository = labelRepository;
        this.cardRepository = cardRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    @Transactional
    public TaskLabel createCustom(UUID cardId, UUID actingUserId, TaskLabelRequest request) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);

        TaskLabel label = labelRepository.save(
                TaskLabel.custom(UUID.randomUUID(), cardId, request.name(), request.colorHex(), Instant.now()));
        cardLabelRepository.save(new TaskCardLabel(UUID.randomUUID(), cardId, label.getId()));
        return label;
    }

    @Transactional
    public void attachPredefined(UUID cardId, UUID labelId, UUID actingUserId) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);
        requireLabelOnCompany(labelId, requireSite(card.getConstructionSiteId()).getCompanyId());

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

    private void requireLabelOnCompany(UUID labelId, UUID companyId) {
        TaskLabel label = labelRepository.findById(labelId).orElseThrow(() -> new TaskLabelNotFoundException(labelId));
        if (!label.isPredefined() || !label.getCompanyId().equals(companyId)) {
            throw new TaskLabelNotFoundException(labelId);
        }
    }

    private TaskCard requireCard(UUID cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new TaskCardNotFoundException(cardId));
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.TASKS);
    }
}
