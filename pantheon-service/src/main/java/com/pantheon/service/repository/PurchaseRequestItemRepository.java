package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, UUID> {

    List<PurchaseRequestItem> findByPurchaseRequestIdOrderByCreatedAtDesc(UUID purchaseRequestId);

    List<PurchaseRequestItem> findByPurchaseRequestIdAndStatusOrderByCreatedAtDesc(
            UUID purchaseRequestId, PurchaseRequestItemStatus status);
}
