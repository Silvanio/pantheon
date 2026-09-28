package com.pantheon.service.controller;

import com.pantheon.service.dto.SetDailyReportApprovalLevelsRequest;
import com.pantheon.service.dto.SiteDailyReportApprovalLevelResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.service.SiteAccessService;
import com.pantheon.service.service.SiteDailyReportApprovalLevelService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Company-staff-only configuration of a site's ordered Diário de Obra approval chain. */
@RestController
@RequestMapping("/api/construction-sites/{siteId}/daily-report-approval-levels")
public class SiteDailyReportApprovalLevelController {

    private final SiteDailyReportApprovalLevelService approvalLevelService;
    private final SiteAccessService siteAccessService;

    public SiteDailyReportApprovalLevelController(
            SiteDailyReportApprovalLevelService approvalLevelService, SiteAccessService siteAccessService) {
        this.approvalLevelService = approvalLevelService;
        this.siteAccessService = siteAccessService;
    }

    @GetMapping
    public ResponseEntity<List<SiteDailyReportApprovalLevelResponse>> get(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        requireCompanyStaff(siteId, user.getId());
        List<SiteDailyReportApprovalLevelResponse> levels = approvalLevelService
                .getEffectiveLevels(siteId)
                .stream()
                .map(SiteDailyReportApprovalLevelResponse::from)
                .toList();
        return ResponseEntity.ok(levels);
    }

    @PutMapping
    public ResponseEntity<List<SiteDailyReportApprovalLevelResponse>> set(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SetDailyReportApprovalLevelsRequest request) {
        requireCompanyStaff(siteId, user.getId());
        List<SiteDailyReportApprovalLevelResponse> levels = approvalLevelService
                .setLevels(siteId, request.levels())
                .stream()
                .map(SiteDailyReportApprovalLevelResponse::from)
                .toList();
        return ResponseEntity.ok(levels);
    }

    private void requireCompanyStaff(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        if (!access.companyStaff()) {
            throw new NotSiteMemberException(siteId);
        }
    }
}
