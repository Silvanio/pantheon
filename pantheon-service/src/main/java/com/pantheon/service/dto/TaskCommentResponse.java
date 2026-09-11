package com.pantheon.service.dto;

import com.pantheon.service.entity.TaskComment;
import java.time.Instant;
import java.util.UUID;

public record TaskCommentResponse(UUID id, UUID cardId, UUID authorId, String body, Instant createdAt) {

    public static TaskCommentResponse from(TaskComment comment) {
        return new TaskCommentResponse(
                comment.getId(), comment.getCardId(), comment.getAuthorId(), comment.getBody(), comment.getCreatedAt());
    }
}
