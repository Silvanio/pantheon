package com.pantheon.service.controller;

import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.DailyReportPdfService;
@RestController
public class DailyReportPdfController {

    private final DailyReportPdfService pdfService;

    public DailyReportPdfController(DailyReportPdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/api/daily-reports/{id}/pdf")
    public ResponseEntity<byte[]> getPdf(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        byte[] pdf = pdfService.generate(id, user.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("diario-obra-" + id + ".pdf").build().toString())
                .body(pdf);
    }
}
