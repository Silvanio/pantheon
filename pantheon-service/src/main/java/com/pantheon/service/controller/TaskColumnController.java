package com.pantheon.service.controller;

import com.pantheon.service.dto.TaskColumnReorderRequest;
import com.pantheon.service.dto.TaskColumnRequest;
import com.pantheon.service.dto.TaskColumnResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.TaskColumnService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/task-columns")
public class TaskColumnController {

    private final TaskColumnService taskColumnService;

    public TaskColumnController(TaskColumnService taskColumnService) {
        this.taskColumnService = taskColumnService;
    }

    @GetMapping
    public ResponseEntity<List<TaskColumnResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID companyId) {
        List<TaskColumnResponse> columns = taskColumnService.list(companyId, user.getId()).stream()
                .map(TaskColumnResponse::from)
                .toList();
        return ResponseEntity.ok(columns);
    }

    @PostMapping
    public ResponseEntity<TaskColumnResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID companyId,
            @Valid @RequestBody TaskColumnRequest request) {
        var column = taskColumnService.create(companyId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskColumnResponse.from(column));
    }

    @PutMapping("/{columnId}")
    public ResponseEntity<TaskColumnResponse> rename(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID companyId,
            @PathVariable UUID columnId,
            @Valid @RequestBody TaskColumnRequest request) {
        var column = taskColumnService.rename(columnId, user.getId(), request);
        return ResponseEntity.ok(TaskColumnResponse.from(column));
    }

    @PutMapping("/{columnId}/sort-order")
    public ResponseEntity<TaskColumnResponse> reorder(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID companyId,
            @PathVariable UUID columnId,
            @Valid @RequestBody TaskColumnReorderRequest request) {
        var column = taskColumnService.reorder(columnId, user.getId(), request.sortOrder());
        return ResponseEntity.ok(TaskColumnResponse.from(column));
    }

    @DeleteMapping("/{columnId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID companyId, @PathVariable UUID columnId) {
        taskColumnService.delete(columnId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
