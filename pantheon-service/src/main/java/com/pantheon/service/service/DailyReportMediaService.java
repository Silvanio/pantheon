package com.pantheon.service.service;

import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DailyReportMediaService {

    /**
     * Longest-side cap for generated thumbnails, and the JPEG quality they're re-encoded at. The
     * media grid (web and mobile) only ever displays photos at ~84-112px cells, so serving a phone
     * camera's multi-megabyte original there was pure waste — every report load fired one
     * full-resolution authenticated request per photo, the same N+1-shaped cost as a per-row DB
     * query, just over HTTP instead of SQL.
     */
    private static final int THUMBNAIL_MAX_DIMENSION = 320;
    private static final float THUMBNAIL_JPEG_QUALITY = 0.8f;

    private final DailyReportMediaRepository mediaRepository;
    private final DailyReportAttachmentRepository attachmentRepository;
    private final DailyReportRepository dailyReportRepository;
    private final SiteAccessService siteAccessService;
    private final StorageService storageService;
    private final long maxPhotoBytes;
    private final long maxVideoBytes;
    private final long maxAttachmentBytes;

    public DailyReportMediaService(
            DailyReportMediaRepository mediaRepository,
            DailyReportAttachmentRepository attachmentRepository,
            DailyReportRepository dailyReportRepository,
            SiteAccessService siteAccessService,
            StorageService storageService,
            @Value("${pantheon.storage.max-photo-size-mb}") long maxPhotoSizeMb,
            @Value("${pantheon.storage.max-video-size-mb}") long maxVideoSizeMb,
            @Value("${pantheon.storage.max-attachment-size-mb}") long maxAttachmentSizeMb) {
        this.mediaRepository = mediaRepository;
        this.attachmentRepository = attachmentRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.siteAccessService = siteAccessService;
        this.storageService = storageService;
        this.maxPhotoBytes = maxPhotoSizeMb * 1024 * 1024;
        this.maxVideoBytes = maxVideoSizeMb * 1024 * 1024;
        this.maxAttachmentBytes = maxAttachmentSizeMb * 1024 * 1024;
    }

    @Transactional
    public DailyReportMedia uploadMedia(
            UUID reportId, UUID actingUserId, MediaType type, MultipartFile file, String caption) {
        DailyReport report = requireReport(reportId, actingUserId);
        long limit = type == MediaType.VIDEO ? maxVideoBytes : maxPhotoBytes;
        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        if (file.getSize() > limit) {
            throw new InvalidFileException("File exceeds the maximum allowed size for " + type);
        }

        UUID mediaId = UUID.randomUUID();
        String extension = extensionOf(file.getOriginalFilename());
        String key = StorageKeys.mediaKey(report.getConstructionSiteId(), reportId, mediaId, extension);
        byte[] bytes = readBytes(file);
        storageService.putObject(key, bytes, file.getContentType());

        String thumbnailKey = null;
        if (type == MediaType.PHOTO) {
            Optional<byte[]> thumbnail = generateThumbnail(bytes);
            if (thumbnail.isPresent()) {
                thumbnailKey = StorageKeys.mediaThumbnailKey(report.getConstructionSiteId(), reportId, mediaId);
                storageService.putObject(thumbnailKey, thumbnail.get(), "image/jpeg");
            }
        }

        DailyReportMedia media = new DailyReportMedia(
                mediaId, reportId, type, key, file.getContentType(), caption, actingUserId, Instant.now(), thumbnailKey);
        return mediaRepository.save(media);
    }

    public List<DailyReportMedia> listMedia(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return mediaRepository.findByDailyReportIdOrderByCreatedAtAsc(reportId);
    }

    public byte[] getMediaContent(UUID reportId, UUID mediaId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        DailyReportMedia media =
                mediaRepository.findById(mediaId).orElseThrow(() -> new InvalidFileException("Media not found: " + mediaId));
        return storageService.getObject(media.getStorageKey());
    }

    /**
     * Serves a small re-encoded copy of a photo instead of the original — see this class's
     * {@code THUMBNAIL_MAX_DIMENSION} doc comment. Media uploaded before thumbnails existed has no
     * {@code thumbnailStorageKey} yet; rather than a one-off backfill migration, it's generated
     * here on first request and persisted so every later request hits the stored thumbnail
     * directly. Falls back to the original bytes — same "degrade, don't fail" approach as the
     * AVIF/font fallbacks elsewhere in the PDF services — for non-PHOTO media, or a photo format
     * {@link ImageIO} can't decode.
     */
    @Transactional
    public byte[] getMediaThumbnail(UUID reportId, UUID mediaId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        DailyReportMedia media =
                mediaRepository.findById(mediaId).orElseThrow(() -> new InvalidFileException("Media not found: " + mediaId));
        if (media.getThumbnailStorageKey() != null) {
            return storageService.getObject(media.getThumbnailStorageKey());
        }

        byte[] original = storageService.getObject(media.getStorageKey());
        if (media.getType() != MediaType.PHOTO) {
            return original;
        }
        Optional<byte[]> thumbnail = generateThumbnail(original);
        if (thumbnail.isEmpty()) {
            return original;
        }
        DailyReport report = dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        String thumbnailKey = StorageKeys.mediaThumbnailKey(report.getConstructionSiteId(), reportId, mediaId);
        storageService.putObject(thumbnailKey, thumbnail.get(), "image/jpeg");
        media.assignThumbnail(thumbnailKey);
        mediaRepository.save(media);
        return thumbnail.get();
    }

    /**
     * Downscales to at most {@code THUMBNAIL_MAX_DIMENSION}px on the longest side (never
     * upscales) and re-encodes as JPEG. Returns {@link Optional#empty()} rather than throwing when
     * {@link ImageIO} can't decode the source (e.g. AVIF/HEIC, which Java has no built-in decoder
     * for — see {@code PdfBrandingService#looksLikeAvif}), so callers can fall back to the
     * original bytes instead of failing the request.
     */
    private Optional<byte[]> generateThumbnail(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                return Optional.empty();
            }
            int width = source.getWidth();
            int height = source.getHeight();
            double scale = Math.min(1.0, (double) THUMBNAIL_MAX_DIMENSION / Math.max(width, height));
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));

            BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(THUMBNAIL_JPEG_QUALITY);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resized, null, null), param);
            } finally {
                writer.dispose();
            }
            return Optional.of(out.toByteArray());
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public DailyReportAttachment uploadAttachment(UUID reportId, UUID actingUserId, MultipartFile file) {
        DailyReport report = requireReport(reportId, actingUserId);
        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        if (file.getSize() > maxAttachmentBytes) {
            throw new InvalidFileException("File exceeds the maximum allowed attachment size");
        }

        UUID attachmentId = UUID.randomUUID();
        String extension = extensionOf(file.getOriginalFilename());
        String key = StorageKeys.attachmentKey(report.getConstructionSiteId(), reportId, attachmentId, extension);
        storageService.putObject(key, readBytes(file), file.getContentType());

        DailyReportAttachment attachment = new DailyReportAttachment(
                attachmentId, reportId, key, file.getContentType(), file.getOriginalFilename(), actingUserId,
                Instant.now());
        return attachmentRepository.save(attachment);
    }

    public List<DailyReportAttachment> listAttachments(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return attachmentRepository.findByDailyReportIdOrderByCreatedAtAsc(reportId);
    }

    public byte[] getAttachmentContent(UUID reportId, UUID attachmentId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        DailyReportAttachment attachment = attachmentRepository
                .findById(attachmentId)
                .orElseThrow(() -> new InvalidFileException("Attachment not found: " + attachmentId));
        return storageService.getObject(attachment.getStorageKey());
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

    private DailyReport requireReport(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        siteAccessService.requireAccess(report.getConstructionSiteId(), actingUserId);
        return report;
    }
}
