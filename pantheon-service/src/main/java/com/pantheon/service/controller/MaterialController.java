package com.pantheon.service.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.MaterialRegistrationRequest;
import com.pantheon.service.dto.MaterialResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Material;
import com.pantheon.service.service.MaterialService;
@RestController
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/api/construction-sites/{siteId}/materials")
    public ResponseEntity<MaterialResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody MaterialRegistrationRequest request) {
        Material material = materialService.create(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaterialResponse.from(material));
    }

    @GetMapping("/api/construction-sites/{siteId}/materials")
    public ResponseEntity<List<MaterialResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        List<MaterialResponse> materials =
                materialService.list(siteId, user.getId()).stream().map(MaterialResponse::from).toList();
        return ResponseEntity.ok(materials);
    }
}
