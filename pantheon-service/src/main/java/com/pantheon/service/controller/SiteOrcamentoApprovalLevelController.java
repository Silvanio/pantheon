package com.pantheon.service.controller;

import com.pantheon.service.dto.SetOrcamentoApprovalLevelsRequest;
import com.pantheon.service.dto.SiteOrcamentoApprovalLevelResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.service.SiteAccessService;
import com.pantheon.service.service.SiteOrcamentoApprovalLevelService;
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

/** Company-staff-only configuration of a site's ordered Orcamento approval chain. */
@RestController
@RequestMapping("/api/construction-sites/{siteId}/orcamento-approval-levels")
public class SiteOrcamentoApprovalLevelController {

    private final SiteOrcamentoApprovalLevelService approvalLevelService;
    private final SiteAccessService siteAccessService;

    public SiteOrcamentoApprovalLevelController(
            SiteOrcamentoApprovalLevelService approvalLevelService, SiteAccessService siteAccessService) {
        this.approvalLevelService = approvalLevelService;
        this.siteAccessService = siteAccessService;
    }

    @GetMapping
    public ResponseEntity<List<SiteOrcamentoApprovalLevelResponse>> get(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        requireCompanyStaff(siteId, user.getId());
        List<SiteOrcamentoApprovalLevelResponse> levels = approvalLevelService
                .getEffectiveLevels(siteId)
                .stream()
                .map(SiteOrcamentoApprovalLevelResponse::from)
                .toList();
        return ResponseEntity.ok(levels);
    }

    @PutMapping
    public ResponseEntity<List<SiteOrcamentoApprovalLevelResponse>> set(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SetOrcamentoApprovalLevelsRequest request) {
        requireCompanyStaff(siteId, user.getId());
        List<SiteOrcamentoApprovalLevelResponse> levels = approvalLevelService
                .setLevels(siteId, request.levels())
                .stream()
                .map(SiteOrcamentoApprovalLevelResponse::from)
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
