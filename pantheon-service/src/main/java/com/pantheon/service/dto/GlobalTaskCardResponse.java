package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.service.GlobalTaskBoardService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GlobalTaskCardResponse(
        UUID id, UUID constructionSiteId, String siteName, String siteColorHex, UUID columnId, String title,
        String description, int sortOrder, List<UUID> labelIds, UUID createdBy, Instant createdAt, Instant updatedAt) {

    public static GlobalTaskCardResponse from(TaskCard card, ConstructionSite site, List<UUID> labelIds) {
        return new GlobalTaskCardResponse(
                card.getId(), card.getConstructionSiteId(), site.getName(), GlobalTaskBoardService.colorFor(site.getId()),
                card.getColumnId(), card.getTitle(), card.getDescription(), card.getSortOrder(), labelIds,
                card.getCreatedBy(), card.getCreatedAt(), card.getUpdatedAt());
    }
}
