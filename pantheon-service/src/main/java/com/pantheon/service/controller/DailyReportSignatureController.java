package com.pantheon.service.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.DailyReportSignatureResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.service.DailyReportSignatureService;
@RestController
public class DailyReportSignatureController {

    private final DailyReportSignatureService signatureService;

    public DailyReportSignatureController(DailyReportSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @PostMapping("/api/daily-reports/{id}/signatures")
    public ResponseEntity<DailyReportSignatureResponse> sign(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        DailyReportSignature signature = signatureService.sign(id, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(DailyReportSignatureResponse.from(signature));
    }

    @GetMapping("/api/daily-reports/{id}/signatures")
    public ResponseEntity<List<DailyReportSignatureResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        List<DailyReportSignatureResponse> signatures =
                signatureService.list(id, user.getId()).stream().map(DailyReportSignatureResponse::from).toList();
        return ResponseEntity.ok(signatures);
    }
}
