package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteDocumentProject;
import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.SiteDocumentProjectNotFoundException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.SiteDocumentProjectAttachmentRepository;
import com.pantheon.service.repository.SiteDocumentProjectRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.storage.StorageService;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class SiteDocumentProjectServiceTest {

    @Mock
    private SiteDocumentProjectRepository projectRepository;

    @Mock
    private SiteDocumentProjectAttachmentRepository attachmentRepository;

    @Mock
    private TaskCardRepository taskCardRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private StorageService storageService;

    @Mock
    private TaskCardService taskCardService;

    private SiteDocumentProjectService service;

    private UUID siteId;
    private UUID actingUserId;

    @BeforeEach
    void setUp() {
        service = new SiteDocumentProjectService(
                projectRepository, attachmentRepository, taskCardRepository, userRepository, siteAccessService,
                permissionService, storageService, taskCardService);

        siteId = UUID.randomUUID();
        actingUserId = UUID.randomUUID();
        lenient().when(siteAccessService.requireAccess(eq(siteId), eq(actingUserId))).thenReturn(new SiteAccessContext(true, null));
        lenient().when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(attachmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SiteDocumentProject folder(UUID id, UUID parentId) {
        return new SiteDocumentProject(id, siteId, parentId, "Pasta", null, actingUserId, Instant.now(), actingUserId, Instant.now());
    }

    private TaskCard taskCard(UUID id, UUID cardSiteId) {
        return new TaskCard(id, cardSiteId, UUID.randomUUID(), "Título", null, null, 0, actingUserId, Instant.now(), Instant.now());
    }

    @Test
    void createRootFolderPersistsWithNullParent() {
        SiteDocumentProject created = service.createFolder(siteId, actingUserId, "Estrutural", null, null);

        assertThat(created.getParentId()).isNull();
        assertThat(created.getName()).isEqualTo("Estrutural");
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.DOCUMENT_PROJECTS));
    }

    @Test
    void createNestedFolderRequiresParentOnSameSite() {
        UUID parentId = UUID.randomUUID();
        when(projectRepository.findById(parentId)).thenReturn(Optional.of(folder(parentId, null)));

        SiteDocumentProject created = service.createFolder(siteId, actingUserId, "Fundação", parentId, null);

        assertThat(created.getParentId()).isEqualTo(parentId);
    }

    @Test
    void createFolderRejectsParentFromAnotherSite() {
        UUID parentId = UUID.randomUUID();
        SiteDocumentProject foreignParent =
                new SiteDocumentProject(parentId, UUID.randomUUID(), null, "Outra obra", null, actingUserId, Instant.now(), actingUserId, Instant.now());
        when(projectRepository.findById(parentId)).thenReturn(Optional.of(foreignParent));

        assertThatThrownBy(() -> service.createFolder(siteId, actingUserId, "Fundação", parentId, null))
                .isInstanceOf(SiteDocumentProjectNotFoundException.class);
    }

    @Test
    void viewOnlyMemberCannotCreateFolder() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.DOCUMENT_PROJECTS))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.DOCUMENT_PROJECTS));

        assertThatThrownBy(() -> service.createFolder(siteId, actingUserId, "Estrutural", null, null))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void renameFolderUpdatesNameAndAuditFields() {
        UUID folderId = UUID.randomUUID();
        when(projectRepository.findById(folderId)).thenReturn(Optional.of(folder(folderId, null)));

        SiteDocumentProject renamed = service.renameFolder(folderId, actingUserId, "Nova estrutural");

        assertThat(renamed.getName()).isEqualTo("Nova estrutural");
        assertThat(renamed.getUpdatedBy()).isEqualTo(actingUserId);
    }

    @Test
    void deleteFolderPurgesStorageForEveryDescendantAttachmentBeforeDeletingTheRow() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        SiteDocumentProject root = folder(rootId, null);
        SiteDocumentProject child = folder(childId, rootId);
        when(projectRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(projectRepository.findByParentId(rootId)).thenReturn(List.of(child));
        when(projectRepository.findByParentId(childId)).thenReturn(List.of());

        SiteDocumentProjectAttachment rootAttachment = new SiteDocumentProjectAttachment(
                UUID.randomUUID(), siteId, rootId, "key-root", "application/pdf", "a.pdf", null, actingUserId, Instant.now());
        SiteDocumentProjectAttachment childAttachment = new SiteDocumentProjectAttachment(
                UUID.randomUUID(), siteId, childId, "key-child", "application/pdf", "b.pdf", null, actingUserId, Instant.now());
        when(attachmentRepository.findBySiteDocumentProjectIdIn(List.of(rootId, childId)))
                .thenReturn(List.of(rootAttachment, childAttachment));

        service.deleteFolder(rootId, actingUserId);

        verify(storageService).deleteObject("key-root");
        verify(storageService).deleteObject("key-child");
        verify(projectRepository).delete(root);
        verify(taskCardService, never()).publishCardUpdated(any());
    }

    @Test
    void deletingAFolderWithTaskLinkedAttachmentsPublishesCardUpdateForEachDistinctCard() {
        UUID rootId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        SiteDocumentProject root = folder(rootId, null);
        when(projectRepository.findById(rootId)).thenReturn(Optional.of(root));
        when(projectRepository.findByParentId(rootId)).thenReturn(List.of());

        SiteDocumentProjectAttachment first = new SiteDocumentProjectAttachment(
                UUID.randomUUID(), siteId, rootId, "key-1", "application/pdf", "a.pdf", cardId, actingUserId, Instant.now());
        SiteDocumentProjectAttachment second = new SiteDocumentProjectAttachment(
                UUID.randomUUID(), siteId, rootId, "key-2", "application/pdf", "b.pdf", cardId, actingUserId, Instant.now());
        when(attachmentRepository.findBySiteDocumentProjectIdIn(List.of(rootId))).thenReturn(List.of(first, second));

        service.deleteFolder(rootId, actingUserId);

        verify(taskCardService, times(1)).publishCardUpdated(cardId);
    }

    @Test
    void setAndClearFolderTaskLink() {
        UUID folderId = UUID.randomUUID();
        UUID taskCardId = UUID.randomUUID();
        when(projectRepository.findById(folderId)).thenReturn(Optional.of(folder(folderId, null)));
        when(taskCardRepository.findById(taskCardId)).thenReturn(Optional.of(taskCard(taskCardId, siteId)));

        SiteDocumentProject linked = service.setFolderTaskLink(folderId, actingUserId, taskCardId);
        assertThat(linked.getTaskCardId()).isEqualTo(taskCardId);

        SiteDocumentProject cleared = service.clearFolderTaskLink(folderId, actingUserId);
        assertThat(cleared.getTaskCardId()).isNull();
    }

    @Test
    void uploadFileAtRootAndNested() {
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "conteudo".getBytes());

        SiteDocumentProjectAttachment rootFile = service.uploadFile(siteId, actingUserId, null, file);
        assertThat(rootFile.getSiteDocumentProjectId()).isNull();
        assertThat(rootFile.getConstructionSiteId()).isEqualTo(siteId);

        UUID folderId = UUID.randomUUID();
        when(projectRepository.findById(folderId)).thenReturn(Optional.of(folder(folderId, null)));
        SiteDocumentProjectAttachment nestedFile = service.uploadFile(siteId, actingUserId, folderId, file);
        assertThat(nestedFile.getSiteDocumentProjectId()).isEqualTo(folderId);
    }

    @Test
    void uploadFileAcceptsEveryAllowedExtension() {
        MockMultipartFile jpg = new MockMultipartFile("file", "foto.JPG", "image/jpeg", "conteudo".getBytes());
        MockMultipartFile jpeg = new MockMultipartFile("file", "foto.jpeg", "image/jpeg", "conteudo".getBytes());
        MockMultipartFile png = new MockMultipartFile("file", "foto.png", "image/png", "conteudo".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("file", "contrato.pdf", "application/pdf", "conteudo".getBytes());
        MockMultipartFile mp4 = new MockMultipartFile("file", "clipe.mp4", "video/mp4", buildMp4(1000, 30_000));

        assertThat(service.uploadFile(siteId, actingUserId, null, jpg).getOriginalName()).isEqualTo("foto.JPG");
        assertThat(service.uploadFile(siteId, actingUserId, null, jpeg).getOriginalName()).isEqualTo("foto.jpeg");
        assertThat(service.uploadFile(siteId, actingUserId, null, png).getOriginalName()).isEqualTo("foto.png");
        assertThat(service.uploadFile(siteId, actingUserId, null, pdf).getOriginalName()).isEqualTo("contrato.pdf");
        assertThat(service.uploadFile(siteId, actingUserId, null, mp4).getOriginalName()).isEqualTo("clipe.mp4");
    }

    @Test
    void uploadFileRejectsDisallowedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "planta.dwg", "application/octet-stream", "conteudo".getBytes());

        assertThatThrownBy(() -> service.uploadFile(siteId, actingUserId, null, file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void uploadFileAcceptsVideoWithinDurationCap() {
        MockMultipartFile file = new MockMultipartFile("file", "clipe.mp4", "video/mp4", buildMp4(1000, 60_000));

        assertThat(service.uploadFile(siteId, actingUserId, null, file)).isNotNull();
    }

    @Test
    void uploadFileRejectsVideoExceedingDurationCap() {
        MockMultipartFile file = new MockMultipartFile("file", "clipe.mp4", "video/mp4", buildMp4(1000, 90_000));

        assertThatThrownBy(() -> service.uploadFile(siteId, actingUserId, null, file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void uploadFileRejectsVideoWithUndeterminableDuration() {
        MockMultipartFile file = new MockMultipartFile("file", "clipe.mp4", "video/mp4", "not a real mp4 file".getBytes());

        assertThatThrownBy(() -> service.uploadFile(siteId, actingUserId, null, file))
                .isInstanceOf(InvalidFileException.class);
    }

    /** Builds the minimal valid MP4 byte layout (ftyp + moov > mvhd, version 0) our duration check needs. */
    private static byte[] buildMp4(int timescale, int durationUnits) {
        try {
            var mvhdContent = new java.io.ByteArrayOutputStream();
            var mvhdOut = new java.io.DataOutputStream(mvhdContent);
            mvhdOut.writeInt(0); // version(1) + flags(3)
            mvhdOut.writeInt(0); // creation_time
            mvhdOut.writeInt(0); // modification_time
            mvhdOut.writeInt(timescale);
            mvhdOut.writeInt(durationUnits);
            byte[] mvhdContentBytes = mvhdContent.toByteArray();

            var out = new java.io.ByteArrayOutputStream();
            var dos = new java.io.DataOutputStream(out);
            dos.writeInt(16); // ftyp box: 8-byte header + 8-byte content
            dos.writeBytes("ftyp");
            dos.writeBytes("isom");
            dos.writeInt(0);

            int mvhdBoxSize = 8 + mvhdContentBytes.length;
            dos.writeInt(8 + mvhdBoxSize); // moov box size
            dos.writeBytes("moov");
            dos.writeInt(mvhdBoxSize);
            dos.writeBytes("mvhd");
            dos.write(mvhdContentBytes);

            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void uploadEmptyFileRejected() {
        MockMultipartFile empty = new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.uploadFile(siteId, actingUserId, null, empty))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void uploadFileFromTaskRequiresManageTasksAndVisibleDocumentProjects() {
        UUID cardId = UUID.randomUUID();
        when(taskCardRepository.findById(cardId)).thenReturn(Optional.of(taskCard(cardId, siteId)));
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        SiteDocumentProjectAttachment attachment = service.uploadFileFromTask(cardId, actingUserId, null, file);

        assertThat(attachment.getTaskCardId()).isEqualTo(cardId);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
        verify(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.DOCUMENT_PROJECTS));
        verify(taskCardService).publishCardUpdated(cardId);
    }

    @Test
    void uploadFileFromTaskRejectedWhenTasksAccessInsufficient() {
        UUID cardId = UUID.randomUUID();
        when(taskCardRepository.findById(cardId)).thenReturn(Optional.of(taskCard(cardId, siteId)));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.TASKS))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        assertThatThrownBy(() -> service.uploadFileFromTask(cardId, actingUserId, null, file))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void uploadFileFromTaskRejectedWhenDocumentProjectsHidden() {
        UUID cardId = UUID.randomUUID();
        when(taskCardRepository.findById(cardId)).thenReturn(Optional.of(taskCard(cardId, siteId)));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.DOCUMENT_PROJECTS))
                .when(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.DOCUMENT_PROJECTS));
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        assertThatThrownBy(() -> service.uploadFileFromTask(cardId, actingUserId, null, file))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void deleteFilePurgesStorageAndRow() {
        UUID attachmentId = UUID.randomUUID();
        SiteDocumentProjectAttachment attachment = new SiteDocumentProjectAttachment(
                attachmentId, siteId, null, "key-1", "application/pdf", "a.pdf", null, actingUserId, Instant.now());
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        service.deleteFile(attachmentId, actingUserId);

        verify(storageService).deleteObject("key-1");
        verify(attachmentRepository).delete(attachment);
        verify(taskCardService, never()).publishCardUpdated(any());
    }

    @Test
    void deletingATaskLinkedFilePublishesCardUpdate() {
        UUID attachmentId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        SiteDocumentProjectAttachment attachment = new SiteDocumentProjectAttachment(
                attachmentId, siteId, null, "key-1", "application/pdf", "a.pdf", cardId, actingUserId, Instant.now());
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        service.deleteFile(attachmentId, actingUserId);

        verify(taskCardService).publishCardUpdated(cardId);
    }

    @Test
    void listContentsAtRootAndNestedLevel() {
        when(projectRepository.findChildren(siteId, null)).thenReturn(List.of(folder(UUID.randomUUID(), null)));
        when(attachmentRepository.findChildren(siteId, null)).thenReturn(List.of());

        var rootContents = service.listContents(siteId, actingUserId, null);
        assertThat(rootContents.folders()).hasSize(1);

        UUID folderId = UUID.randomUUID();
        when(projectRepository.findById(folderId)).thenReturn(Optional.of(folder(folderId, null)));
        when(projectRepository.findChildren(siteId, folderId)).thenReturn(List.of());
        when(attachmentRepository.findChildren(siteId, folderId)).thenReturn(List.of());

        var nestedContents = service.listContents(siteId, actingUserId, folderId);
        assertThat(nestedContents.folders()).isEmpty();
        verify(permissionService, times(2)).requireVisible(eq(siteId), any(), eq(PermissionCapability.DOCUMENT_PROJECTS));
    }

    @Test
    void breadcrumbsWalkFromRootToSelfInclusive() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        SiteDocumentProject root = folder(rootId, null);
        SiteDocumentProject child = folder(childId, rootId);
        when(projectRepository.findById(childId)).thenReturn(Optional.of(child));
        when(projectRepository.findById(rootId)).thenReturn(Optional.of(root));

        List<SiteDocumentProject> breadcrumbs = service.getBreadcrumbs(childId, actingUserId);

        assertThat(breadcrumbs).extracting(SiteDocumentProject::getId).containsExactly(rootId, childId);
    }

    @Test
    void listAttachmentsForTaskCardRequiresAtLeastViewOnTasks() {
        UUID cardId = UUID.randomUUID();
        when(taskCardRepository.findById(cardId)).thenReturn(Optional.of(taskCard(cardId, siteId)));
        when(attachmentRepository.findByTaskCardId(cardId)).thenReturn(List.of());

        service.listAttachmentsForTaskCard(cardId, actingUserId);

        verify(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.TASKS));
        verify(permissionService, never()).requireManage(eq(siteId), any(), eq(PermissionCapability.TASKS));
    }

    @Test
    void folderPathForRootAttachmentIsNull() {
        SiteDocumentProjectAttachment rootAttachment = new SiteDocumentProjectAttachment(
                UUID.randomUUID(), siteId, null, "key", "application/pdf", "a.pdf", null, actingUserId, Instant.now());

        assertThat(service.folderPathFor(rootAttachment)).isNull();
    }

    @Test
    void folderPathForNestedAttachmentJoinsAncestorNames() {
        UUID grandparentId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        SiteDocumentProject grandparent = new SiteDocumentProject(
                grandparentId, siteId, null, "Estrutural", null, actingUserId, Instant.now(), actingUserId, Instant.now());
        SiteDocumentProject parent = new SiteDocumentProject(
                parentId, siteId, grandparentId, "Fundação", null, actingUserId, Instant.now(), actingUserId, Instant.now());
        when(projectRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(projectRepository.findById(grandparentId)).thenReturn(Optional.of(grandparent));
        SiteDocumentProjectAttachment attachment = new SiteDocumentProjectAttachment(
                UUID.randomUUID(), siteId, parentId, "key", "application/pdf", "a.pdf", null, actingUserId, Instant.now());

        assertThat(service.folderPathFor(attachment)).isEqualTo("Estrutural / Fundação");
    }
}
