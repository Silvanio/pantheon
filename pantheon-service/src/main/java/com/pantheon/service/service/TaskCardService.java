package com.pantheon.service.service;

import com.pantheon.service.dto.MoveTaskCardRequest;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.dto.TaskLabelResponse;
import com.pantheon.service.dto.UpdateTaskCardDueDateRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardAssignee;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.SiteMembershipNotFoundException;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.exception.TaskColumnNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskCardAssigneeRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import com.pantheon.service.sse.SseEventPublisher;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Obra-scoped Tasks board: cards placed into the columns shared by that obra's company. Read
 * access only requires site membership; create/move/assign/delete require {@code MANAGE} on
 * {@link PermissionCapability#TASKS}. See {@code obra-tasks-board}.
 */
@Service
public class TaskCardService {

    private final TaskCardRepository cardRepository;
    private final TaskColumnRepository columnRepository;
    private final TaskCardLabelRepository cardLabelRepository;
    private final TaskLabelRepository labelRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskCardAssigneeRepository assigneeRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final SseEventPublisher sseEventPublisher;

    public TaskCardService(
            TaskCardRepository cardRepository, TaskColumnRepository columnRepository,
            TaskCardLabelRepository cardLabelRepository, TaskLabelRepository labelRepository,
            TaskCommentRepository commentRepository, TaskCardAssigneeRepository assigneeRepository,
            ConstructionSiteRepository siteRepository, SiteMembershipRepository siteMembershipRepository,
            SiteAccessService siteAccessService, SitePermissionService permissionService,
            SseEventPublisher sseEventPublisher) {
        this.cardRepository = cardRepository;
        this.columnRepository = columnRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.labelRepository = labelRepository;
        this.commentRepository = commentRepository;
        this.assigneeRepository = assigneeRepository;
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.sseEventPublisher = sseEventPublisher;
    }

    public TaskBoard getBoard(UUID siteId, UUID actingUserId) {
        ConstructionSite site = requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.TASKS);

        List<TaskColumn> columns = columnRepository.findByCompanyIdOrderBySortOrderAsc(site.getCompanyId());
        List<TaskCard> cards = cardRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId);
        List<UUID> cardIds = cards.stream().map(TaskCard::getId).toList();

        Map<UUID, List<UUID>> labelIdsByCard = labelIdsByCard(cardIds);
        Map<UUID, List<UUID>> assigneeIdsByCard = assigneeIdsByCard(cardIds);
        Map<UUID, Long> commentCountByCard = commentRepository.countByCardIdIn(cardIds).stream()
                .collect(Collectors.toMap(
                        TaskCommentRepository.CardCommentCount::getCardId,
                        TaskCommentRepository.CardCommentCount::getCommentCount));

        List<UUID> attachedLabelIds = labelIdsByCard.values().stream().flatMap(List::stream).distinct().toList();
        List<TaskLabel> labels = labelRepository.findAllById(attachedLabelIds);

        return new TaskBoard(columns, cards, labelIdsByCard, assigneeIdsByCard, commentCountByCard, labels);
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
                request.dueDate(), nextSortOrder, actingUserId, now, now);
        TaskCard created = cardRepository.save(card);

        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", created.getId());
        payload.put("constructionSiteId", created.getConstructionSiteId());
        payload.put("companyId", site.getCompanyId());
        payload.put("columnId", created.getColumnId());
        payload.put("title", created.getTitle());
        payload.put("description", created.getDescription());
        payload.put("dueDate", created.getDueDate());
        payload.put("sortOrder", created.getSortOrder());
        payload.put("createdBy", created.getCreatedBy());
        payload.put("createdAt", created.getCreatedAt().toString());
        sseEventPublisher.publishToCompany(site.getCompanyId(), "task-card-created", payload);

        return created;
    }

    @Transactional
    public TaskCard moveCard(UUID cardId, UUID actingUserId, MoveTaskCardRequest request) {
        TaskCard card = requireCard(cardId);
        ConstructionSite site = requireSite(card.getConstructionSiteId());
        requireManage(card.getConstructionSiteId(), actingUserId);
        requireColumnBelongsToCompany(request.columnId(), site.getCompanyId());

        card.moveTo(request.columnId(), request.sortOrder(), Instant.now());
        TaskCard moved = cardRepository.save(card);

        sseEventPublisher.publishToCompany(site.getCompanyId(), "task-card-moved", Map.of(
                "cardId", moved.getId(),
                "constructionSiteId", moved.getConstructionSiteId(),
                "companyId", site.getCompanyId(),
                "columnId", moved.getColumnId(),
                "sortOrder", moved.getSortOrder(),
                "movedBy", actingUserId,
                "movedAt", Instant.now().toString()));

        return moved;
    }

    @Transactional
    public TaskCard updateDueDate(UUID cardId, UUID actingUserId, UpdateTaskCardDueDateRequest request) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);

        card.updateDueDate(request.dueDate(), Instant.now());
        TaskCard updated = cardRepository.save(card);
        publishCardUpdated(updated);
        return updated;
    }

    @Transactional
    public void assign(UUID cardId, UUID actingUserId, UUID siteMembershipId) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);
        requireMembershipOnSite(siteMembershipId, card.getConstructionSiteId());

        if (assigneeRepository.findByCardIdAndSiteMembershipId(cardId, siteMembershipId).isEmpty()) {
            assigneeRepository.save(new TaskCardAssignee(UUID.randomUUID(), cardId, siteMembershipId, Instant.now()));
        }
        publishCardUpdated(card);
    }

    @Transactional
    public void unassign(UUID cardId, UUID actingUserId, UUID siteMembershipId) {
        TaskCard card = requireCard(cardId);
        requireManage(card.getConstructionSiteId(), actingUserId);

        assigneeRepository.findByCardIdAndSiteMembershipId(cardId, siteMembershipId)
                .ifPresent(assigneeRepository::delete);
        publishCardUpdated(card);
    }

    /**
     * Publishes a company-scoped {@code task-card-updated} event carrying the card's current
     * derived state (labels, assignees, comment count) so any client with the board open can
     * merge it into its local copy in place. Called after any in-place edit to an existing card
     * (due date, assignees, labels, comments) — as opposed to {@code task-card-moved},
     * {@code task-card-created}, and {@code task-card-deleted}, which cover structural changes.
     * Public so {@link TaskLabelService} and {@link TaskCommentService} can call it after their
     * own mutations without duplicating this card/site/derived-data lookup.
     */
    @Transactional
    public void publishCardUpdated(UUID cardId) {
        publishCardUpdated(requireCard(cardId));
    }

    private void publishCardUpdated(TaskCard card) {
        ConstructionSite site = requireSite(card.getConstructionSiteId());
        List<UUID> labelIds = labelIdsForCard(card.getId());
        // Full label objects, not just ids: a label a recipient hasn't seen yet (a card-only
        // custom label, or a predefined label they can't independently fetch — see
        // CompanyTaskLabelService's company-staff-only catalog endpoint) would otherwise render
        // as a pill with no name/color until their next full reload.
        List<TaskLabelResponse> labels =
                labelRepository.findAllById(labelIds).stream().map(TaskLabelResponse::from).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", card.getId());
        payload.put("constructionSiteId", card.getConstructionSiteId());
        payload.put("companyId", site.getCompanyId());
        payload.put("columnId", card.getColumnId());
        payload.put("title", card.getTitle());
        payload.put("description", card.getDescription());
        payload.put("dueDate", card.getDueDate());
        payload.put("sortOrder", card.getSortOrder());
        payload.put("labelIds", labelIds);
        payload.put("labels", labels);
        payload.put("assigneeIds", assigneeIdsForCard(card.getId()));
        payload.put("commentCount", commentCountForCard(card.getId()));
        payload.put("updatedAt", Instant.now().toString());
        sseEventPublisher.publishToCompany(site.getCompanyId(), "task-card-updated", payload);
    }

    @Transactional
    public void deleteCard(UUID cardId, UUID actingUserId) {
        TaskCard card = requireCard(cardId);
        ConstructionSite site = requireSite(card.getConstructionSiteId());
        requireManage(card.getConstructionSiteId(), actingUserId);

        cardLabelRepository.deleteByCardId(cardId);
        labelRepository.deleteByCardId(cardId);
        commentRepository.deleteByCardId(cardId);
        assigneeRepository.deleteByCardId(cardId);
        cardRepository.delete(card);

        sseEventPublisher.publishToCompany(site.getCompanyId(), "task-card-deleted", Map.of(
                "cardId", cardId,
                "constructionSiteId", card.getConstructionSiteId(),
                "companyId", site.getCompanyId()));
    }

    public List<UUID> labelIdsForCard(UUID cardId) {
        return cardLabelRepository.findByCardIdIn(List.of(cardId)).stream().map(TaskCardLabel::getLabelId).toList();
    }

    public List<UUID> assigneeIdsForCard(UUID cardId) {
        return assigneeRepository.findByCardIdIn(List.of(cardId)).stream()
                .map(TaskCardAssignee::getSiteMembershipId)
                .toList();
    }

    public long commentCountForCard(UUID cardId) {
        return commentRepository.findByCardIdOrderByCreatedAtAsc(cardId).size();
    }

    private Map<UUID, List<UUID>> labelIdsByCard(List<UUID> cardIds) {
        List<TaskCardLabel> cardLabels = cardLabelRepository.findByCardIdIn(cardIds);
        return cardLabels.stream().collect(Collectors.groupingBy(
                TaskCardLabel::getCardId, Collectors.mapping(TaskCardLabel::getLabelId, Collectors.toList())));
    }

    private Map<UUID, List<UUID>> assigneeIdsByCard(List<UUID> cardIds) {
        List<TaskCardAssignee> assignees = assigneeRepository.findByCardIdIn(cardIds);
        return assignees.stream().collect(Collectors.groupingBy(
                TaskCardAssignee::getCardId, Collectors.mapping(TaskCardAssignee::getSiteMembershipId, Collectors.toList())));
    }

    private void requireColumnBelongsToCompany(UUID columnId, UUID companyId) {
        TaskColumn column = columnRepository.findById(columnId).orElseThrow(() -> new TaskColumnNotFoundException(columnId));
        if (!column.getCompanyId().equals(companyId)) {
            throw new TaskColumnNotFoundException(columnId);
        }
    }

    private void requireMembershipOnSite(UUID siteMembershipId, UUID siteId) {
        SiteMembership membership = siteMembershipRepository.findById(siteMembershipId)
                .orElseThrow(() -> new SiteMembershipNotFoundException(siteMembershipId));
        if (!membership.getConstructionSiteId().equals(siteId)) {
            throw new SiteMembershipNotFoundException(siteMembershipId);
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

    /**
     * A site's task board: the company's shared columns, that obra's cards, and each card's
     * label ids, assignee (site membership) ids, and comment count.
     */
    public record TaskBoard(
            List<TaskColumn> columns, List<TaskCard> cards, Map<UUID, List<UUID>> labelIdsByCard,
            Map<UUID, List<UUID>> assigneeIdsByCard, Map<UUID, Long> commentCountByCard, List<TaskLabel> labels) {
    }
}
