package com.pantheon.service.controller;

import com.pantheon.service.dto.TaskCommentRequest;
import com.pantheon.service.dto.TaskCommentResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.TaskCommentService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    public TaskCommentController(TaskCommentService taskCommentService) {
        this.taskCommentService = taskCommentService;
    }

    @GetMapping("/api/task-cards/{cardId}/comments")
    public ResponseEntity<List<TaskCommentResponse>> list(@AuthenticationPrincipal AppUser user, @PathVariable UUID cardId) {
        List<TaskCommentResponse> comments =
                taskCommentService.list(cardId, user.getId()).stream().map(TaskCommentResponse::from).toList();
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/api/task-cards/{cardId}/comments")
    public ResponseEntity<TaskCommentResponse> add(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID cardId,
            @Valid @RequestBody TaskCommentRequest request) {
        var comment = taskCommentService.add(cardId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskCommentResponse.from(comment));
    }
}
