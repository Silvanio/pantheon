package com.pantheon.service.controller;

import com.pantheon.service.dto.SiteDocumentProjectAttachmentResponse;
import com.pantheon.service.dto.SiteDocumentProjectRegistrationRequest;
import com.pantheon.service.dto.SiteDocumentProjectResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.SiteDocumentProject;
import com.pantheon.service.service.SiteDocumentProjectService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SiteDocumentProjectController {

    private final SiteDocumentProjectService projectService;

    public SiteDocumentProjectController(SiteDocumentProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/api/sites/{siteId}/projects")
    public ResponseEntity<SiteDocumentProjectResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SiteDocumentProjectRegistrationRequest request) {
        SiteDocumentProject project = projectService.create(siteId, user.getId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(SiteDocumentProjectResponse.from(project));
    }

    @GetMapping("/api/sites/{siteId}/projects")
    public ResponseEntity<List<SiteDocumentProjectResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        List<SiteDocumentProjectResponse> projects =
                projectService.list(siteId, user.getId()).stream().map(SiteDocumentProjectResponse::from).toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/api/site-projects/{id}")
    public ResponseEntity<SiteDocumentProjectResponse> get(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(SiteDocumentProjectResponse.from(projectService.get(id, user.getId())));
    }

    @PostMapping(value = "/api/site-projects/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SiteDocumentProjectAttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestParam MultipartFile file) {
        var attachment = projectService.uploadAttachment(id, user.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(SiteDocumentProjectAttachmentResponse.from(attachment));
    }

    @GetMapping("/api/site-projects/{id}/attachments")
    public ResponseEntity<List<SiteDocumentProjectAttachmentResponse>> listAttachments(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        List<SiteDocumentProjectAttachmentResponse> attachments = projectService.listAttachments(id, user.getId())
                .stream()
                .map(SiteDocumentProjectAttachmentResponse::from)
                .toList();
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/api/site-projects/{id}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> getAttachmentContent(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID attachmentId) {
        byte[] content = projectService.getAttachmentContent(id, attachmentId, user.getId());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(content);
    }
}
