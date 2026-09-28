package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseRequestRepository
        extends JpaRepository<PurchaseRequest, UUID>, JpaSpecificationExecutor<PurchaseRequest> {

    long countByConstructionSiteIdAndCreatedAtBetween(UUID constructionSiteId, Instant dayStart, Instant dayEnd);

    /** "Aguardando aprovação" — mirrors `PurchaseRequestPanel.vue`'s own stats definition. */
    long countByConstructionSiteIdAndStatusAndSubmittedAtIsNotNull(UUID constructionSiteId, PurchaseRequestStatus status);

    /** Every Pedido de Compra header for a site, regardless of status — used to scope a site's {@code PurchaseRequestInvoice}s when surfacing same-day invoices on a daily report. */
    List<PurchaseRequest> findByConstructionSiteId(UUID constructionSiteId);
}
