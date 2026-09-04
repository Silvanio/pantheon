package com.pantheon.service.controller;

import com.pantheon.service.entity.AppUser;
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

import com.pantheon.service.dto.AddMemberRequest;
import com.pantheon.service.dto.MemberInvitationResponse;
import com.pantheon.service.dto.PlanSelectionRequest;
import com.pantheon.service.dto.ProjectMemberResponse;
import com.pantheon.service.dto.ProjectMembershipResponse;
import com.pantheon.service.dto.ProjectRegistrationRequest;
import com.pantheon.service.dto.ProjectResponse;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.Project;
import com.pantheon.service.service.ProjectService;
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal AppUser user, @Valid @RequestBody ProjectRegistrationRequest request) {
        Project project = projectService.create(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProjectMembershipResponse>> myProjects(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(projectService.listMyProjects(user.getId()));
    }

    @PostMapping("/{id}/plan")
    public ResponseEntity<ProjectResponse> confirmPlan(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody PlanSelectionRequest request) {
        Project project = projectService.confirmPlan(id, user.getId(), request.plan());
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberInvitationResponse> addMember(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        MembershipInvitation invitation =
                projectService.addMember(id, user.getId(), request.email(), request.function(), request.specialty());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MemberInvitationResponse(
                invitation.getId(),
                invitation.getMembershipId(),
                invitation.getEmail(),
                invitation.isRequiresRegistration()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.listMembers(id, user.getId()));
    }
}
