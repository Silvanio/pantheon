package com.pantheon.service.dto;

import java.time.Instant;
import java.util.UUID;

import com.pantheon.service.entity.MaterialRequest;
import com.pantheon.service.entity.MaterialRequestStatus;
public record MaterialRequestResponse(
        UUID id,
        UUID constructionSiteId,
        MaterialRequestStatus status,
        UUID requestedBy,
        UUID decidedBy,
        String decisionNote,
        Instant decidedAt) {

    public static MaterialRequestResponse from(MaterialRequest request) {
        return new MaterialRequestResponse(
                request.getId(),
                request.getConstructionSiteId(),
                request.getStatus(),
                request.getRequestedBy(),
                request.getDecidedBy(),
                request.getDecisionNote(),
                request.getDecidedAt());
    }
}
