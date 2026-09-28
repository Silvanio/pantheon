package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequestInvoice;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRequestInvoiceRepository extends JpaRepository<PurchaseRequestInvoice, UUID> {

    List<PurchaseRequestInvoice> findByPurchaseRequestIdOrderByCreatedAtDesc(UUID purchaseRequestId);

    /**
     * Invoices for any of the given Pedidos de Compra uploaded within {@code [startInclusive,
     * endExclusive)} — used to surface same-day invoices in a daily report's Anexos list (see
     * {@code daily-report-media-and-signoff}'s "Same-day Pedido de Compra invoice surfaced"). An
     * explicit query (rather than a derived {@code Between}) keeps the upper bound exclusive.
     */
    @Query("SELECT i FROM PurchaseRequestInvoice i WHERE i.purchaseRequestId IN :purchaseRequestIds "
            + "AND i.createdAt >= :startInclusive AND i.createdAt < :endExclusive")
    List<PurchaseRequestInvoice> findByPurchaseRequestIdInAndCreatedAtBetween(
            @Param("purchaseRequestIds") List<UUID> purchaseRequestIds,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);
}
