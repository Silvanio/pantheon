package com.pantheon.service.dto;

import com.pantheon.service.entity.PurchaseRequestInvoice;
import java.time.Instant;
import java.util.UUID;

/**
 * A {@link PurchaseRequestInvoice} uploaded on the same calendar date as a daily report's {@code
 * reportDate}, surfaced read-only in that report's Anexos list — see {@code
 * daily-report-media-and-signoff}'s "Same-day Pedido de Compra invoice surfaced". References its
 * source Pedido de Compra rather than copying the file; its content is fetched from the existing
 * {@code GET /api/purchase-request-invoices/{id}/content} endpoint using {@link #id}.
 */
public record DailyReportImportedInvoiceResponse(
        UUID id,
        UUID purchaseRequestId,
        String purchaseRequestName,
        String originalName,
        String contentType,
        Instant uploadedAt) {

    public static DailyReportImportedInvoiceResponse from(PurchaseRequestInvoice invoice, String purchaseRequestName) {
        return new DailyReportImportedInvoiceResponse(
                invoice.getId(), invoice.getPurchaseRequestId(), purchaseRequestName, invoice.getOriginalName(),
                invoice.getContentType(), invoice.getCreatedAt());
    }
}
