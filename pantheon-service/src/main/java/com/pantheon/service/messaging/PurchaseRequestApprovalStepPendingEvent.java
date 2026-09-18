package com.pantheon.service.messaging;

import java.util.UUID;

/**
 * Payload of the {@code purchase-request-approval-step-pending} event, published once per
 * recipient whenever a new {@code PurchaseRequestApproval} step becomes {@code PENDING} (on
 * submit, and when a non-final step is approved and the next one activates).
 */
public record PurchaseRequestApprovalStepPendingEvent(
        UUID purchaseRequestId,
        UUID constructionSiteId,
        String constructionSiteName,
        String approverFunction,
        String recipientEmail) {

    public static final String TYPE = "purchase-request-approval-step-pending";
}
