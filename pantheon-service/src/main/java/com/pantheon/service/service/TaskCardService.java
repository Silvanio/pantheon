package com.pantheon.service.service;

import com.pantheon.service.dto.MoveTaskCardRequest;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.exception.TaskColumnNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Obra-scoped Tasks board: cards placed into the columns shared by that obra's company. Read
 * access only requires site membership; create/move require {@code MANAGE} on
 * {@link PermissionCapability#TASKS}. See {@code obra-tasks-board}.
 */
@Service
public class TaskCardService {

    private final TaskCardRepository cardRepository;
    private final TaskColumnRepository columnRepository;
    private final TaskCardLabelRepository cardLabelRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public TaskCardService(
            TaskCardRepository cardRepository, TaskColumnRepository columnRepository,
            TaskCardLabelRepository cardLabelRepository, ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService, SitePermissionService permissionService) {
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public TaskBoard getBoard(UUID siteId, UUID actingUserId) {
        ConstructionSite site = requireSite(siteId);
        siteAccessService.requireAccess(siteId, actingUserId);

        List<TaskColumn> columns = columnRepository.findByCompanyIdOrderBySortOrderAsc(site.getCompanyId());
        List<TaskCard> cards = cardRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId);
        Map<UUID, List<UUID>> labelIdsByCard = labelIdsByCard(cards);
        return new TaskBoard(columns, cards, labelIdsByCard);
    }

    @Transactional
    public TaskCard createCard(UUID siteId, UUID actingUserId, TaskCardCreationRequest request) {
        ConstructionSite site = requireSite(siteId);
        requireManage(siteId, actingUserId);
        requireColumnBelongsToCompany(request.columnId(), site.getCompanyId());

        int nextSortOrder = (int) cardRepository.countByConstructionSiteIdAndColumnId(siteId, request.columnId());
        Instant now = Instant.now();
        TaskCard card = new TaskCard(
                UUID.randomUUID(), siteId, request.columnId(), request.title(), request.description(),
                nextSortOrder, actingUserId, now, now);
        return cardRepository.save(card);
    }

    @Transactional
    public TaskCard moveCard(UUID cardId, UUID actingUserId, MoveTaskCardRequest request) {
        TaskCard card = requireCard(cardId);
        ConstructionSite site = requireSite(card.getConstructionSiteId());
        requireManage(card.getConstructionSiteId(), actingUserId);
        requireColumnBelongsToCompany(request.columnId(), site.getCompanyId());

        card.moveTo(request.columnId(), request.sortOrder(), Instant.now());
        return cardRepository.save(card);
    }

    public List<UUID> labelIdsForCard(UUID cardId) {
        return cardLabelRepository.findByCardIdIn(List.of(cardId)).stream().map(TaskCardLabel::getLabelId).toList();
    }

    private Map<UUID, List<UUID>> labelIdsByCard(List<TaskCard> cards) {
        List<UUID> cardIds = cards.stream().map(TaskCard::getId).toList();
        List<TaskCardLabel> cardLabels = cardLabelRepository.findByCardIdIn(cardIds);
        return cardLabels.stream().collect(Collectors.groupingBy(
                TaskCardLabel::getCardId, Collectors.mapping(TaskCardLabel::getLabelId, Collectors.toList())));
    }

    private void requireColumnBelongsToCompany(UUID columnId, UUID companyId) {
        TaskColumn column = columnRepository.findById(columnId).orElseThrow(() -> new TaskColumnNotFoundException(columnId));
        if (!column.getCompanyId().equals(companyId)) {
            throw new TaskColumnNotFoundException(columnId);
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

    /** A site's task board: the company's shared columns, that obra's cards, and each card's label ids. */
    public record TaskBoard(List<TaskColumn> columns, List<TaskCard> cards, Map<UUID, List<UUID>> labelIdsByCard) {
    }
}
