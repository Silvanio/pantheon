package com.pantheon.service.dto;

import java.util.List;

/** {@code awaitingApproval} mirrors {@code pantheon-web}'s `PurchaseRequestPanel.vue` stats definition:
 * status `ORCADO` and already submitted. */
public record PurchaseRequestSummaryResponse(long total, long awaitingApproval, List<RecentItemResponse> recent) {
}
