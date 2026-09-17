package com.pantheon.service.controller;

import com.pantheon.service.dto.RenameSiteDocumentProjectRequest;
import com.pantheon.service.dto.SetFolderTaskLinkRequest;
import com.pantheon.service.dto.SiteDocumentBreadcrumbResponse;
import com.pantheon.service.dto.SiteDocumentFolderContentsResponse;
import com.pantheon.service.dto.SiteDocumentProjectAttachmentResponse;
import com.pantheon.service.dto.SiteDocumentProjectRegistrationRequest;
import com.pantheon.service.dto.SiteDocumentProjectResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.SiteDocumentProject;
import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import com.pantheon.service.service.SiteDocumentProjectService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class SiteDocumentProjectController {

    private final SiteDocumentProjectService projectService;

    public SiteDocumentProjectController(SiteDocumentProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/api/sites/{siteId}/projects/contents")
    public ResponseEntity<SiteDocumentFolderContentsResponse> contents(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) UUID parentId) {
        var contents = projectService.listContents(siteId, user.getId(), parentId);

        Map<UUID, String> userNames = projectService.displayNamesFor(userIdsOf(contents.folders(), contents.files()));
        Map<UUID, String> taskTitles = projectService.taskTitlesFor(
                contents.folders().stream().map(SiteDocumentProject::getTaskCardId).toList());

        List<SiteDocumentProjectResponse> folders =
                contents.folders().stream().map(folder -> toResponse(folder, userNames, taskTitles)).toList();
        List<SiteDocumentProjectAttachmentResponse> files = contents.files().stream()
                .map(file -> SiteDocumentProjectAttachmentResponse.from(file, userNames.get(file.getUploadedBy())))
                .toList();
        return ResponseEntity.ok(new SiteDocumentFolderContentsResponse(folders, files));
    }

    @GetMapping("/api/site-projects/{id}")
    public ResponseEntity<SiteDocumentProjectResponse> get(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        SiteDocumentProject folder = projectService.getFolder(id, user.getId());
        Map<UUID, String> userNames = projectService.displayNamesFor(List.of(folder.getCreatedBy(), folder.getUpdatedBy()));
        Map<UUID, String> taskTitles = projectService.taskTitlesFor(Collections.singletonList(folder.getTaskCardId()));
        return ResponseEntity.ok(toResponse(folder, userNames, taskTitles));
    }

    @GetMapping("/api/site-projects/{id}/breadcrumbs")
    public ResponseEntity<List<SiteDocumentBreadcrumbResponse>> breadcrumbs(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        List<SiteDocumentBreadcrumbResponse> breadcrumbs = projectService.getBreadcrumbs(id, user.getId()).stream()
                .map(folder -> new SiteDocumentBreadcrumbResponse(folder.getId(), folder.getName()))
                .toList();
        return ResponseEntity.ok(breadcrumbs);
    }

    @PostMapping("/api/sites/{siteId}/projects")
    public ResponseEntity<SiteDocumentProjectResponse> createFolder(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SiteDocumentProjectRegistrationRequest request) {
        SiteDocumentProject folder = projectService.createFolder(
                siteId, user.getId(), request.name(), request.parentId(), request.taskCardId());
        Map<UUID, String> userNames = projectService.displayNamesFor(List.of(folder.getCreatedBy()));
        Map<UUID, String> taskTitles = projectService.taskTitlesFor(Collections.singletonList(folder.getTaskCardId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(folder, userNames, taskTitles));
    }

    @PatchMapping("/api/site-projects/{id}/name")
    public ResponseEntity<SiteDocumentProjectResponse> rename(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody RenameSiteDocumentProjectRequest request) {
        SiteDocumentProject folder = projectService.renameFolder(id, user.getId(), request.name());
        Map<UUID, String> userNames = projectService.displayNamesFor(List.of(folder.getCreatedBy(), folder.getUpdatedBy()));
        Map<UUID, String> taskTitles = projectService.taskTitlesFor(Collections.singletonList(folder.getTaskCardId()));
        return ResponseEntity.ok(toResponse(folder, userNames, taskTitles));
    }

    @PutMapping("/api/site-projects/{id}/task-link")
    public ResponseEntity<SiteDocumentProjectResponse> setTaskLink(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody SetFolderTaskLinkRequest request) {
        SiteDocumentProject folder = projectService.setFolderTaskLink(id, user.getId(), request.taskCardId());
        Map<UUID, String> userNames = projectService.displayNamesFor(List.of(folder.getCreatedBy(), folder.getUpdatedBy()));
        Map<UUID, String> taskTitles = projectService.taskTitlesFor(Collections.singletonList(folder.getTaskCardId()));
        return ResponseEntity.ok(toResponse(folder, userNames, taskTitles));
    }

    @DeleteMapping("/api/site-projects/{id}/task-link")
    public ResponseEntity<Void> clearTaskLink(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        projectService.clearFolderTaskLink(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/site-projects/{id}")
    public ResponseEntity<Void> deleteFolder(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        projectService.deleteFolder(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/sites/{siteId}/projects/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SiteDocumentProjectAttachmentResponse> uploadFile(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam MultipartFile file) {
        var attachment = projectService.uploadFile(siteId, user.getId(), parentId, file);
        String uploaderName = projectService.displayNamesFor(List.of(attachment.getUploadedBy())).get(attachment.getUploadedBy());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SiteDocumentProjectAttachmentResponse.from(attachment, uploaderName));
    }

    @GetMapping("/api/site-project-attachments/{id}/content")
    public ResponseEntity<byte[]> getFileContent(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var content = projectService.getFileContent(id, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(content.originalName()).build().toString())
                .body(content.bytes());
    }

    @DeleteMapping("/api/site-project-attachments/{id}")
    public ResponseEntity<Void> deleteFile(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        projectService.deleteFile(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private SiteDocumentProjectResponse toResponse(
            SiteDocumentProject folder, Map<UUID, String> userNames, Map<UUID, String> taskTitles) {
        return SiteDocumentProjectResponse.from(
                folder, userNames.get(folder.getCreatedBy()), userNames.get(folder.getUpdatedBy()),
                taskTitles.get(folder.getTaskCardId()));
    }

    private List<UUID> userIdsOf(List<SiteDocumentProject> folders, List<SiteDocumentProjectAttachment> files) {
        List<UUID> ids = new ArrayList<>();
        folders.forEach(folder -> {
            ids.add(folder.getCreatedBy());
            ids.add(folder.getUpdatedBy());
        });
        files.forEach(file -> ids.add(file.getUploadedBy()));
        return ids;
    }
}
