package com.pantheon.service.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.dto.ConstructionSiteResponse;
import com.pantheon.service.dto.ConstructionSiteStatusUpdateRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.service.ConstructionSiteService;
@RestController
public class ConstructionSiteController {

    private final ConstructionSiteService constructionSiteService;

    public ConstructionSiteController(ConstructionSiteService constructionSiteService) {
        this.constructionSiteService = constructionSiteService;
    }

    @PostMapping("/api/projects/{projectId}/construction-sites")
    public ResponseEntity<ConstructionSiteResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody ConstructionSiteRegistrationRequest request) {
        ConstructionSite site = constructionSiteService.create(projectId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConstructionSiteResponse.from(site));
    }

    @GetMapping("/api/projects/{projectId}/construction-sites")
    public ResponseEntity<List<ConstructionSiteResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID projectId) {
        List<ConstructionSiteResponse> sites = constructionSiteService.list(projectId, user.getId()).stream()
                .map(ConstructionSiteResponse::from)
                .toList();
        return ResponseEntity.ok(sites);
    }

    @PatchMapping("/api/construction-sites/{id}/status")
    public ResponseEntity<ConstructionSiteResponse> updateStatus(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ConstructionSiteStatusUpdateRequest request) {
        ConstructionSite site = constructionSiteService.updateStatus(id, user.getId(), request.status());
        return ResponseEntity.ok(ConstructionSiteResponse.from(site));
    }
}
