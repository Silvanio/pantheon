package com.pantheon.service.dto;

import java.util.List;

public record OrcamentoDetailResponse(
        OrcamentoResponse orcamento,
        List<OrcamentoLineItemResponse> lineItems,
        List<OrcamentoApprovalResponse> approvals,
        List<MaterialResponse> materials) {
}
