package com.pantheon.service.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.EquipmentRegistrationRequest;
import com.pantheon.service.dto.EquipmentResponse;
import com.pantheon.service.dto.EquipmentStatusUpdateRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.service.EquipmentService;
@RestController
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @PostMapping("/api/construction-sites/{siteId}/equipment")
    public ResponseEntity<EquipmentResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody EquipmentRegistrationRequest request) {
        Equipment equipment = equipmentService.create(siteId, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipmentResponse.from(equipment));
    }

    @GetMapping("/api/construction-sites/{siteId}/equipment")
    public ResponseEntity<Page<EquipmentResponse>> list(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) EquipmentStatus status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EquipmentResponse> equipment = equipmentService
                .list(siteId, user.getId(), name, status, type, PageRequest.of(page, size))
                .map(EquipmentResponse::from);
        return ResponseEntity.ok(equipment);
    }

    @PatchMapping("/api/equipment/{id}/status")
    public ResponseEntity<EquipmentResponse> updateStatus(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody EquipmentStatusUpdateRequest request) {
        Equipment equipment = equipmentService.updateStatus(id, user.getId(), request.status());
        return ResponseEntity.ok(EquipmentResponse.from(equipment));
    }
}
