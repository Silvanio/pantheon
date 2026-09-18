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

    @ExceptionHandler(SiteDocumentAttachmentNotFoundException.class)
    public ResponseEntity<String> handleSiteDocumentAttachmentNotFound(SiteDocumentAttachmentNotFoundException e) {
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

    @ExceptionHandler(SiteMembershipNotFoundException.class)
    public ResponseEntity<String> handleSiteMembershipNotFound(SiteMembershipNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(InvalidCpfException.class)
    public ResponseEntity<String> handleInvalidCpf(InvalidCpfException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(EmailRequiredException.class)
    public ResponseEntity<String> handleEmailRequired(EmailRequiredException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(PersonBelongsToAnotherCompanyException.class)
    public ResponseEntity<String> handlePersonBelongsToAnotherCompany(PersonBelongsToAnotherCompanyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(SelectionNotAllowedException.class)
    public ResponseEntity<String> handleSelectionNotAllowed(SelectionNotAllowedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoLineItemNotLinkedException.class)
    public ResponseEntity<String> handleOrcamentoLineItemNotLinked(OrcamentoLineItemNotLinkedException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestNotOrcadoException.class)
    public ResponseEntity<String> handlePurchaseRequestNotOrcado(PurchaseRequestNotOrcadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestSelectionIncompleteException.class)
    public ResponseEntity<String> handlePurchaseRequestSelectionIncomplete(PurchaseRequestSelectionIncompleteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestNotConferidoException.class)
    public ResponseEntity<String> handlePurchaseRequestNotConferido(PurchaseRequestNotConferidoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoNotLinkedToPurchaseRequestException.class)
    public ResponseEntity<String> handleOrcamentoNotLinkedToPurchaseRequest(OrcamentoNotLinkedToPurchaseRequestException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestNotDeletableException.class)
    public ResponseEntity<String> handlePurchaseRequestNotDeletable(PurchaseRequestNotDeletableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(OrcamentoNotDeletableException.class)
    public ResponseEntity<String> handleOrcamentoNotDeletable(OrcamentoNotDeletableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(PurchaseRequestInvoiceNotFoundException.class)
    public ResponseEntity<String> handlePurchaseRequestInvoiceNotFound(PurchaseRequestInvoiceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(PixKeyRequiredException.class)
    public ResponseEntity<String> handlePixKeyRequired(PixKeyRequiredException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
