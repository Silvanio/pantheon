package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.storage.StorageService;
import java.time.Instant;
import java.time.LocalDate;
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
class DailyReportMediaServiceTest {

    @Mock
    private DailyReportMediaRepository mediaRepository;

    @Mock
    private DailyReportAttachmentRepository attachmentRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    @Mock
    private StorageService storageService;

    private DailyReportMediaService mediaService;

    private UUID projectId;
    private UUID userId;
    private ConstructionSite site;
    private DailyReport report;

    @BeforeEach
    void setUp() {
        mediaService = new DailyReportMediaService(
                mediaRepository, attachmentRepository, dailyReportRepository, siteRepository, membershipRepository,
                storageService, 10, 100, 20);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Torre Norte", "Av. Central, 500", LocalDate.now(), null, userId,
                Instant.now());
        report = new DailyReport(UUID.randomUUID(), site.getId(), LocalDate.now(), 1, userId, Instant.now());

        lenient().when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        lenient().when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        lenient().when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(membership));
        lenient().when(mediaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(attachmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void uploadMediaStoresFileAndPersistsRecord() {
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "conteudo".getBytes());

        DailyReportMedia media = mediaService.uploadMedia(report.getId(), userId, MediaType.PHOTO, file, "Fundação");

        assertThat(media.getType()).isEqualTo(MediaType.PHOTO);
        assertThat(media.getCaption()).isEqualTo("Fundação");
        assertThat(media.getStorageKey()).contains(site.getId().toString()).contains(report.getId().toString());
        verify(storageService).putObject(media.getStorageKey(), "conteudo".getBytes(), "image/jpeg");
    }

    @Test
    void uploadMediaRejectedWhenOverSizeLimit() {
        MockMultipartFile bigFile =
                new MockMultipartFile("file", "video.mp4", "video/mp4", new byte[(int) (101 * 1024 * 1024)]);

        assertThatThrownBy(() -> mediaService.uploadMedia(report.getId(), userId, MediaType.VIDEO, bigFile, null))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void uploadMediaRejectedForNonMember() {
        UUID outsiderId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(projectId, outsiderId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", "x".getBytes());

        assertThatThrownBy(() -> mediaService.uploadMedia(report.getId(), outsiderId, MediaType.PHOTO, file, null))
                .isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    void listMediaReturnsEntriesInUploadOrder() {
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), report.getId(), MediaType.PHOTO, "key", "image/jpeg", null, userId, Instant.now());
        when(mediaRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId())).thenReturn(List.of(media));

        assertThat(mediaService.listMedia(report.getId(), userId)).containsExactly(media);
    }

    @Test
    void uploadAttachmentStoresFileAndPersistsRecord() {
        MockMultipartFile file =
                new MockMultipartFile("file", "planta.pdf", "application/pdf", "conteudo-pdf".getBytes());

        DailyReportAttachment attachment = mediaService.uploadAttachment(report.getId(), userId, file);

        assertThat(attachment.getOriginalName()).isEqualTo("planta.pdf");
        verify(storageService).putObject(attachment.getStorageKey(), "conteudo-pdf".getBytes(), "application/pdf");
    }

    @Test
    void uploadAttachmentRejectedWhenOverSizeLimit() {
        MockMultipartFile bigFile =
                new MockMultipartFile("file", "grande.pdf", "application/pdf", new byte[(int) (21 * 1024 * 1024)]);

        assertThatThrownBy(() -> mediaService.uploadAttachment(report.getId(), userId, bigFile))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void listAttachmentsReturnsEntries() {
        DailyReportAttachment attachment = new DailyReportAttachment(
                UUID.randomUUID(), report.getId(), "key", "application/pdf", "planta.pdf", userId, Instant.now());
        when(attachmentRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId()))
                .thenReturn(List.of(attachment));

        assertThat(mediaService.listAttachments(report.getId(), userId)).containsExactly(attachment);
    }
}
