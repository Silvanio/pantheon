package com.pantheon.service.controller;

import com.pantheon.service.dto.MaterialResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.MaterialService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/api/construction-sites/{siteId}/materials")
    public ResponseEntity<List<MaterialResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) UUID orcamentoId) {
        List<MaterialResponse> materials =
                materialService.list(siteId, user.getId(), orcamentoId).stream().map(MaterialResponse::from).toList();
        return ResponseEntity.ok(materials);
    }

    @PostMapping("/api/materials/{id}/mark-delivered")
    public ResponseEntity<MaterialResponse> markDelivered(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(MaterialResponse.from(materialService.markDelivered(id, user.getId())));
    }

    @PostMapping(value = "/api/materials/{id}/mark-checked", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> markChecked(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestParam(required = false) List<MultipartFile> photos) {
        return ResponseEntity.ok(MaterialResponse.from(materialService.markChecked(id, user.getId(), photos)));
    }

    @GetMapping("/api/material-photos/{photoId}/content")
    public ResponseEntity<byte[]> getPhotoContent(@AuthenticationPrincipal AppUser user, @PathVariable UUID photoId) {
        return ResponseEntity.ok(materialService.getPhotoContent(photoId, user.getId()));
    }
}
