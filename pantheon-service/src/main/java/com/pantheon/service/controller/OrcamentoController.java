package com.pantheon.service.controller;

import com.pantheon.service.dto.OrcamentoApprovalResponse;
import com.pantheon.service.dto.OrcamentoCreationRequest;
import com.pantheon.service.dto.OrcamentoDetailResponse;
import com.pantheon.service.dto.OrcamentoLineItemRequest;
import com.pantheon.service.dto.OrcamentoLineItemResponse;
import com.pantheon.service.dto.OrcamentoResponse;
import com.pantheon.service.dto.RejectOrcamentoRequest;
import com.pantheon.service.dto.MaterialResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.service.MaterialService;
import com.pantheon.service.service.OrcamentoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

@RestController
public class OrcamentoController {

    private final OrcamentoService orcamentoService;
    private final MaterialService materialService;

    public OrcamentoController(OrcamentoService orcamentoService, MaterialService materialService) {
        this.orcamentoService = orcamentoService;
        this.materialService = materialService;
    }

    @PostMapping("/api/construction-sites/{siteId}/orcamentos")
    public ResponseEntity<OrcamentoResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody OrcamentoCreationRequest request) {
        Orcamento orcamento = orcamentoService.create(siteId, user.getId(), request.items(), request.fornecedor());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoResponse.from(orcamento));
    }

    @GetMapping("/api/construction-sites/{siteId}/orcamentos")
    public ResponseEntity<List<OrcamentoResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID purchaseRequestId) {
        List<OrcamentoResponse> orcamentos = orcamentoService.list(siteId, user.getId(), date, purchaseRequestId)
                .stream()
                .map(o -> OrcamentoResponse.from(o, orcamentoService.getSourcePurchaseRequestName(o.getSourcePurchaseRequestId())))
                .toList();
        return ResponseEntity.ok(orcamentos);
    }

    @GetMapping("/api/orcamentos/{id}")
    public ResponseEntity<OrcamentoDetailResponse> getDetail(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        Orcamento orcamento = orcamentoService.get(id, user.getId());
        List<OrcamentoLineItem> lineItems = orcamentoService.listLineItems(id);
        List<MaterialResponse> materials = materialService
                .listByLineItemIds(lineItems.stream().map(OrcamentoLineItem::getId).toList())
                .stream()
                .map(MaterialResponse::from)
                .toList();
        String sourcePurchaseRequestName = orcamentoService.getSourcePurchaseRequestName(orcamento.getSourcePurchaseRequestId());
        return ResponseEntity.ok(new OrcamentoDetailResponse(
                OrcamentoResponse.from(orcamento, sourcePurchaseRequestName),
                lineItems.stream().map(OrcamentoLineItemResponse::from).toList(),
                orcamentoService.listApprovals(id).stream().map(OrcamentoApprovalResponse::from).toList(),
                materials));
    }

    @PostMapping("/api/orcamentos/{id}/line-items")
    public ResponseEntity<OrcamentoLineItemResponse> addLineItem(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody OrcamentoLineItemRequest request) {
        var item = orcamentoService.addLineItem(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoLineItemResponse.from(item));
    }

    @PutMapping("/api/orcamentos/{id}/line-items/{lineItemId}")
    public ResponseEntity<OrcamentoLineItemResponse> updateLineItem(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @PathVariable UUID lineItemId,
            @Valid @RequestBody OrcamentoLineItemRequest request) {
        var item = orcamentoService.updateLineItem(id, lineItemId, user.getId(), request);
        return ResponseEntity.ok(OrcamentoLineItemResponse.from(item));
    }

    @DeleteMapping("/api/orcamentos/{id}/line-items/{lineItemId}")
    public ResponseEntity<Void> removeLineItem(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID lineItemId) {
        orcamentoService.removeLineItem(id, lineItemId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/orcamentos/{id}/submit")
    public ResponseEntity<OrcamentoResponse> submit(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.submitForApproval(id, user.getId())));
    }

    @PostMapping("/api/orcamentos/{id}/approve-step")
    public ResponseEntity<OrcamentoResponse> approveStep(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestParam(required = false) String comment) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.approveStep(id, user.getId(), comment)));
    }

    @PostMapping("/api/orcamentos/{id}/reject-step")
    public ResponseEntity<OrcamentoResponse> rejectStep(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody RejectOrcamentoRequest request) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.rejectStep(id, user.getId(), request.reason())));
    }

    @PostMapping("/api/orcamentos/{id}/conclude")
    public ResponseEntity<OrcamentoResponse> conclude(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.conclude(id, user.getId())));
    }
}
