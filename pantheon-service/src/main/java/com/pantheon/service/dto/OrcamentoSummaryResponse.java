package com.pantheon.service.dto;

import java.util.List;

public record OrcamentoSummaryResponse(long total, long draft, List<RecentItemResponse> recent) {
}
