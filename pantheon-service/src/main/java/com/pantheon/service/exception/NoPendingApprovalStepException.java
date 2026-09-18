package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when approving/rejecting is attempted on a Pedido de Compra with no pending approval step. */
public class NoPendingApprovalStepException extends RuntimeException {

    public NoPendingApprovalStepException(UUID purchaseRequestId) {
        super("Purchase request has no pending approval step: " + purchaseRequestId);
    }
}
