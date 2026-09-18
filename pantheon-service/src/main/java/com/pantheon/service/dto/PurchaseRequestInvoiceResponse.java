package com.pantheon.service.dto;

import com.pantheon.service.entity.PurchaseRequestInvoice;
import java.time.Instant;
import java.util.UUID;

public record PurchaseRequestInvoiceResponse(
        UUID id, UUID purchaseRequestId, String originalName, String contentType, UUID uploadedBy, Instant createdAt) {

    public static PurchaseRequestInvoiceResponse from(PurchaseRequestInvoice invoice) {
        return new PurchaseRequestInvoiceResponse(
                invoice.getId(), invoice.getPurchaseRequestId(), invoice.getOriginalName(), invoice.getContentType(),
                invoice.getUploadedBy(), invoice.getCreatedAt());
    }
}
