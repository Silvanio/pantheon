package com.pantheon.service.controller;

import com.pantheon.service.dto.ScheduleDependencyRequest;
import com.pantheon.service.dto.ScheduleStageCreationRequest;
import com.pantheon.service.dto.ScheduleStageResponse;
import com.pantheon.service.dto.ScheduleStageUpdateRequest;
import com.pantheon.service.dto.ScheduleTaskCreationRequest;
import com.pantheon.service.dto.ScheduleTaskResponse;
import com.pantheon.service.dto.ScheduleTaskUpdateRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ScheduleTaskDependency;
import com.pantheon.service.service.ScheduleService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/api/construction-sites/{siteId}/schedule-stages")
    public ResponseEntity<List<ScheduleStageResponse>> listStages(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        return ResponseEntity.ok(scheduleService.listStages(siteId, user.getId()));
    }

    @PostMapping("/api/construction-sites/{siteId}/schedule-stages")
    public ResponseEntity<ScheduleStageResponse> createStage(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody ScheduleStageCreationRequest request) {
        ScheduleStageResponse stage = scheduleService.createStage(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(stage);
    }

    @PatchMapping("/api/schedule-stages/{id}")
    public ResponseEntity<ScheduleStageResponse> updateStage(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestBody ScheduleStageUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateStage(id, user.getId(), request));
    }

    @DeleteMapping("/api/schedule-stages/{id}")
    public ResponseEntity<Void> deleteStage(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        scheduleService.deleteStage(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/schedule-stages/{stageId}/tasks")
    public ResponseEntity<ScheduleTaskResponse> createTask(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID stageId,
            @Valid @RequestBody ScheduleTaskCreationRequest request) {
        ScheduleTaskResponse task = scheduleService.createTask(stageId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }

    @PatchMapping("/api/schedule-tasks/{id}")
    public ResponseEntity<ScheduleTaskResponse> updateTask(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestBody ScheduleTaskUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateTask(id, user.getId(), request));
    }

    @DeleteMapping("/api/schedule-tasks/{id}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        scheduleService.deleteTask(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/schedule-tasks/{id}/dependencies")
    public ResponseEntity<ScheduleTaskDependency> linkDependency(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleDependencyRequest request) {
        ScheduleTaskDependency dependency = scheduleService.linkDependency(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dependency);
    }

    @DeleteMapping("/api/schedule-tasks/{id}/dependencies/{depId}")
    public ResponseEntity<Void> unlinkDependency(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @PathVariable UUID depId) {
        scheduleService.unlinkDependency(depId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
