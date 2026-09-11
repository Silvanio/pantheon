package com.pantheon.service.controller;

import com.pantheon.service.dto.GlobalTaskBoardResponse;
import com.pantheon.service.dto.GlobalTaskCardResponse;
import com.pantheon.service.dto.TaskColumnResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.GlobalTaskBoardService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GlobalTaskBoardController {

    private final GlobalTaskBoardService globalTaskBoardService;

    public GlobalTaskBoardController(GlobalTaskBoardService globalTaskBoardService) {
        this.globalTaskBoardService = globalTaskBoardService;
    }

    @GetMapping("/api/companies/{companyId}/tasks-board")
    public ResponseEntity<GlobalTaskBoardResponse> getBoard(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID companyId) {
        var board = globalTaskBoardService.build(companyId, user.getId());

        List<TaskColumnResponse> columns = board.columns().stream().map(TaskColumnResponse::from).toList();
        List<GlobalTaskCardResponse> cards = board.cards().stream()
                .map(card -> GlobalTaskCardResponse.from(
                        card, board.siteById().get(card.getConstructionSiteId()),
                        board.labelIdsByCard().getOrDefault(card.getId(), List.of())))
                .toList();
        return ResponseEntity.ok(new GlobalTaskBoardResponse(columns, cards));
    }
}
