package com.pantheon.service.controller;

import com.pantheon.service.entity.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.OnboardingStatusResponse;
import com.pantheon.service.service.ProjectService;
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final ProjectService projectService;

    public OnboardingController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusResponse> status(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(projectService.getOnboardingStatus(user.getId()));
    }
}
