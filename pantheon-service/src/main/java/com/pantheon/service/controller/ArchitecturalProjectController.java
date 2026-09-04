package com.pantheon.service.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.ArchitecturalProjectRegistrationRequest;
import com.pantheon.service.dto.ArchitecturalProjectResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ArchitecturalProject;
import com.pantheon.service.service.ArchitecturalProjectService;
@RestController
@RequestMapping("/api/projects/{projectId}/architectural-projects")
public class ArchitecturalProjectController {

    private final ArchitecturalProjectService architecturalProjectService;

    public ArchitecturalProjectController(ArchitecturalProjectService architecturalProjectService) {
        this.architecturalProjectService = architecturalProjectService;
    }

    @PostMapping
    public ResponseEntity<ArchitecturalProjectResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody ArchitecturalProjectRegistrationRequest request) {
        ArchitecturalProject architecturalProject =
                architecturalProjectService.create(projectId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ArchitecturalProjectResponse.from(architecturalProject));
    }

    @GetMapping
    public ResponseEntity<List<ArchitecturalProjectResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID projectId) {
        List<ArchitecturalProjectResponse> projects =
                architecturalProjectService.list(projectId, user.getId()).stream()
                        .map(ArchitecturalProjectResponse::from)
                        .toList();
        return ResponseEntity.ok(projects);
    }
}
