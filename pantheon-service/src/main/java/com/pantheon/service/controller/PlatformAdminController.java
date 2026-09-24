package com.pantheon.service.controller;

import com.pantheon.service.dto.CompanyResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.CompanyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Superadmin-only, cross-tenant endpoints — see {@code PlatformAdminService}. */
@RestController
public class PlatformAdminController {

    private final CompanyService companyService;

    public PlatformAdminController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/api/admin/companies")
    public ResponseEntity<Page<CompanyResponse>> listCompanies(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CompanyResponse> response = companyService
                .listAllCompanies(user.getId(), search, PageRequest.of(page, size))
                .map(CompanyResponse::from);
        return ResponseEntity.ok(response);
    }
}
