package com.pantheon.service.controller;

import com.pantheon.service.dto.SiteSummaryResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.SiteSummaryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SiteSummaryController {

    private final SiteSummaryService siteSummaryService;

    public SiteSummaryController(SiteSummaryService siteSummaryService) {
        this.siteSummaryService = siteSummaryService;
    }

    @GetMapping("/api/construction-sites/{siteId}/summary")
    public ResponseEntity<SiteSummaryResponse> getSummary(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        return ResponseEntity.ok(siteSummaryService.build(siteId, user.getId()));
    }
}
