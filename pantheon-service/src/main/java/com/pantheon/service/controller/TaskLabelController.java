package com.pantheon.service.controller;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.dto.TaskLabelResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.TaskLabelService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskLabelController {

    private final TaskLabelService taskLabelService;

    public TaskLabelController(TaskLabelService taskLabelService) {
        this.taskLabelService = taskLabelService;
    }

    @GetMapping("/api/construction-sites/{siteId}/task-labels")
    public ResponseEntity<List<TaskLabelResponse>> list(@AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        List<TaskLabelResponse> labels =
                taskLabelService.list(siteId, user.getId()).stream().map(TaskLabelResponse::from).toList();
        return ResponseEntity.ok(labels);
    }

    @PostMapping("/api/construction-sites/{siteId}/task-labels")
    public ResponseEntity<TaskLabelResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody TaskLabelRequest request) {
        var label = taskLabelService.create(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskLabelResponse.from(label));
    }

    @PostMapping("/api/task-cards/{cardId}/labels/{labelId}")
    public ResponseEntity<Void> attach(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID cardId, @PathVariable UUID labelId) {
        taskLabelService.attach(cardId, labelId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/task-cards/{cardId}/labels/{labelId}")
    public ResponseEntity<Void> detach(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID cardId, @PathVariable UUID labelId) {
        taskLabelService.detach(cardId, labelId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
