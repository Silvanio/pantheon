package com.pantheon.service.controller;

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

import com.pantheon.service.dto.ActivityRequest;
import com.pantheon.service.dto.ActivityResponse;
import com.pantheon.service.dto.DailyReportCoreUpdateRequest;
import com.pantheon.service.dto.DailyReportCreationRequest;
import com.pantheon.service.dto.DailyReportDetailResponse;
import com.pantheon.service.dto.DailyReportResponse;
import com.pantheon.service.dto.EquipmentUsageRequest;
import com.pantheon.service.dto.EquipmentUsageResponse;
import com.pantheon.service.dto.MaterialReceivedRequest;
import com.pantheon.service.dto.MaterialReceivedResponse;
import com.pantheon.service.dto.OccurrenceRequest;
import com.pantheon.service.dto.OccurrenceResponse;
import com.pantheon.service.dto.WorkforceEntryRequest;
import com.pantheon.service.dto.WorkforceEntryResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.service.DailyReportService;
@RestController
public class DailyReportController {

    private final DailyReportService dailyReportService;

    public DailyReportController(DailyReportService dailyReportService) {
        this.dailyReportService = dailyReportService;
    }

    @PostMapping("/api/construction-sites/{siteId}/daily-reports")
    public ResponseEntity<DailyReportResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody DailyReportCreationRequest request) {
        DailyReport report = dailyReportService.create(siteId, user.getId(), request.reportDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(DailyReportResponse.from(report));
    }

    @GetMapping("/api/construction-sites/{siteId}/daily-reports")
    public ResponseEntity<List<DailyReportResponse>> list(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        List<DailyReportResponse> reports =
                dailyReportService.list(siteId, user.getId()).stream().map(DailyReportResponse::from).toList();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/api/daily-reports/{id}")
    public ResponseEntity<DailyReportDetailResponse> getDetail(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(dailyReportService.getDetail(id, user.getId()));
    }

    @PatchMapping("/api/daily-reports/{id}")
    public ResponseEntity<DailyReportResponse> updateCore(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestBody DailyReportCoreUpdateRequest request) {
        DailyReport report = dailyReportService.updateCore(id, user.getId(), request);
        return ResponseEntity.ok(DailyReportResponse.from(report));
    }

    @PostMapping("/api/daily-reports/{id}/submit")
    public ResponseEntity<DailyReportResponse> submit(@AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        DailyReport report = dailyReportService.submit(id, user.getId());
        return ResponseEntity.ok(DailyReportResponse.from(report));
    }

    @PostMapping("/api/daily-reports/{id}/workforce-entries")
    public ResponseEntity<WorkforceEntryResponse> addWorkforceEntry(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody WorkforceEntryRequest request) {
        var entry = dailyReportService.addWorkforceEntry(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkforceEntryResponse.from(entry));
    }

    @GetMapping("/api/daily-reports/{id}/workforce-entries")
    public ResponseEntity<List<WorkforceEntryResponse>> listWorkforceEntries(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var entries = dailyReportService.listWorkforceEntries(id, user.getId()).stream()
                .map(WorkforceEntryResponse::from)
                .toList();
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/api/daily-reports/{id}/equipment-usage")
    public ResponseEntity<EquipmentUsageResponse> addEquipmentUsage(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody EquipmentUsageRequest request) {
        var usage = dailyReportService.addEquipmentUsage(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipmentUsageResponse.from(usage));
    }

    @GetMapping("/api/daily-reports/{id}/equipment-usage")
    public ResponseEntity<List<EquipmentUsageResponse>> listEquipmentUsage(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var usage = dailyReportService.listEquipmentUsage(id, user.getId()).stream()
                .map(EquipmentUsageResponse::from)
                .toList();
        return ResponseEntity.ok(usage);
    }

    @PostMapping("/api/daily-reports/{id}/activities")
    public ResponseEntity<ActivityResponse> addActivity(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody ActivityRequest request) {
        var activity = dailyReportService.addActivity(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityResponse.from(activity));
    }

    @GetMapping("/api/daily-reports/{id}/activities")
    public ResponseEntity<List<ActivityResponse>> listActivities(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var activities =
                dailyReportService.listActivities(id, user.getId()).stream().map(ActivityResponse::from).toList();
        return ResponseEntity.ok(activities);
    }

    @PostMapping("/api/daily-reports/{id}/occurrences")
    public ResponseEntity<OccurrenceResponse> addOccurrence(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody OccurrenceRequest request) {
        var occurrence = dailyReportService.addOccurrence(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(OccurrenceResponse.from(occurrence));
    }

    @GetMapping("/api/daily-reports/{id}/occurrences")
    public ResponseEntity<List<OccurrenceResponse>> listOccurrences(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var occurrences =
                dailyReportService.listOccurrences(id, user.getId()).stream().map(OccurrenceResponse::from).toList();
        return ResponseEntity.ok(occurrences);
    }

    @PostMapping("/api/daily-reports/{id}/materials-received")
    public ResponseEntity<MaterialReceivedResponse> addMaterialReceived(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialReceivedRequest request) {
        var received = dailyReportService.addMaterialReceived(id, user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaterialReceivedResponse.from(received));
    }

    @GetMapping("/api/daily-reports/{id}/materials-received")
    public ResponseEntity<List<MaterialReceivedResponse>> listMaterialsReceived(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        var received = dailyReportService.listMaterialsReceived(id, user.getId()).stream()
                .map(MaterialReceivedResponse::from)
                .toList();
        return ResponseEntity.ok(received);
    }
}
