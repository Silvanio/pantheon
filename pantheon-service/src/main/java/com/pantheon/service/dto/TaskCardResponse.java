package com.pantheon.service.dto;

import com.pantheon.service.entity.TaskCard;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskCardResponse(
        UUID id, UUID constructionSiteId, UUID columnId, String title, String description, int sortOrder,
        List<UUID> labelIds, UUID createdBy, Instant createdAt, Instant updatedAt) {

    public static TaskCardResponse from(TaskCard card, List<UUID> labelIds) {
        return new TaskCardResponse(
                card.getId(), card.getConstructionSiteId(), card.getColumnId(), card.getTitle(), card.getDescription(),
                card.getSortOrder(), labelIds, card.getCreatedBy(), card.getCreatedAt(), card.getUpdatedAt());
    }
}
