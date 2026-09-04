package com.pantheon.service.controller;

import com.pantheon.service.dto.OrcamentoAttachmentResponse;
import com.pantheon.service.dto.OrcamentoCreationRequest;
import com.pantheon.service.dto.OrcamentoDetailResponse;
import com.pantheon.service.dto.OrcamentoLineItemResponse;
import com.pantheon.service.dto.OrcamentoResponse;
import com.pantheon.service.dto.RejectOrcamentoRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.AttachmentKind;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.service.OrcamentoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping("/api/material-requests/{requestId}/orcamentos")
    public ResponseEntity<OrcamentoResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID requestId,
            @Valid @RequestBody OrcamentoCreationRequest request) {
        Orcamento orcamento = orcamentoService.create(requestId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoResponse.from(orcamento));
    }

    @GetMapping("/api/material-requests/{requestId}/orcamentos")
    public ResponseEntity<List<OrcamentoResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID requestId) {
        List<OrcamentoResponse> orcamentos =
                orcamentoService.list(requestId, user.getId()).stream().map(OrcamentoResponse::from).toList();
        return ResponseEntity.ok(orcamentos);
    }

    @GetMapping("/api/orcamentos/{id}")
    public ResponseEntity<OrcamentoDetailResponse> getDetail(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        Orcamento orcamento = orcamentoService.get(id, user.getId());
        return ResponseEntity.ok(new OrcamentoDetailResponse(
                OrcamentoResponse.from(orcamento),
                orcamentoService.listLineItems(id).stream().map(OrcamentoLineItemResponse::from).toList(),
                orcamentoService.listAttachments(id).stream().map(OrcamentoAttachmentResponse::from).toList()));
    }

    @PostMapping("/api/orcamentos/{id}/send")
    public ResponseEntity<OrcamentoResponse> send(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.send(id, user.getId())));
    }

    @PostMapping("/api/orcamentos/{id}/approve")
    public ResponseEntity<OrcamentoResponse> approve(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.approve(id, user.getId())));
    }

    @PostMapping("/api/orcamentos/{id}/reject")
    public ResponseEntity<OrcamentoResponse> reject(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody RejectOrcamentoRequest request) {
        return ResponseEntity.ok(OrcamentoResponse.from(orcamentoService.reject(id, user.getId(), request.reason())));
    }

    @PostMapping(value = "/api/orcamentos/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrcamentoAttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestParam AttachmentKind kind,
            @RequestParam MultipartFile file) {
        var attachment = orcamentoService.uploadAttachment(id, user.getId(), kind, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrcamentoAttachmentResponse.from(attachment));
    }

    @GetMapping("/api/orcamento-attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> getAttachmentContent(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID attachmentId) {
        return ResponseEntity.ok(orcamentoService.getAttachmentContent(attachmentId, user.getId()));
    }
}
