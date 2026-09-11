package com.pantheon.service.controller;

import com.pantheon.service.dto.ConvertPurchaseRequestItemsRequest;
import com.pantheon.service.dto.OrcamentoResponse;
import com.pantheon.service.dto.PurchaseRequestCreationRequest;
import com.pantheon.service.dto.PurchaseRequestDetailResponse;
import com.pantheon.service.dto.PurchaseRequestItemResponse;
import com.pantheon.service.dto.PurchaseRequestResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import com.pantheon.service.service.OrcamentoService;
import com.pantheon.service.service.PurchaseRequestItemService;
import com.pantheon.service.service.PurchaseRequestService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final PurchaseRequestItemService purchaseRequestItemService;
    private final OrcamentoService orcamentoService;

    public PurchaseRequestController(
            PurchaseRequestService purchaseRequestService,
            PurchaseRequestItemService purchaseRequestItemService,
            OrcamentoService orcamentoService) {
        this.purchaseRequestService = purchaseRequestService;
        this.purchaseRequestItemService = purchaseRequestItemService;
        this.orcamentoService = orcamentoService;
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
    public ResponseEntity<List<PurchaseRequestResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<PurchaseRequestResponse> purchaseRequests = purchaseRequestService
                .list(siteId, user.getId(), date)
                .stream()
                .map(PurchaseRequestResponse::from)
                .toList();
        return ResponseEntity.ok(purchaseRequests);
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
        return ResponseEntity.ok(new PurchaseRequestDetailResponse(PurchaseRequestResponse.from(purchaseRequest), items));
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
}
