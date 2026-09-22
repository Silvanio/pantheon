package com.pantheon.service.controller;

import com.pantheon.service.dto.ConstructionSiteRegistrationRequest;
import com.pantheon.service.dto.ConstructionSiteResponse;
import com.pantheon.service.dto.ConstructionSiteStatusUpdateRequest;
import com.pantheon.service.dto.MySiteResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.service.ConstructionSiteService;
import com.pantheon.service.service.ScheduleService;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ConstructionSiteController {

    private final ConstructionSiteService constructionSiteService;
    private final StorageService storageService;
    private final ScheduleService scheduleService;

    public ConstructionSiteController(
            ConstructionSiteService constructionSiteService, StorageService storageService, ScheduleService scheduleService) {
        this.constructionSiteService = constructionSiteService;
        this.storageService = storageService;
        this.scheduleService = scheduleService;
    }

    @PostMapping("/api/companies/{companyId}/construction-sites")
    public ResponseEntity<ConstructionSiteResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID companyId,
            @Valid @RequestBody ConstructionSiteRegistrationRequest request) {
        ConstructionSite site = constructionSiteService.create(companyId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(site));
    }

    @GetMapping("/api/companies/{companyId}/construction-sites")
    public ResponseEntity<List<ConstructionSiteResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID companyId) {
        List<ConstructionSiteResponse> sites = constructionSiteService.list(companyId, user.getId()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(sites);
    }

    // Static "mine" segment resolved before Spring MVC ever tries the "{id}" mapping below.
    @GetMapping("/api/construction-sites/mine")
    public ResponseEntity<List<MySiteResponse>> mine(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(constructionSiteService.listMine(user.getId()));
    }

    @GetMapping("/api/construction-sites/{id}")
    public ResponseEntity<ConstructionSiteResponse> get(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(constructionSiteService.get(id, user.getId())));
    }

    @GetMapping("/api/construction-sites/{id}/photo/content")
    public ResponseEntity<byte[]> getPhotoContent(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        ConstructionSite site = constructionSiteService.get(id, user.getId());
        byte[] content = storageService.getObject(site.getPhotoObjectKey());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(content);
    }

    // Gated by site access (company staff OR SiteMembership), not company-staff membership —
    // a site-only member has no CompanyMembership to authorize a plain /companies/{id}/logo call.
    @GetMapping("/api/construction-sites/{id}/company-logo")
    public ResponseEntity<byte[]> getCompanyLogo(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        String logoObjectKey = constructionSiteService.getCompanyLogoObjectKey(id, user.getId());
        byte[] content = storageService.getObject(logoObjectKey);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(content);
    }

    @PatchMapping("/api/construction-sites/{id}/status")
    public ResponseEntity<ConstructionSiteResponse> updateStatus(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ConstructionSiteStatusUpdateRequest request) {
        ConstructionSite site = constructionSiteService.updateStatus(id, user.getId(), request.status());
        return ResponseEntity.ok(toResponse(site));
    }

    @PutMapping(value = "/api/construction-sites/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConstructionSiteResponse> updatePhoto(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestParam MultipartFile photo) {
        String extension = extensionOf(photo.getOriginalFilename());
        String key = StorageKeys.sitePhotoKey(id, extension);
        storageService.putObject(key, readBytes(photo), photo.getContentType());

        ConstructionSite site = constructionSiteService.updatePhoto(id, user.getId(), key);
        return ResponseEntity.ok(toResponse(site));
    }

    private ConstructionSiteResponse toResponse(ConstructionSite site) {
        return ConstructionSiteResponse.from(site, scheduleService.computeProgress(site.getId()));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
