package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequestInvoice;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestInvoiceRepository extends JpaRepository<PurchaseRequestInvoice, UUID> {

    List<PurchaseRequestInvoice> findByPurchaseRequestIdOrderByCreatedAtDesc(UUID purchaseRequestId);
}
