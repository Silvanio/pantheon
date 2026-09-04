package com.pantheon.service.service;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DailyReportMediaService {

    private final DailyReportMediaRepository mediaRepository;
    private final DailyReportAttachmentRepository attachmentRepository;
    private final DailyReportRepository dailyReportRepository;
    private final ConstructionSiteRepository siteRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final StorageService storageService;
    private final long maxPhotoBytes;
    private final long maxVideoBytes;
    private final long maxAttachmentBytes;

    public DailyReportMediaService(
            DailyReportMediaRepository mediaRepository,
            DailyReportAttachmentRepository attachmentRepository,
            DailyReportRepository dailyReportRepository,
            ConstructionSiteRepository siteRepository,
            ProjectMembershipRepository membershipRepository,
            StorageService storageService,
            @Value("${pantheon.storage.max-photo-size-mb}") long maxPhotoSizeMb,
            @Value("${pantheon.storage.max-video-size-mb}") long maxVideoSizeMb,
            @Value("${pantheon.storage.max-attachment-size-mb}") long maxAttachmentSizeMb) {
        this.mediaRepository = mediaRepository;
        this.attachmentRepository = attachmentRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
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
        storageService.putObject(key, readBytes(file), file.getContentType());

        DailyReportMedia media = new DailyReportMedia(
                mediaId, reportId, type, key, file.getContentType(), caption, actingUserId, Instant.now());
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
        ConstructionSite site = siteRepository
                .findById(report.getConstructionSiteId())
                .orElseThrow(() -> new ConstructionSiteNotFoundException(report.getConstructionSiteId()));
        membershipRepository
                .findByProjectIdAndUserId(site.getProjectId(), actingUserId)
                .orElseThrow(() -> new NotProjectMemberException(site.getProjectId()));
        return report;
    }
}
