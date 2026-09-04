package com.pantheon.service.controller;

import com.pantheon.service.dto.SetFunctionPermissionRequest;
import com.pantheon.service.dto.SetMemberPermissionRequest;
import com.pantheon.service.dto.SitePermissionOverrideResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.NotSiteMemberException;
import com.pantheon.service.service.SiteAccessService;
import com.pantheon.service.service.SitePermissionService;
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

@RestController
@RequestMapping("/api/sites/{siteId}/permissions")
public class SitePermissionController {

    private final SitePermissionService permissionService;
    private final SiteAccessService siteAccessService;

    public SitePermissionController(SitePermissionService permissionService, SiteAccessService siteAccessService) {
        this.permissionService = permissionService;
        this.siteAccessService = siteAccessService;
    }

    @GetMapping
    public ResponseEntity<List<SitePermissionOverrideResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        requireCompanyStaff(siteId, user.getId());
        List<SitePermissionOverrideResponse> overrides =
                permissionService.listOverrides(siteId).stream().map(SitePermissionOverrideResponse::from).toList();
        return ResponseEntity.ok(overrides);
    }

    @PutMapping("/function")
    public ResponseEntity<SitePermissionOverrideResponse> setFunctionOverride(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SetFunctionPermissionRequest request) {
        requireCompanyStaff(siteId, user.getId());
        var override = permissionService.setFunctionOverride(
                siteId, request.function(), request.capability(), request.accessLevel());
        return ResponseEntity.ok(SitePermissionOverrideResponse.from(override));
    }

    @PutMapping("/member")
    public ResponseEntity<SitePermissionOverrideResponse> setMemberOverride(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody SetMemberPermissionRequest request) {
        requireCompanyStaff(siteId, user.getId());
        var override = permissionService.setMemberOverride(
                siteId, request.siteMembershipId(), request.capability(), request.accessLevel());
        return ResponseEntity.ok(SitePermissionOverrideResponse.from(override));
    }

    private void requireCompanyStaff(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        if (!access.companyStaff()) {
            throw new NotSiteMemberException(siteId);
        }
    }
}
