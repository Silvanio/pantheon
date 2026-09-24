package com.pantheon.service.service;

import com.pantheon.service.dto.SiteMemberResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardAssignee;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.entity.TaskLabel;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteDocumentProjectAttachmentRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskCardAssigneeRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
import com.pantheon.service.repository.TaskCommentRepository;
import com.pantheon.service.repository.TaskLabelRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Company-admin-only aggregated Tasks board combining every obra's cards into one view. Each
 * card is annotated with a color derived deterministically from its {@code constructionSiteId}
 * (same obra always renders the same color; nothing is persisted for it). See
 * {@code global-tasks-board}.
 */
@Service
public class GlobalTaskBoardService {

    /** Hand-picked, visually distinct colors. Same obra always maps to the same index. */
    private static final List<String> COLOR_PALETTE = List.of(
            "#EF4444", "#F97316", "#F59E0B", "#84CC16", "#22C55E", "#10B981", "#14B8A6", "#06B6D4",
            "#3B82F6", "#6366F1", "#8B5CF6", "#A855F7", "#D946EF", "#EC4899");

    private final ConstructionSiteRepository siteRepository;
    private final TaskColumnRepository columnRepository;
    private final TaskCardRepository cardRepository;
    private final TaskCardLabelRepository cardLabelRepository;
    private final TaskLabelRepository labelRepository;
    private final CompanyMembershipRepository membershipRepository;
    private final PlatformAdminService platformAdminService;
    private final TaskCardAssigneeRepository assigneeRepository;
    private final TaskCommentRepository commentRepository;
    private final SiteDocumentProjectAttachmentRepository attachmentRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final AppUserRepository userRepository;

    public GlobalTaskBoardService(
            ConstructionSiteRepository siteRepository, TaskColumnRepository columnRepository,
            TaskCardRepository cardRepository, TaskCardLabelRepository cardLabelRepository,
            TaskLabelRepository labelRepository, CompanyMembershipRepository membershipRepository,
            PlatformAdminService platformAdminService, TaskCardAssigneeRepository assigneeRepository,
            TaskCommentRepository commentRepository, SiteDocumentProjectAttachmentRepository attachmentRepository,
            SiteMembershipRepository siteMembershipRepository, AppUserRepository userRepository) {
        this.siteRepository = siteRepository;
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.labelRepository = labelRepository;
        this.membershipRepository = membershipRepository;
        this.platformAdminService = platformAdminService;
        this.assigneeRepository = assigneeRepository;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.userRepository = userRepository;
    }

    public GlobalTaskBoard build(UUID companyId, UUID actingUserId) {
        requireAdmin(companyId, actingUserId);

        List<ConstructionSite> sites = siteRepository.findByCompanyId(companyId);
        Map<UUID, ConstructionSite> siteById = sites.stream().collect(Collectors.toMap(ConstructionSite::getId, s -> s));

        List<TaskColumn> columns = columnRepository.findByCompanyIdOrderBySortOrderAsc(companyId);
        List<TaskCard> cards = cardRepository.findByConstructionSiteIdInOrderBySortOrderAsc(sites.stream()
                .map(ConstructionSite::getId)
                .toList());

        List<UUID> cardIds = cards.stream().map(TaskCard::getId).toList();
        Map<UUID, List<UUID>> labelIdsByCard = cardLabelRepository.findByCardIdIn(cardIds).stream()
                .collect(Collectors.groupingBy(TaskCardLabel::getCardId, Collectors.mapping(TaskCardLabel::getLabelId, Collectors.toList())));

        List<TaskLabel> labels = labelRepository.findByCompanyIdOrderByNameAsc(companyId);

        Map<UUID, List<UUID>> assigneeIdsByCard = assigneeRepository.findByCardIdIn(cardIds).stream()
                .collect(Collectors.groupingBy(TaskCardAssignee::getCardId, Collectors.mapping(TaskCardAssignee::getSiteMembershipId, Collectors.toList())));
        Map<UUID, Long> commentCountByCard = commentRepository.countByCardIdIn(cardIds).stream()
                .collect(Collectors.toMap(TaskCommentRepository.CardCommentCount::getCardId, TaskCommentRepository.CardCommentCount::getCommentCount));
        Map<UUID, Long> attachmentCountByCard = attachmentRepository.countByTaskCardIdIn(cardIds).stream()
                .collect(Collectors.toMap(SiteDocumentProjectAttachmentRepository.CardAttachmentCount::getCardId, SiteDocumentProjectAttachmentRepository.CardAttachmentCount::getAttachmentCount));

        List<UUID> assigneeMembershipIds = assigneeIdsByCard.values().stream().flatMap(List::stream).distinct().toList();
        List<SiteMemberResponse> assignees = resolveAssignees(assigneeMembershipIds);

        return new GlobalTaskBoard(columns, cards, siteById, labelIdsByCard, labels, assigneeIdsByCard, commentCountByCard, attachmentCountByCard, assignees);
    }

    /** Mirrors {@code SiteMembershipService.listMembers}'s name resolution (AppUser display name when linked, else the accountless membership's own stored name). */
    private List<SiteMemberResponse> resolveAssignees(List<UUID> membershipIds) {
        List<SiteMembership> memberships = siteMembershipRepository.findAllById(membershipIds);
        List<UUID> userIds = memberships.stream().map(SiteMembership::getUserId).filter(id -> id != null).toList();
        Map<UUID, AppUser> usersById =
                userRepository.findAllById(userIds).stream().collect(Collectors.toMap(AppUser::getId, u -> u));

        return memberships.stream()
                .map(m -> {
                    AppUser user = m.getUserId() != null ? usersById.get(m.getUserId()) : null;
                    return new SiteMemberResponse(
                            m.getId(), m.getUserId(), user != null ? user.getEmail() : m.getContactEmail(),
                            user != null ? user.getDisplayName() : m.getDisplayName(), m.getFunction(),
                            m.getServiceProviderTrade(), m.getCpf(), m.getPhone(), m.getStatus(), !m.isActive());
                })
                .toList();
    }

    /** Deterministic color for an obra: same input always yields the same palette entry. */
    public static String colorFor(UUID constructionSiteId) {
        int index = Math.floorMod(constructionSiteId.hashCode(), COLOR_PALETTE.size());
        return COLOR_PALETTE.get(index);
    }

    private void requireAdmin(UUID companyId, UUID userId) {
        if (platformAdminService.isSuperAdmin(userId)) {
            return;
        }
        CompanyMembership membership = membershipRepository
                .findByCompanyIdAndUserId(companyId, userId)
                .filter(CompanyMembership::isActive)
                .orElseThrow(() -> new NotCompanyAdminException(companyId));
        if (membership.getRole() != CompanyRole.ADMIN) {
            throw new NotCompanyAdminException(companyId);
        }
    }

    public record GlobalTaskBoard(
            List<TaskColumn> columns, List<TaskCard> cards, Map<UUID, ConstructionSite> siteById,
            Map<UUID, List<UUID>> labelIdsByCard, List<TaskLabel> labels, Map<UUID, List<UUID>> assigneeIdsByCard,
            Map<UUID, Long> commentCountByCard, Map<UUID, Long> attachmentCountByCard, List<SiteMemberResponse> assignees) {
    }
}
