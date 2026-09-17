package com.pantheon.service.controller;

import com.pantheon.service.dto.MoveTaskCardRequest;
import com.pantheon.service.dto.SiteDocumentProjectAttachmentResponse;
import com.pantheon.service.dto.TaskBoardResponse;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.dto.TaskCardResponse;
import com.pantheon.service.dto.TaskColumnResponse;
import com.pantheon.service.dto.TaskLabelResponse;
import com.pantheon.service.dto.UpdateTaskCardDueDateRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.service.SiteDocumentProjectService;
import com.pantheon.service.service.TaskCardService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TaskCardController {

    private final TaskCardService taskCardService;
    private final SiteDocumentProjectService documentProjectService;

    public TaskCardController(TaskCardService taskCardService, SiteDocumentProjectService documentProjectService) {
        this.taskCardService = taskCardService;
        this.documentProjectService = documentProjectService;
    }

    @GetMapping("/api/construction-sites/{siteId}/task-board")
    public ResponseEntity<TaskBoardResponse> getBoard(@AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        TaskCardService.TaskBoard board = taskCardService.getBoard(siteId, user.getId());

        List<TaskColumnResponse> columns = board.columns().stream().map(TaskColumnResponse::from).toList();
        List<TaskCardResponse> cards = board.cards().stream()
                .map(card -> TaskCardResponse.from(
                        card, board.labelIdsByCard().getOrDefault(card.getId(), List.of()),
                        board.assigneeIdsByCard().getOrDefault(card.getId(), List.of()),
                        board.commentCountByCard().getOrDefault(card.getId(), 0L),
                        board.attachmentCountByCard().getOrDefault(card.getId(), 0L)))
                .toList();
        List<TaskLabelResponse> labels = board.labels().stream().map(TaskLabelResponse::from).toList();
        return ResponseEntity.ok(new TaskBoardResponse(columns, cards, labels));
    }

    @PostMapping("/api/construction-sites/{siteId}/task-cards")
    public ResponseEntity<TaskCardResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody TaskCardCreationRequest request) {
        TaskCard card = taskCardService.createCard(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskCardResponse.from(card, List.of(), List.of(), 0L, 0L));
    }

    @PatchMapping("/api/task-cards/{cardId}/move")
    public ResponseEntity<TaskCardResponse> move(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID cardId,
            @Valid @RequestBody MoveTaskCardRequest request) {
        TaskCard card = taskCardService.moveCard(cardId, user.getId(), request);
        return ResponseEntity.ok(respond(card));
    }

    @PatchMapping("/api/task-cards/{cardId}/due-date")
    public ResponseEntity<TaskCardResponse> updateDueDate(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID cardId,
            @RequestBody UpdateTaskCardDueDateRequest request) {
        TaskCard card = taskCardService.updateDueDate(cardId, user.getId(), request);
        return ResponseEntity.ok(respond(card));
    }

    @PostMapping("/api/task-cards/{cardId}/assignees/{siteMembershipId}")
    public ResponseEntity<Void> assign(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID cardId, @PathVariable UUID siteMembershipId) {
        taskCardService.assign(cardId, user.getId(), siteMembershipId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/task-cards/{cardId}/assignees/{siteMembershipId}")
    public ResponseEntity<Void> unassign(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID cardId, @PathVariable UUID siteMembershipId) {
        taskCardService.unassign(cardId, user.getId(), siteMembershipId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/task-cards/{cardId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUser user, @PathVariable UUID cardId) {
        taskCardService.deleteCard(cardId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/task-cards/{cardId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SiteDocumentProjectAttachmentResponse> attachFile(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID cardId,
            @RequestParam(required = false) UUID parentId,
            @RequestParam MultipartFile file) {
        SiteDocumentProjectAttachment attachment =
                documentProjectService.uploadFileFromTask(cardId, user.getId(), parentId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAttachmentResponse(attachment));
    }

    @GetMapping("/api/task-cards/{cardId}/attachments")
    public ResponseEntity<List<SiteDocumentProjectAttachmentResponse>> listAttachments(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID cardId) {
        List<SiteDocumentProjectAttachment> attachments =
                documentProjectService.listAttachmentsForTaskCard(cardId, user.getId());
        Map<UUID, String> uploaderNames = documentProjectService.displayNamesFor(
                attachments.stream().map(SiteDocumentProjectAttachment::getUploadedBy).toList());
        List<SiteDocumentProjectAttachmentResponse> response = attachments.stream()
                .map(attachment -> SiteDocumentProjectAttachmentResponse.from(
                        attachment, uploaderNames.get(attachment.getUploadedBy()), documentProjectService.folderPathFor(attachment)))
                .toList();
        return ResponseEntity.ok(response);
    }

    private SiteDocumentProjectAttachmentResponse toAttachmentResponse(SiteDocumentProjectAttachment attachment) {
        String uploaderName = documentProjectService.displayNamesFor(List.of(attachment.getUploadedBy())).get(attachment.getUploadedBy());
        return SiteDocumentProjectAttachmentResponse.from(attachment, uploaderName, documentProjectService.folderPathFor(attachment));
    }

    private TaskCardResponse respond(TaskCard card) {
        return TaskCardResponse.from(
                card, taskCardService.labelIdsForCard(card.getId()), taskCardService.assigneeIdsForCard(card.getId()),
                taskCardService.commentCountForCard(card.getId()), taskCardService.attachmentCountForCard(card.getId()));
    }
}
