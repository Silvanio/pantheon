package com.pantheon.service.messaging;

import java.util.UUID;

/**
 * Payload of the {@code orcamento-approval-step-pending} event, published once per recipient
 * whenever a new {@code OrcamentoApproval} step becomes {@code PENDING} (on submit, and when a
 * non-final step is approved and the next one activates).
 */
public record OrcamentoApprovalStepPendingEvent(
        UUID orcamentoId,
        UUID constructionSiteId,
        String constructionSiteName,
        String approverFunction,
        String recipientEmail) {

    public static final String TYPE = "orcamento-approval-step-pending";
}
