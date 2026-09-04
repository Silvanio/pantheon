package com.pantheon.service.dto;

import com.pantheon.service.entity.AttachmentKind;
import com.pantheon.service.entity.OrcamentoAttachment;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoAttachmentResponse(
        UUID id, UUID orcamentoId, AttachmentKind kind, String originalName, String contentType, Instant createdAt) {

    public static OrcamentoAttachmentResponse from(OrcamentoAttachment attachment) {
        return new OrcamentoAttachmentResponse(
                attachment.getId(), attachment.getOrcamentoId(), attachment.getKind(), attachment.getOriginalName(),
                attachment.getContentType(), attachment.getCreatedAt());
    }
}
