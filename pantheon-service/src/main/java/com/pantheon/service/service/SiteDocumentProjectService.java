package com.pantheon.service.service;

import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteDocumentProject;
import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.SiteDocumentProjectNotFoundException;
import com.pantheon.service.repository.SiteDocumentProjectAttachmentRepository;
import com.pantheon.service.repository.SiteDocumentProjectRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** "Projetos" owned by a construction site: create, list, and attach PDF files. */
@Service
public class SiteDocumentProjectService {

    private final SiteDocumentProjectRepository projectRepository;
    private final SiteDocumentProjectAttachmentRepository attachmentRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final StorageService storageService;

    public SiteDocumentProjectService(
            SiteDocumentProjectRepository projectRepository,
            SiteDocumentProjectAttachmentRepository attachmentRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            StorageService storageService) {
        this.projectRepository = projectRepository;
        this.attachmentRepository = attachmentRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.storageService = storageService;
    }

    @Transactional
    public SiteDocumentProject create(UUID constructionSiteId, UUID actingUserId, String name) {
        var access = siteAccessService.requireAccess(constructionSiteId, actingUserId);
        permissionService.requireManage(constructionSiteId, access, PermissionCapability.DOCUMENT_PROJECTS);

        SiteDocumentProject project =
                new SiteDocumentProject(UUID.randomUUID(), constructionSiteId, name, actingUserId, Instant.now());
        return projectRepository.save(project);
    }

    public List<SiteDocumentProject> list(UUID constructionSiteId, UUID actingUserId) {
        siteAccessService.requireAccess(constructionSiteId, actingUserId);
        return projectRepository.findByConstructionSiteId(constructionSiteId);
    }

    public SiteDocumentProject get(UUID projectId, UUID actingUserId) {
        SiteDocumentProject project = requireProject(projectId);
        siteAccessService.requireAccess(project.getConstructionSiteId(), actingUserId);
        return project;
    }

    @Transactional
    public SiteDocumentProjectAttachment uploadAttachment(UUID projectId, UUID actingUserId, MultipartFile file) {
        SiteDocumentProject project = requireProject(projectId);
        var access = siteAccessService.requireAccess(project.getConstructionSiteId(), actingUserId);
        permissionService.requireManage(project.getConstructionSiteId(), access, PermissionCapability.DOCUMENT_PROJECTS);

        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new InvalidFileException("Only PDF files are accepted");
        }

        UUID attachmentId = UUID.randomUUID();
        String key = StorageKeys.siteDocumentProjectAttachmentKey(
                project.getConstructionSiteId(), projectId, attachmentId, "pdf");
        storageService.putObject(key, readBytes(file), file.getContentType());

        SiteDocumentProjectAttachment attachment = new SiteDocumentProjectAttachment(
                attachmentId, projectId, key, file.getContentType(), file.getOriginalFilename(), actingUserId,
                Instant.now());
        return attachmentRepository.save(attachment);
    }

    public List<SiteDocumentProjectAttachment> listAttachments(UUID projectId, UUID actingUserId) {
        SiteDocumentProject project = requireProject(projectId);
        siteAccessService.requireAccess(project.getConstructionSiteId(), actingUserId);
        return attachmentRepository.findBySiteDocumentProjectId(projectId);
    }

    public byte[] getAttachmentContent(UUID projectId, UUID attachmentId, UUID actingUserId) {
        SiteDocumentProject project = requireProject(projectId);
        siteAccessService.requireAccess(project.getConstructionSiteId(), actingUserId);
        SiteDocumentProjectAttachment attachment = attachmentRepository
                .findById(attachmentId)
                .orElseThrow(() -> new InvalidFileException("Attachment not found: " + attachmentId));
        return storageService.getObject(attachment.getStorageKey());
    }

    private SiteDocumentProject requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new SiteDocumentProjectNotFoundException(projectId));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
