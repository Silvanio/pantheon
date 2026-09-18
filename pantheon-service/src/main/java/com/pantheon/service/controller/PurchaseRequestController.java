package com.pantheon.service.controller;

import com.pantheon.service.dto.ConvertPurchaseRequestItemsRequest;
import com.pantheon.service.dto.OrcamentoResponse;
import com.pantheon.service.dto.PurchaseRequestApprovalResponse;
import com.pantheon.service.dto.PurchaseRequestComparisonResponse;
import com.pantheon.service.dto.PurchaseRequestCreationRequest;
import com.pantheon.service.dto.PurchaseRequestDetailResponse;
import com.pantheon.service.dto.PurchaseRequestInvoiceResponse;
import com.pantheon.service.dto.PurchaseRequestItemResponse;
import com.pantheon.service.dto.PurchaseRequestResponse;
import com.pantheon.service.dto.RejectPurchaseRequestRequest;
import com.pantheon.service.dto.SetItemSelectionRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.service.OrcamentoService;
import com.pantheon.service.service.PurchaseRequestItemService;
import com.pantheon.service.service.PurchaseRequestPdfService;
import com.pantheon.service.service.PurchaseRequestService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseRequestItemService purchaseRequestItemService;
    private final OrcamentoService orcamentoService;
    private final PurchaseRequestPdfService purchaseRequestPdfService;

    public PurchaseRequestController(
            PurchaseRequestService purchaseRequestService,
            PurchaseRequestItemService purchaseRequestItemService,
            OrcamentoService orcamentoService,
            PurchaseRequestPdfService purchaseRequestPdfService) {
        this.purchaseRequestService = purchaseRequestService;
        this.purchaseRequestItemService = purchaseRequestItemService;
        this.orcamentoService = orcamentoService;
        this.purchaseRequestPdfService = purchaseRequestPdfService;
    }

    @PostMapping("/api/construction-sites/{siteId}/purchase-requests")
    public ResponseEntity<PurchaseRequestResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody PurchaseRequestCreationRequest request) {
        var purchaseRequest = purchaseRequestService.create(siteId, user.getId(), request.items());
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseRequestResponse.from(purchaseRequest));
    }

    @GetMapping("/api/construction-sites/{siteId}/purchase-requests")
    public ResponseEntity<Page<PurchaseRequestResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) PurchaseRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PurchaseRequestResponse> response = purchaseRequestService
                .list(siteId, user.getId(), date, status, PageRequest.of(page, size))
                .map(pr -> PurchaseRequestResponse.from(pr, purchaseRequestService.listLinkedOrcamentos(pr.getId())));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/purchase-requests/{id}")
    public ResponseEntity<PurchaseRequestDetailResponse> getDetail(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestParam(required = false) PurchaseRequestItemStatus status) {
        var purchaseRequest = purchaseRequestService.get(id, user.getId());
        List<PurchaseRequestItemResponse> items = purchaseRequestItemService
                .list(id, user.getId(), status)
                .stream()
                .map(PurchaseRequestItemResponse::from)
                .toList();
        List<PurchaseRequestApprovalResponse> approvals = purchaseRequestService
                .listApprovals(id)
                .stream()
                .map(PurchaseRequestApprovalResponse::from)
                .toList();
        var response = PurchaseRequestResponse.from(purchaseRequest, purchaseRequestService.listLinkedOrcamentos(id));
        return ResponseEntity.ok(new PurchaseRequestDetailResponse(response, items, approvals));
    }

    @DeleteMapping("/api/purchase-requests/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        purchaseRequestService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/purchase-requests/{id}/convert-to-orcamento")
    public ResponseEntity<OrcamentoResponse> convertToOrcamento(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ConvertPurchaseRequestItemsRequest request) {
        var orcamento = purchaseRequestItemService.convertToOrcamento(id, user.getId(), request.itemIds(), request.fornecedor());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoResponse.from(
                orcamento, orcamentoService.getSourcePurchaseRequestName(orcamento.getSourcePurchaseRequestId())));
    }

    @PutMapping("/api/purchase-requests/{id}/items/{itemId}/selection")
    public ResponseEntity<PurchaseRequestItemResponse> setItemSelection(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @RequestBody SetItemSelectionRequest request) {
        var item = purchaseRequestItemService.setSelection(itemId, user.getId(), request.orcamentoLineItemId());
        return ResponseEntity.ok(PurchaseRequestItemResponse.from(item));
    }

    @GetMapping("/api/purchase-requests/{id}/comparison")
    public ResponseEntity<PurchaseRequestComparisonResponse> getComparison(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(purchaseRequestService.getComparison(id, user.getId()));
    }

    @GetMapping("/api/purchase-requests/{id}/orcamentos/{orcamentoId}/pdf")
    public ResponseEntity<byte[]> getSupplierPdf(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID orcamentoId) {
        byte[] pdf = purchaseRequestPdfService.generate(id, orcamentoId, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("pedido-" + id + "-fornecedor-" + orcamentoId + ".pdf").build().toString())
                .body(pdf);
    }

    @PostMapping("/api/purchase-requests/{id}/submit")
    public ResponseEntity<PurchaseRequestResponse> submit(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var purchaseRequest = purchaseRequestService.submitForApproval(id, user.getId());
        return ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequest, purchaseRequestService.listLinkedOrcamentos(id)));
    }

    @PostMapping("/api/purchase-requests/{id}/approve-step")
    public ResponseEntity<PurchaseRequestResponse> approveStep(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestParam(required = false) String comment) {
        var purchaseRequest = purchaseRequestService.approveStep(id, user.getId(), comment);
        return ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequest, purchaseRequestService.listLinkedOrcamentos(id)));
    }

    @PostMapping("/api/purchase-requests/{id}/reject-step")
    public ResponseEntity<PurchaseRequestResponse> rejectStep(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody RejectPurchaseRequestRequest request) {
        var purchaseRequest = purchaseRequestService.rejectStep(id, user.getId(), request.reason());
        return ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequest, purchaseRequestService.listLinkedOrcamentos(id)));
    }

    @PostMapping("/api/purchase-requests/{id}/conclude")
    public ResponseEntity<PurchaseRequestResponse> conclude(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var purchaseRequest = purchaseRequestService.conclude(id, user.getId());
        return ResponseEntity.ok(PurchaseRequestResponse.from(purchaseRequest, purchaseRequestService.listLinkedOrcamentos(id)));
    }

    @PostMapping(value = "/api/purchase-requests/{id}/invoices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PurchaseRequestInvoiceResponse> uploadInvoice(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestParam MultipartFile file) {
        var invoice = purchaseRequestService.uploadInvoice(id, user.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseRequestInvoiceResponse.from(invoice));
    }

    @GetMapping("/api/purchase-requests/{id}/invoices")
    public ResponseEntity<List<PurchaseRequestInvoiceResponse>> listInvoices(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var invoices = purchaseRequestService.listInvoices(id, user.getId()).stream()
                .map(PurchaseRequestInvoiceResponse::from)
                .toList();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/api/purchase-request-invoices/{id}/content")
    public ResponseEntity<byte[]> getInvoiceContent(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var content = purchaseRequestService.getInvoiceContent(id, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(content.originalName()).build().toString())
                .body(content.bytes());
    }

    @DeleteMapping("/api/purchase-request-invoices/{id}")
    public ResponseEntity<Void> deleteInvoice(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        purchaseRequestService.deleteInvoice(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
