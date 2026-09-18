package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequestApproval;
import com.pantheon.service.entity.PurchaseRequestApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestApprovalRepository extends JpaRepository<PurchaseRequestApproval, UUID> {

    List<PurchaseRequestApproval> findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(UUID purchaseRequestId);

    List<PurchaseRequestApproval> findByPurchaseRequestIdAndCycleNumberOrderByStepOrderAsc(
            UUID purchaseRequestId, int cycleNumber);

    Optional<PurchaseRequestApproval> findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
            UUID purchaseRequestId, int cycleNumber, PurchaseRequestApprovalStatus status);
}
