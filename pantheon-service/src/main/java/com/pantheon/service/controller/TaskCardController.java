package com.pantheon.service.controller;

import com.pantheon.service.dto.MoveTaskCardRequest;
import com.pantheon.service.dto.TaskBoardResponse;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.dto.TaskCardResponse;
import com.pantheon.service.dto.TaskColumnResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.service.TaskCardService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskCardController {

    private final TaskCardService taskCardService;

    public TaskCardController(TaskCardService taskCardService) {
        this.taskCardService = taskCardService;
    }

    @GetMapping("/api/construction-sites/{siteId}/task-board")
    public ResponseEntity<TaskBoardResponse> getBoard(@AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        TaskCardService.TaskBoard board = taskCardService.getBoard(siteId, user.getId());

        List<TaskColumnResponse> columns = board.columns().stream().map(TaskColumnResponse::from).toList();
        List<TaskCardResponse> cards = board.cards().stream()
                .map(card -> TaskCardResponse.from(card, board.labelIdsByCard().getOrDefault(card.getId(), List.of())))
                .toList();
        return ResponseEntity.ok(new TaskBoardResponse(columns, cards));
    }

    @PostMapping("/api/construction-sites/{siteId}/task-cards")
    public ResponseEntity<TaskCardResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody TaskCardCreationRequest request) {
        TaskCard card = taskCardService.createCard(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskCardResponse.from(card, List.of()));
    }

    @PatchMapping("/api/task-cards/{cardId}/move")
    public ResponseEntity<TaskCardResponse> move(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID cardId,
            @Valid @RequestBody MoveTaskCardRequest request) {
        TaskCard card = taskCardService.moveCard(cardId, user.getId(), request);
        return ResponseEntity.ok(TaskCardResponse.from(card, taskCardService.labelIdsForCard(card.getId())));
    }
}
