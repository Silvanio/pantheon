package com.pantheon.service.controller;

import com.pantheon.service.dto.MaterialResponse;
import com.pantheon.service.dto.OrcamentoCreationRequest;
import com.pantheon.service.dto.OrcamentoDetailResponse;
import com.pantheon.service.dto.OrcamentoLineItemRequest;
import com.pantheon.service.dto.OrcamentoLineItemResponse;
import com.pantheon.service.dto.OrcamentoResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.service.MaterialService;
import com.pantheon.service.service.OrcamentoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public ResponseEntity<Page<OrcamentoResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID purchaseRequestId,
            @RequestParam(required = false) String supplier,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Orcamento> result = orcamentoService.list(
                siteId, user.getId(), date, purchaseRequestId, supplier, PageRequest.of(page, size));
        Page<OrcamentoResponse> response = result.map(o -> OrcamentoResponse.from(
                o, orcamentoService.getSourcePurchaseRequestName(o.getSourcePurchaseRequestId())));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/orcamentos/{id}")
    public ResponseEntity<OrcamentoDetailResponse> getDetail(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        Orcamento orcamento = orcamentoService.get(id, user.getId());
        List<OrcamentoLineItem> lineItems = orcamentoService.listLineItems(id);
        Map<UUID, Boolean> selectedFlags = orcamentoService.selectedFlags(lineItems);
        List<MaterialResponse> materials = materialService
                .listByLineItemIds(lineItems.stream().map(OrcamentoLineItem::getId).toList())
                .stream()
                .map(MaterialResponse::from)
                .toList();
        String sourcePurchaseRequestName = orcamentoService.getSourcePurchaseRequestName(orcamento.getSourcePurchaseRequestId());
        return ResponseEntity.ok(new OrcamentoDetailResponse(
                OrcamentoResponse.from(orcamento, sourcePurchaseRequestName),
                lineItems.stream()
                        .map(item -> OrcamentoLineItemResponse.from(item, selectedFlags.getOrDefault(item.getId(), false)))
                        .toList(),
                materials));
    }

    @PostMapping("/api/orcamentos/{id}/line-items")
    public ResponseEntity<OrcamentoLineItemResponse> addLineItem(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody OrcamentoLineItemRequest request) {
        var item = orcamentoService.addLineItem(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoLineItemResponse.from(item, orcamentoService.isSelected(item)));
    }

    @PutMapping("/api/orcamentos/{id}/line-items/{lineItemId}")
    public ResponseEntity<OrcamentoLineItemResponse> updateLineItem(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @PathVariable UUID lineItemId,
            @Valid @RequestBody OrcamentoLineItemRequest request) {
        var item = orcamentoService.updateLineItem(id, lineItemId, user.getId(), request);
        return ResponseEntity.ok(OrcamentoLineItemResponse.from(item, orcamentoService.isSelected(item)));
    }

    @DeleteMapping("/api/orcamentos/{id}/line-items/{lineItemId}")
    public ResponseEntity<Void> removeLineItem(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID lineItemId) {
        orcamentoService.removeLineItem(id, lineItemId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/orcamentos/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        orcamentoService.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
