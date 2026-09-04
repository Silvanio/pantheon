package com.pantheon.service.controller;

import jakarta.validation.Valid;
import java.math.BigDecimal;
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

import com.pantheon.service.dto.MaterialRequestCreationRequest;
import com.pantheon.service.dto.MaterialRequestDetailResponse;
import com.pantheon.service.dto.MaterialRequestItemResponse;
import com.pantheon.service.dto.MaterialRequestResponse;
import com.pantheon.service.dto.ReceiptVerificationPhotoResponse;
import com.pantheon.service.dto.ReceiptVerificationRequest;
import com.pantheon.service.dto.ReceiptVerificationResponse;
import com.pantheon.service.dto.RejectMaterialRequestRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.MaterialRequest;
import com.pantheon.service.entity.MaterialRequestItem;
import com.pantheon.service.entity.MaterialRequestStatus;
import com.pantheon.service.entity.ReceiptVerification;
import com.pantheon.service.service.MaterialRequestService;
@RestController
public class MaterialRequestController {

    private final MaterialRequestService materialRequestService;

    public MaterialRequestController(MaterialRequestService materialRequestService) {
        this.materialRequestService = materialRequestService;
    }

    @PostMapping("/api/construction-sites/{siteId}/material-requests")
    public ResponseEntity<MaterialRequestResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody MaterialRequestCreationRequest request) {
        MaterialRequest materialRequest = materialRequestService.create(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaterialRequestResponse.from(materialRequest));
    }

    @GetMapping("/api/construction-sites/{siteId}/material-requests")
    public ResponseEntity<List<MaterialRequestResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) MaterialRequestStatus status) {
        List<MaterialRequestResponse> requests = materialRequestService.list(siteId, user.getId(), status).stream()
                .map(MaterialRequestResponse::from)
                .toList();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/api/material-requests/{id}")
    public ResponseEntity<MaterialRequestDetailResponse> getDetail(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        MaterialRequest request = materialRequestService.getRequest(id, user.getId());
        List<MaterialRequestItem> items = materialRequestService.listItems(id);
        List<ReceiptVerification> verifications =
                materialRequestService.listVerifications(items.stream().map(MaterialRequestItem::getId).toList());

        var requestedByItemId = items.stream().collect(
                java.util.stream.Collectors.toMap(MaterialRequestItem::getId, MaterialRequestItem::getRequestedQuantity));

        return ResponseEntity.ok(new MaterialRequestDetailResponse(
                MaterialRequestResponse.from(request),
                items.stream().map(MaterialRequestItemResponse::from).toList(),
                verifications.stream()
                        .map(v -> ReceiptVerificationResponse.from(
                                v, requestedByItemId.get(v.getMaterialRequestItemId())))
                        .toList()));
    }

    @PostMapping("/api/material-requests/{id}/approve")
    public ResponseEntity<MaterialRequestResponse> approve(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        MaterialRequest request = materialRequestService.approve(id, user.getId());
        return ResponseEntity.ok(MaterialRequestResponse.from(request));
    }

    @PostMapping("/api/material-requests/{id}/reject")
    public ResponseEntity<MaterialRequestResponse> reject(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody RejectMaterialRequestRequest request) {
        MaterialRequest materialRequest = materialRequestService.reject(id, user.getId(), request.reason());
        return ResponseEntity.ok(MaterialRequestResponse.from(materialRequest));
    }

    @PostMapping("/api/material-requests/{id}/receipt-verifications")
    public ResponseEntity<ReceiptVerificationResponse> recordVerification(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ReceiptVerificationRequest request) {
        ReceiptVerification verification = materialRequestService.recordVerification(
                id, user.getId(), request.materialRequestItemId(), request.receivedQuantity(), request.note());
        BigDecimal requestedQuantity = materialRequestService.listItems(id).stream()
                .filter(i -> i.getId().equals(request.materialRequestItemId()))
                .findFirst()
                .map(MaterialRequestItem::getRequestedQuantity)
                .orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReceiptVerificationResponse.from(verification, requestedQuantity));
    }

    @PostMapping(
            value = "/api/material-requests/{id}/receipt-verifications/{verificationId}/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptVerificationPhotoResponse> uploadVerificationPhoto(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @PathVariable UUID verificationId,
            @RequestParam MultipartFile file) {
        var photo = materialRequestService.uploadVerificationPhoto(id, user.getId(), verificationId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReceiptVerificationPhotoResponse.from(photo));
    }
}
