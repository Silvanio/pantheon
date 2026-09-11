package com.pantheon.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ConstructionExceptionHandler {

    @ExceptionHandler(ConstructionSiteNotFoundException.class)
    public ResponseEntity<String> handleConstructionSiteNotFound(ConstructionSiteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NotSiteMemberException.class)
    public ResponseEntity<String> handleNotSiteMember(NotSiteMemberException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(ForbiddenCapabilityException.class)
    public ResponseEntity<String> handleForbiddenCapability(ForbiddenCapabilityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(EquipmentNotFoundException.class)
    public ResponseEntity<String> handleEquipmentNotFound(EquipmentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(SiteDocumentProjectNotFoundException.class)
    public ResponseEntity<String> handleSiteDocumentProjectNotFound(SiteDocumentProjectNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoNotFoundException.class)
    public ResponseEntity<String> handleOrcamentoNotFound(OrcamentoNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoNotDraftException.class)
    public ResponseEntity<String> handleOrcamentoNotDraft(OrcamentoNotDraftException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoNotApprovedException.class)
    public ResponseEntity<String> handleOrcamentoNotApproved(OrcamentoNotApprovedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoEmptyException.class)
    public ResponseEntity<String> handleOrcamentoEmpty(OrcamentoEmptyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(NoPendingApprovalStepException.class)
    public ResponseEntity<String> handleNoPendingApprovalStep(NoPendingApprovalStepException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(NotCurrentApprovalStepException.class)
    public ResponseEntity<String> handleNotCurrentApprovalStep(NotCurrentApprovalStepException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestItemNotFoundException.class)
    public ResponseEntity<String> handlePurchaseRequestItemNotFound(PurchaseRequestItemNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestItemAlreadyConvertedException.class)
    public ResponseEntity<String> handlePurchaseRequestItemAlreadyConverted(PurchaseRequestItemAlreadyConvertedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(MaterialNotFoundException.class)
    public ResponseEntity<String> handleMaterialNotFound(MaterialNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MaterialDeliveryStatusOrderException.class)
    public ResponseEntity<String> handleMaterialDeliveryStatusOrder(MaterialDeliveryStatusOrderException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestNotFoundException.class)
    public ResponseEntity<String> handlePurchaseRequestNotFound(PurchaseRequestNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(ItemsSpanMultiplePurchaseRequestsException.class)
    public ResponseEntity<String> handleItemsSpanMultiplePurchaseRequests(ItemsSpanMultiplePurchaseRequestsException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(CnpjPrefixTooShortException.class)
    public ResponseEntity<String> handleCnpjPrefixTooShort(CnpjPrefixTooShortException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(TaskColumnNotFoundException.class)
    public ResponseEntity<String> handleTaskColumnNotFound(TaskColumnNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(TaskColumnInUseException.class)
    public ResponseEntity<String> handleTaskColumnInUse(TaskColumnInUseException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(TaskCardNotFoundException.class)
    public ResponseEntity<String> handleTaskCardNotFound(TaskCardNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(TaskLabelNotFoundException.class)
    public ResponseEntity<String> handleTaskLabelNotFound(TaskLabelNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
