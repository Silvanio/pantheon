package com.pantheon.service.service;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteDocumentProject;
import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.SiteDocumentAttachmentNotFoundException;
import com.pantheon.service.exception.SiteDocumentProjectNotFoundException;
import com.pantheon.service.exception.TaskCardNotFoundException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.SiteDocumentProjectAttachmentRepository;
import com.pantheon.service.repository.SiteDocumentProjectRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import com.pantheon.service.validation.Mp4DurationReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * A construction site's "Projetos" explorer: a tree of folders ({@link SiteDocumentProject},
 * self-referencing via {@code parentId}) holding files ({@link SiteDocumentProjectAttachment}),
 * both optionally at the site's root. See redesign-site-projects-as-folder-explorer's design.md.
 */
@Service
public class SiteDocumentProjectService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf", "mp4");
    private static final int MAX_VIDEO_DURATION_SECONDS = 60;

    private final SiteDocumentProjectRepository projectRepository;
    private final SiteDocumentProjectAttachmentRepository attachmentRepository;
    private final TaskCardRepository taskCardRepository;
    private final AppUserRepository userRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final StorageService storageService;
    private final TaskCardService taskCardService;

    public SiteDocumentProjectService(
            SiteDocumentProjectRepository projectRepository,
            SiteDocumentProjectAttachmentRepository attachmentRepository,
            TaskCardRepository taskCardRepository,
            AppUserRepository userRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            StorageService storageService,
            TaskCardService taskCardService) {
        this.projectRepository = projectRepository;
        this.attachmentRepository = attachmentRepository;
        this.taskCardRepository = taskCardRepository;
        this.userRepository = userRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.storageService = storageService;
        this.taskCardService = taskCardService;
    }

    @Transactional
    public SiteDocumentProject createFolder(
            UUID constructionSiteId, UUID actingUserId, String name, UUID parentId, UUID taskCardId) {
        requireManageProjects(constructionSiteId, actingUserId);
        if (parentId != null) {
            requireFolderOnSite(parentId, constructionSiteId);
        }
        if (taskCardId != null) {
            requireTaskCardOnSite(taskCardId, constructionSiteId);
        }

        Instant now = Instant.now();
        SiteDocumentProject folder = new SiteDocumentProject(
                UUID.randomUUID(), constructionSiteId, parentId, name, taskCardId, actingUserId, now, actingUserId, now);
        return projectRepository.save(folder);
    }

    @Transactional
    public SiteDocumentProject renameFolder(UUID folderId, UUID actingUserId, String name) {
        SiteDocumentProject folder = requireFolder(folderId);
        requireManageProjects(folder.getConstructionSiteId(), actingUserId);

        folder.rename(name, actingUserId, Instant.now());
        return projectRepository.save(folder);
    }

    @Transactional
    public SiteDocumentProject setFolderTaskLink(UUID folderId, UUID actingUserId, UUID taskCardId) {
        SiteDocumentProject folder = requireFolder(folderId);
        requireManageProjects(folder.getConstructionSiteId(), actingUserId);
        requireTaskCardOnSite(taskCardId, folder.getConstructionSiteId());

        folder.linkTask(taskCardId, actingUserId, Instant.now());
        return projectRepository.save(folder);
    }

    @Transactional
    public SiteDocumentProject clearFolderTaskLink(UUID folderId, UUID actingUserId) {
        SiteDocumentProject folder = requireFolder(folderId);
        requireManageProjects(folder.getConstructionSiteId(), actingUserId);

        folder.linkTask(null, actingUserId, Instant.now());
        return projectRepository.save(folder);
    }

    /**
     * Deletes a folder and its entire subtree. Object storage has no cascading delete, so every
     * descendant's attachment content is purged first; the database's own {@code ON DELETE
     * CASCADE} then removes every descendant folder and file row when the root folder is deleted.
     */
    @Transactional
    public void deleteFolder(UUID folderId, UUID actingUserId) {
        SiteDocumentProject folder = requireFolder(folderId);
        requireManageProjects(folder.getConstructionSiteId(), actingUserId);

        List<UUID> subtreeFolderIds = collectSubtreeFolderIds(folderId);
        List<SiteDocumentProjectAttachment> attachments =
                attachmentRepository.findBySiteDocumentProjectIdIn(subtreeFolderIds);
        attachments.forEach(attachment -> storageService.deleteObject(attachment.getStorageKey()));
        List<UUID> affectedTaskCardIds =
                attachments.stream().map(SiteDocumentProjectAttachment::getTaskCardId).filter(Objects::nonNull).distinct().toList();

        projectRepository.delete(folder);

        affectedTaskCardIds.forEach(taskCardService::publishCardUpdated);
    }

    @Transactional
    public SiteDocumentProjectAttachment uploadFile(
            UUID constructionSiteId, UUID actingUserId, UUID parentId, MultipartFile file) {
        requireManageProjects(constructionSiteId, actingUserId);
        if (parentId != null) {
            requireFolderOnSite(parentId, constructionSiteId);
        }
        return storeFile(constructionSiteId, parentId, null, actingUserId, file);
    }

    /**
     * The only way a file ever gains a task link: uploaded from the task's own detail view, into
     * a folder chosen from the Projetos tree. Gated by {@code MANAGE(TASKS)} (this is a task
     * action) plus {@code VISIBLE(DOCUMENT_PROJECTS)} (the member must be able to browse Projetos
     * to pick a destination) — deliberately narrower than the {@code MANAGE(DOCUMENT_PROJECTS)}
     * required for a direct upload from the Projetos screen. See design.md decision 5.
     */
    @Transactional
    public SiteDocumentProjectAttachment uploadFileFromTask(
            UUID taskCardId, UUID actingUserId, UUID parentId, MultipartFile file) {
        TaskCard card = requireTaskCard(taskCardId);
        var access = siteAccessService.requireAccess(card.getConstructionSiteId(), actingUserId);
        permissionService.requireManage(card.getConstructionSiteId(), access, PermissionCapability.TASKS);
        permissionService.requireVisible(card.getConstructionSiteId(), access, PermissionCapability.DOCUMENT_PROJECTS);
        if (parentId != null) {
            requireFolderOnSite(parentId, card.getConstructionSiteId());
        }
        SiteDocumentProjectAttachment attachment = storeFile(card.getConstructionSiteId(), parentId, taskCardId, actingUserId, file);
        taskCardService.publishCardUpdated(taskCardId);
        return attachment;
    }

    @Transactional
    public void deleteFile(UUID attachmentId, UUID actingUserId) {
        SiteDocumentProjectAttachment attachment = requireAttachment(attachmentId);
        requireManageProjects(attachment.getConstructionSiteId(), actingUserId);

        storageService.deleteObject(attachment.getStorageKey());
        attachmentRepository.delete(attachment);

        if (attachment.getTaskCardId() != null) {
            taskCardService.publishCardUpdated(attachment.getTaskCardId());
        }
    }

    public FileContent getFileContent(UUID attachmentId, UUID actingUserId) {
        SiteDocumentProjectAttachment attachment = requireAttachment(attachmentId);
        requireVisibleProjects(attachment.getConstructionSiteId(), actingUserId);

        byte[] bytes = storageService.getObject(attachment.getStorageKey());
        return new FileContent(bytes, attachment.getContentType(), attachment.getOriginalName());
    }

    public SiteDocumentProject getFolder(UUID folderId, UUID actingUserId) {
        SiteDocumentProject folder = requireFolder(folderId);
        requireVisibleProjects(folder.getConstructionSiteId(), actingUserId);
        return folder;
    }

    public FolderContents listContents(UUID constructionSiteId, UUID actingUserId, UUID parentId) {
        requireVisibleProjects(constructionSiteId, actingUserId);
        if (parentId != null) {
            requireFolderOnSite(parentId, constructionSiteId);
        }
        List<SiteDocumentProject> folders = projectRepository.findChildren(constructionSiteId, parentId);
        List<SiteDocumentProjectAttachment> files = attachmentRepository.findChildren(constructionSiteId, parentId);
        return new FolderContents(folders, files);
    }

    /** Ordered root-to-self ancestor chain, inclusive of {@code folderId}. */
    public List<SiteDocumentProject> getBreadcrumbs(UUID folderId, UUID actingUserId) {
        SiteDocumentProject folder = requireFolder(folderId);
        requireVisibleProjects(folder.getConstructionSiteId(), actingUserId);
        return ancestorChainInclusive(folder);
    }

    /** Files linked to {@code taskCardId}, readable by anyone with at least VIEW on TASKS. */
    public List<SiteDocumentProjectAttachment> listAttachmentsForTaskCard(UUID taskCardId, UUID actingUserId) {
        TaskCard card = requireTaskCard(taskCardId);
        var access = siteAccessService.requireAccess(card.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(card.getConstructionSiteId(), access, PermissionCapability.TASKS);
        return attachmentRepository.findByTaskCardId(taskCardId);
    }

    /** Human-readable path (e.g. {@code "Estrutural / Fundação"}), or {@code null} for a root-level file. */
    public String folderPathFor(SiteDocumentProjectAttachment attachment) {
        if (attachment.getSiteDocumentProjectId() == null) {
            return null;
        }
        return projectRepository.findById(attachment.getSiteDocumentProjectId())
                .map(folder -> ancestorChainInclusive(folder).stream()
                        .map(SiteDocumentProject::getName)
                        .collect(Collectors.joining(" / ")))
                .orElse(null);
    }

    /** Same "display name, fallback to email" resolution {@code TaskCommentService} uses for comment authors. */
    public Map<UUID, String> displayNamesFor(List<UUID> userIds) {
        List<UUID> distinct = userIds.stream().filter(Objects::nonNull).distinct().toList();
        return userRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(AppUser::getId, SiteDocumentProjectService::displayNameOrEmail));
    }

    public Map<UUID, String> taskTitlesFor(List<UUID> taskCardIds) {
        List<UUID> distinct = taskCardIds.stream().filter(Objects::nonNull).distinct().toList();
        return taskCardRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(TaskCard::getId, TaskCard::getTitle));
    }

    private static String displayNameOrEmail(AppUser user) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getEmail();
    }

    private SiteDocumentProjectAttachment storeFile(
            UUID constructionSiteId, UUID parentId, UUID taskCardId, UUID actingUserId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "Only these file types are accepted: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        byte[] bytes = readBytes(file);
        if ("mp4".equals(extension)) {
            OptionalDouble durationSeconds = Mp4DurationReader.durationSeconds(bytes);
            if (durationSeconds.isEmpty() || durationSeconds.getAsDouble() > MAX_VIDEO_DURATION_SECONDS) {
                throw new InvalidFileException(
                        "Videos must be " + MAX_VIDEO_DURATION_SECONDS + " seconds or shorter");
            }
        }

        UUID attachmentId = UUID.randomUUID();
        String key = parentId != null
                ? StorageKeys.siteDocumentProjectAttachmentKey(constructionSiteId, parentId, attachmentId, extension)
                : StorageKeys.siteDocumentRootAttachmentKey(constructionSiteId, attachmentId, extension);
        // Browsers may omit the content type for an unrecognized extension; fall back to a generic
        // binary type so it is always a valid MediaType when served back for download.
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String originalName = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "arquivo." + extension;
        storageService.putObject(key, bytes, contentType);

        SiteDocumentProjectAttachment attachment = new SiteDocumentProjectAttachment(
                attachmentId, constructionSiteId, parentId, key, contentType, originalName,
                taskCardId, actingUserId, Instant.now());
        return attachmentRepository.save(attachment);
    }

    private List<UUID> collectSubtreeFolderIds(UUID rootFolderId) {
        List<UUID> ids = new ArrayList<>();
        Deque<UUID> pending = new ArrayDeque<>(List.of(rootFolderId));
        while (!pending.isEmpty()) {
            UUID current = pending.poll();
            ids.add(current);
            projectRepository.findByParentId(current).forEach(child -> pending.add(child.getId()));
        }
        return ids;
    }

    private List<SiteDocumentProject> ancestorChainInclusive(SiteDocumentProject folder) {
        List<SiteDocumentProject> chain = new ArrayList<>();
        SiteDocumentProject current = folder;
        while (current != null) {
            chain.add(current);
            current = current.getParentId() != null ? requireFolder(current.getParentId()) : null;
        }
        java.util.Collections.reverse(chain);
        return chain;
    }

    private void requireFolderOnSite(UUID folderId, UUID constructionSiteId) {
        SiteDocumentProject folder = requireFolder(folderId);
        if (!folder.getConstructionSiteId().equals(constructionSiteId)) {
            throw new SiteDocumentProjectNotFoundException(folderId);
        }
    }

    private void requireTaskCardOnSite(UUID taskCardId, UUID constructionSiteId) {
        TaskCard card = requireTaskCard(taskCardId);
        if (!card.getConstructionSiteId().equals(constructionSiteId)) {
            throw new TaskCardNotFoundException(taskCardId);
        }
    }

    private void requireManageProjects(UUID constructionSiteId, UUID actingUserId) {
        var access = siteAccessService.requireAccess(constructionSiteId, actingUserId);
        permissionService.requireManage(constructionSiteId, access, PermissionCapability.DOCUMENT_PROJECTS);
    }

    private void requireVisibleProjects(UUID constructionSiteId, UUID actingUserId) {
        var access = siteAccessService.requireAccess(constructionSiteId, actingUserId);
        permissionService.requireVisible(constructionSiteId, access, PermissionCapability.DOCUMENT_PROJECTS);
    }

    private SiteDocumentProject requireFolder(UUID folderId) {
        return projectRepository.findById(folderId).orElseThrow(() -> new SiteDocumentProjectNotFoundException(folderId));
    }

    private SiteDocumentProjectAttachment requireAttachment(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new SiteDocumentAttachmentNotFoundException(attachmentId));
    }

    private TaskCard requireTaskCard(UUID taskCardId) {
        return taskCardRepository.findById(taskCardId).orElseThrow(() -> new TaskCardNotFoundException(taskCardId));
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "bin";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "bin";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public record FolderContents(List<SiteDocumentProject> folders, List<SiteDocumentProjectAttachment> files) {
    }

    public record FileContent(byte[] bytes, String contentType, String originalName) {
    }
}
