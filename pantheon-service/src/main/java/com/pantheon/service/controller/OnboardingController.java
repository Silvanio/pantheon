package com.pantheon.service.controller;

import com.pantheon.service.dto.OnboardingStatusResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final CompanyService companyService;

    public OnboardingController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusResponse> status(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(companyService.getOnboardingStatus(user.getId()));
    }
}
