package com.pantheon.service.repository;

import com.pantheon.service.entity.ConstructionFunction;
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

    /**
     * Every approval row (any cycle) across a set of Pedidos de Compra — batched form used to
     * evaluate {@code isVisibleToViewAndApprove} for a whole page of headers in one query instead
     * of two per row.
     */
    List<PurchaseRequestApproval> findByPurchaseRequestIdIn(List<UUID> purchaseRequestIds);

    Optional<PurchaseRequestApproval> findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
            UUID purchaseRequestId, int cycleNumber, PurchaseRequestApprovalStatus status);

    /** Whether {@code function} already decided (approved or rejected) a step of this cycle — i.e. their turn already happened. */
    boolean existsByPurchaseRequestIdAndCycleNumberAndApproverFunctionAndStatusNot(
            UUID purchaseRequestId, int cycleNumber, ConstructionFunction approverFunction, PurchaseRequestApprovalStatus status);
}
