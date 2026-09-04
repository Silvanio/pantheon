package com.pantheon.service.dto;

import java.util.List;

public record OrcamentoDetailResponse(
        OrcamentoResponse orcamento, List<OrcamentoLineItemResponse> items, List<OrcamentoAttachmentResponse> attachments) {
}
