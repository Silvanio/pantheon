package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.storage.StorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private SiteAccessService siteAccessService;

    @Mock
    private StorageService storageService;

    private DailyReportMediaService service;

    private UUID siteId;
    private UUID reportId;
    private DailyReport report;

    @BeforeEach
    void setUp() {
        service = new DailyReportMediaService(
                mediaRepository, attachmentRepository, dailyReportRepository, siteAccessService, storageService,
                25, 200, 25);
        siteId = UUID.randomUUID();
        reportId = UUID.randomUUID();
        report = new DailyReport(reportId, siteId, LocalDate.now(), 1, UUID.randomUUID(), Instant.now());
        lenient().when(dailyReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        lenient().when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private byte[] realPngBytes() throws Exception {
        BufferedImage image = new BufferedImage(500, 400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void uploadMediaGeneratesAndStoresThumbnailForPhoto() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "site.png", "image/png", realPngBytes());

        DailyReportMedia media = service.uploadMedia(reportId, UUID.randomUUID(), MediaType.PHOTO, file, null);

        assertThat(media.getThumbnailStorageKey()).isNotNull();
        assertThat(media.getThumbnailStorageKey()).contains("media-thumbnails").endsWith(".jpg");
        verify(storageService).putObject(eq(media.getStorageKey()), any(), eq("image/png"));
        verify(storageService).putObject(eq(media.getThumbnailStorageKey()), any(), eq("image/jpeg"));
    }

    @Test
    void uploadMediaDoesNotGenerateThumbnailForVideo() {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", "not-a-real-video".getBytes());

        DailyReportMedia media = service.uploadMedia(reportId, UUID.randomUUID(), MediaType.VIDEO, file, null);

        assertThat(media.getThumbnailStorageKey()).isNull();
        verify(storageService, times(1)).putObject(any(), any(), any());
    }

    @Test
    void uploadMediaFallsBackGracefullyWhenBytesArentADecodableImage() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "not-a-real-image".getBytes());

        DailyReportMedia media = service.uploadMedia(reportId, UUID.randomUUID(), MediaType.PHOTO, file, null);

        assertThat(media.getThumbnailStorageKey()).isNull();
        verify(storageService, times(1)).putObject(any(), any(), any());
    }

    @Test
    void getMediaThumbnailReturnsStoredThumbnailWhenAlreadyGenerated() {
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), reportId, MediaType.PHOTO, "media/original.jpg", "image/jpeg", null,
                UUID.randomUUID(), Instant.now(), "media-thumbnails/existing.jpg");
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));
        byte[] storedThumbnail = {1, 2, 3};
        when(storageService.getObject("media-thumbnails/existing.jpg")).thenReturn(storedThumbnail);

        byte[] result = service.getMediaThumbnail(reportId, media.getId(), UUID.randomUUID());

        assertThat(result).isEqualTo(storedThumbnail);
        verify(storageService, never()).getObject("media/original.jpg");
    }

    @Test
    void getMediaThumbnailLazilyGeneratesAndPersistsWhenMissing() throws Exception {
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), reportId, MediaType.PHOTO, "media/original.png", "image/png", null,
                UUID.randomUUID(), Instant.now());
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));
        when(storageService.getObject("media/original.png")).thenReturn(realPngBytes());

        byte[] result = service.getMediaThumbnail(reportId, media.getId(), UUID.randomUUID());

        assertThat(result).isNotEmpty();
        assertThat(media.getThumbnailStorageKey()).isNotNull().contains("media-thumbnails");
        verify(storageService).putObject(eq(media.getThumbnailStorageKey()), eq(result), eq("image/jpeg"));
        ArgumentCaptor<DailyReportMedia> saved = ArgumentCaptor.forClass(DailyReportMedia.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getThumbnailStorageKey()).isEqualTo(media.getThumbnailStorageKey());
    }

    @Test
    void getMediaThumbnailFallsBackToOriginalBytesWhenNotDecodable() {
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), reportId, MediaType.PHOTO, "media/original.bin", "application/octet-stream", null,
                UUID.randomUUID(), Instant.now());
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));
        byte[] originalBytes = "not-a-real-image".getBytes();
        when(storageService.getObject("media/original.bin")).thenReturn(originalBytes);

        byte[] result = service.getMediaThumbnail(reportId, media.getId(), UUID.randomUUID());

        assertThat(result).isEqualTo(originalBytes);
        assertThat(media.getThumbnailStorageKey()).isNull();
        verify(storageService, never()).putObject(anyString(), any(), anyString());
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void getMediaThumbnailReturnsOriginalBytesForVideoWithoutAttemptingGeneration() {
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), reportId, MediaType.VIDEO, "media/clip.mp4", "video/mp4", null,
                UUID.randomUUID(), Instant.now());
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));
        byte[] originalBytes = "video-bytes".getBytes();
        when(storageService.getObject("media/clip.mp4")).thenReturn(originalBytes);

        byte[] result = service.getMediaThumbnail(reportId, media.getId(), UUID.randomUUID());

        assertThat(result).isEqualTo(originalBytes);
        verify(mediaRepository, never()).save(any());
    }
}
