package com.pantheon.service.controller;

import com.pantheon.service.dto.TaskLabelRequest;
import com.pantheon.service.dto.TaskLabelResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.CompanyTaskLabelService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/task-labels")
public class CompanyTaskLabelController {

    private final CompanyTaskLabelService companyTaskLabelService;

    public CompanyTaskLabelController(CompanyTaskLabelService companyTaskLabelService) {
        this.companyTaskLabelService = companyTaskLabelService;
    }

    @GetMapping
    public ResponseEntity<List<TaskLabelResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID companyId) {
        List<TaskLabelResponse> labels = companyTaskLabelService.list(companyId, user.getId()).stream()
                .map(TaskLabelResponse::from)
                .toList();
        return ResponseEntity.ok(labels);
    }

    @PostMapping
    public ResponseEntity<TaskLabelResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID companyId,
            @Valid @RequestBody TaskLabelRequest request) {
        var label = companyTaskLabelService.create(companyId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskLabelResponse.from(label));
    }

    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID companyId, @PathVariable UUID labelId) {
        companyTaskLabelService.delete(labelId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
