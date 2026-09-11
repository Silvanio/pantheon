package com.pantheon.service.service;

import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskCardLabel;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.NotCompanyAdminException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.TaskCardLabelRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
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
    private final CompanyMembershipRepository membershipRepository;

    public GlobalTaskBoardService(
            ConstructionSiteRepository siteRepository, TaskColumnRepository columnRepository,
            TaskCardRepository cardRepository, TaskCardLabelRepository cardLabelRepository,
            CompanyMembershipRepository membershipRepository) {
        this.siteRepository = siteRepository;
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.cardLabelRepository = cardLabelRepository;
        this.membershipRepository = membershipRepository;
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

        return new GlobalTaskBoard(columns, cards, siteById, labelIdsByCard);
    }

    /** Deterministic color for an obra: same input always yields the same palette entry. */
    public static String colorFor(UUID constructionSiteId) {
        int index = Math.floorMod(constructionSiteId.hashCode(), COLOR_PALETTE.size());
        return COLOR_PALETTE.get(index);
    }

    private void requireAdmin(UUID companyId, UUID userId) {
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
            Map<UUID, List<UUID>> labelIdsByCard) {
    }
}
