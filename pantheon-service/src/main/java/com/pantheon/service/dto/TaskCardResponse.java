package com.pantheon.service.dto;

import com.pantheon.service.entity.TaskCard;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskCardResponse(
        UUID id, UUID constructionSiteId, UUID columnId, String title, String description, LocalDate dueDate,
        int sortOrder, List<UUID> labelIds, List<UUID> assigneeIds, long commentCount, UUID createdBy,
        Instant createdAt, Instant updatedAt) {

    public static TaskCardResponse from(TaskCard card, List<UUID> labelIds, List<UUID> assigneeIds, long commentCount) {
        return new TaskCardResponse(
                card.getId(), card.getConstructionSiteId(), card.getColumnId(), card.getTitle(), card.getDescription(),
                card.getDueDate(), card.getSortOrder(), labelIds, assigneeIds, commentCount, card.getCreatedBy(),
                card.getCreatedAt(), card.getUpdatedAt());
    }
}
