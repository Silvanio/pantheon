package com.pantheon.service.messaging;

import java.util.UUID;

/** Payload of the {@code orcamento-sent} event, notifying the site's client that a budget awaits their decision. */
public record OrcamentoSentEvent(
        UUID orcamentoId,
        UUID materialRequestId,
        UUID constructionSiteId,
        String constructionSiteName,
        String clientEmail) {

    public static final String TYPE = "orcamento-sent";
}
