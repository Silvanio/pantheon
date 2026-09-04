package com.pantheon.service.controller;

import java.util.List;
import java.util.UUID;
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
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
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
