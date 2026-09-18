package com.pantheon.service.controller;

import com.pantheon.service.dto.SetPurchaseRequestApprovalLevelsRequest;
import com.pantheon.service.dto.SitePurchaseRequestApprovalLevelResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.service.SiteAccessService;
import com.pantheon.service.service.SitePurchaseRequestApprovalLevelService;
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

/** Company-staff-only configuration of a site's ordered Pedido de Compra approval chain. */
@RestController
@RequestMapping("/api/construction-sites/{siteId}/purchase-request-approval-levels")
public class SitePurchaseRequestApprovalLevelController {

    private final SitePurchaseRequestApprovalLevelService approvalLevelService;
    private final SiteAccessService siteAccessService;

    public SitePurchaseRequestApprovalLevelController(
            SitePurchaseRequestApprovalLevelService approvalLevelService, SiteAccessService siteAccessService) {
        this.approvalLevelService = approvalLevelService;
        this.siteAccessService = siteAccessService;
    }

    @GetMapping
    public ResponseEntity<List<SitePurchaseRequestApprovalLevelResponse>> get(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        requireCompanyStaff(siteId, user.getId());
        List<SitePurchaseRequestApprovalLevelResponse> levels = approvalLevelService
                .getEffectiveLevels(siteId)
                .stream()
                .map(SitePurchaseRequestApprovalLevelResponse::from)
                .toList();
        return ResponseEntity.ok(levels);
    }

    @PutMapping
    public ResponseEntity<List<SitePurchaseRequestApprovalLevelResponse>> set(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SetPurchaseRequestApprovalLevelsRequest request) {
        requireCompanyStaff(siteId, user.getId());
        List<SitePurchaseRequestApprovalLevelResponse> levels = approvalLevelService
                .setLevels(siteId, request.levels())
                .stream()
                .map(SitePurchaseRequestApprovalLevelResponse::from)
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
