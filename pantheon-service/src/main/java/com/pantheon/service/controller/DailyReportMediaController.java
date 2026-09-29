package com.pantheon.service.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pantheon.service.dto.DailyReportAttachmentResponse;
import com.pantheon.service.dto.DailyReportMediaResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.service.DailyReportMediaService;
@RestController
public class DailyReportMediaController {

    /** A media id's bytes are immutable once uploaded, so both content endpoints can be cached hard. */
    private static final CacheControl MEDIA_CACHE_CONTROL =
            CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().immutable();

    private final DailyReportMediaService mediaService;

    public DailyReportMediaController(DailyReportMediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/api/daily-reports/{id}/media")
    public ResponseEntity<DailyReportMediaResponse> uploadMedia(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @RequestParam com.pantheon.service.entity.MediaType type,
            @RequestParam(required = false) String caption) {
        DailyReportMedia media = mediaService.uploadMedia(id, user.getId(), type, file, caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(DailyReportMediaResponse.from(media));
    }

    @GetMapping("/api/daily-reports/{id}/media")
    public ResponseEntity<List<DailyReportMediaResponse>> listMedia(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        List<DailyReportMediaResponse> media =
                mediaService.listMedia(id, user.getId()).stream().map(DailyReportMediaResponse::from).toList();
        return ResponseEntity.ok(media);
    }

    @GetMapping("/api/daily-reports/{id}/media/{mediaId}/content")
    public ResponseEntity<byte[]> getMediaContent(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID mediaId) {
        byte[] content = mediaService.getMediaContent(id, mediaId, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(MEDIA_CACHE_CONTROL)
                .eTag(mediaId.toString())
                .body(content);
    }

    /**
     * Small re-encoded copy of a photo, used by the media grid instead of {@code /content} — see
     * {@code DailyReportMediaService#getMediaThumbnail}'s doc comment for why the grid was fetching
     * full-resolution originals for a ~100px cell. A media id's bytes never change once uploaded,
     * so this is cached aggressively rather than refetched on every report view.
     */
    @GetMapping("/api/daily-reports/{id}/media/{mediaId}/thumbnail")
    public ResponseEntity<byte[]> getMediaThumbnail(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID mediaId) {
        // Not always actually JPEG: falls back to the original's own bytes/type (PNG, a video file,
        // ...) when thumbnail generation isn't possible — see getMediaThumbnail's doc comment.
        // APPLICATION_OCTET_STREAM, same as /content below, avoids declaring a MIME type that
        // doesn't match the fallback's real bytes.
        byte[] content = mediaService.getMediaThumbnail(id, mediaId, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(MEDIA_CACHE_CONTROL)
                .eTag(mediaId.toString())
                .body(content);
    }

    @PostMapping("/api/daily-reports/{id}/attachments")
    public ResponseEntity<DailyReportAttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        DailyReportAttachment attachment = mediaService.uploadAttachment(id, user.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(DailyReportAttachmentResponse.from(attachment));
    }

    @GetMapping("/api/daily-reports/{id}/attachments")
    public ResponseEntity<List<DailyReportAttachmentResponse>> listAttachments(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        List<DailyReportAttachmentResponse> attachments = mediaService.listAttachments(id, user.getId()).stream()
                .map(DailyReportAttachmentResponse::from)
                .toList();
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/api/daily-reports/{id}/attachments/{attachmentId}/content")
    public ResponseEntity<byte[]> getAttachmentContent(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID attachmentId) {
        byte[] content = mediaService.getAttachmentContent(id, attachmentId, user.getId());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
    }
}
