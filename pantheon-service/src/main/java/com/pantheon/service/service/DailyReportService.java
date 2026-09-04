package com.pantheon.service.service;

import com.pantheon.service.dto.ActivityRequest;
import com.pantheon.service.dto.DailyReportCoreUpdateRequest;
import com.pantheon.service.dto.DailyReportDetailResponse;
import com.pantheon.service.dto.DailyReportResponse;
import com.pantheon.service.dto.EquipmentUsageRequest;
import com.pantheon.service.dto.MaterialReceivedRequest;
import com.pantheon.service.dto.OccurrenceRequest;
import com.pantheon.service.dto.WorkforceEntryRequest;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMaterialReceived;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportNotEditableException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.DuplicateDailyReportException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportEquipmentUsageRepository;
import com.pantheon.service.repository.DailyReportMaterialReceivedRepository;
import com.pantheon.service.repository.DailyReportOccurrenceRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;
    private final DailyReportWorkforceEntryRepository workforceEntryRepository;
    private final DailyReportEquipmentUsageRepository equipmentUsageRepository;
    private final DailyReportActivityRepository activityRepository;
    private final DailyReportOccurrenceRepository occurrenceRepository;
    private final DailyReportMaterialReceivedRepository materialReceivedRepository;
    private final ConstructionSiteRepository siteRepository;
    private final ProjectMembershipRepository membershipRepository;

    public DailyReportService(
            DailyReportRepository dailyReportRepository,
            DailyReportWorkforceEntryRepository workforceEntryRepository,
            DailyReportEquipmentUsageRepository equipmentUsageRepository,
            DailyReportActivityRepository activityRepository,
            DailyReportOccurrenceRepository occurrenceRepository,
            DailyReportMaterialReceivedRepository materialReceivedRepository,
            ConstructionSiteRepository siteRepository,
            ProjectMembershipRepository membershipRepository) {
        this.dailyReportRepository = dailyReportRepository;
        this.workforceEntryRepository = workforceEntryRepository;
        this.equipmentUsageRepository = equipmentUsageRepository;
        this.activityRepository = activityRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.materialReceivedRepository = materialReceivedRepository;
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public DailyReport create(UUID siteId, UUID actingUserId, LocalDate reportDate) {
        ConstructionSite site = requireSite(siteId);
        requireMembership(site.getProjectId(), actingUserId);

        if (dailyReportRepository.findByConstructionSiteIdAndReportDate(siteId, reportDate).isPresent()) {
            throw new DuplicateDailyReportException(siteId, reportDate);
        }

        long nextSequence = dailyReportRepository.countByConstructionSiteId(siteId) + 1;
        DailyReport report =
                new DailyReport(UUID.randomUUID(), siteId, reportDate, (int) nextSequence, actingUserId, Instant.now());
        return dailyReportRepository.save(report);
    }

    @Transactional
    public DailyReport updateCore(UUID reportId, UUID actingUserId, DailyReportCoreUpdateRequest request) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        report.updateCore(
                request.weatherCondition(),
                request.weatherBlockedTasks(),
                request.workHoursStart(),
                request.workHoursEnd(),
                request.comments(),
                Instant.now());
        return dailyReportRepository.save(report);
    }

    @Transactional
    public DailyReport submit(UUID reportId, UUID actingUserId) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        report.submit(Instant.now());
        return dailyReportRepository.save(report);
    }

    @Transactional
    public DailyReportWorkforceEntry addWorkforceEntry(UUID reportId, UUID actingUserId, WorkforceEntryRequest request) {
        DailyReport report = requireEditableReport(reportId, actingUserId);

        String roleDescription = request.roleDescription();
        if (request.membershipId() != null && (roleDescription == null || roleDescription.isBlank())) {
            roleDescription = membershipRepository
                    .findById(request.membershipId())
                    .map(this::describeFunction)
                    .orElse(roleDescription);
        }

        DailyReportWorkforceEntry entry = new DailyReportWorkforceEntry(
                UUID.randomUUID(), report.getId(), request.membershipId(), roleDescription, request.headcount(),
                Instant.now());
        return workforceEntryRepository.save(entry);
    }

    public List<DailyReportWorkforceEntry> listWorkforceEntries(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return workforceEntryRepository.findByDailyReportId(reportId);
    }

    @Transactional
    public DailyReportEquipmentUsage addEquipmentUsage(UUID reportId, UUID actingUserId, EquipmentUsageRequest request) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        DailyReportEquipmentUsage usage = new DailyReportEquipmentUsage(
                UUID.randomUUID(), report.getId(), request.equipmentId(), request.statusNote(), Instant.now());
        return equipmentUsageRepository.save(usage);
    }

    public List<DailyReportEquipmentUsage> listEquipmentUsage(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return equipmentUsageRepository.findByDailyReportId(reportId);
    }

    @Transactional
    public DailyReportActivity addActivity(UUID reportId, UUID actingUserId, ActivityRequest request) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        DailyReportActivity activity = new DailyReportActivity(
                UUID.randomUUID(), report.getId(), request.description(), request.progressNote(), request.status(),
                Instant.now());
        return activityRepository.save(activity);
    }

    public List<DailyReportActivity> listActivities(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return activityRepository.findByDailyReportId(reportId);
    }

    @Transactional
    public DailyReportOccurrence addOccurrence(UUID reportId, UUID actingUserId, OccurrenceRequest request) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        DailyReportOccurrence occurrence =
                new DailyReportOccurrence(UUID.randomUUID(), report.getId(), request.description(), Instant.now());
        return occurrenceRepository.save(occurrence);
    }

    public List<DailyReportOccurrence> listOccurrences(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return occurrenceRepository.findByDailyReportId(reportId);
    }

    @Transactional
    public DailyReportMaterialReceived addMaterialReceived(
            UUID reportId, UUID actingUserId, MaterialReceivedRequest request) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        DailyReportMaterialReceived received = new DailyReportMaterialReceived(
                UUID.randomUUID(), report.getId(), request.materialId(), request.quantity(), Instant.now());
        return materialReceivedRepository.save(received);
    }

    public List<DailyReportMaterialReceived> listMaterialsReceived(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return materialReceivedRepository.findByDailyReportId(reportId);
    }

    public List<DailyReport> list(UUID siteId, UUID actingUserId) {
        ConstructionSite site = requireSite(siteId);
        requireMembership(site.getProjectId(), actingUserId);
        return dailyReportRepository.findByConstructionSiteIdOrderByReportDateDesc(siteId);
    }

    public DailyReportDetailResponse getDetail(UUID reportId, UUID actingUserId) {
        DailyReport report = requireReport(reportId, actingUserId);
        return new DailyReportDetailResponse(
                DailyReportResponse.from(report),
                workforceEntryRepository.findByDailyReportId(reportId).stream()
                        .map(com.pantheon.service.dto.WorkforceEntryResponse::from)
                        .toList(),
                equipmentUsageRepository.findByDailyReportId(reportId).stream()
                        .map(com.pantheon.service.dto.EquipmentUsageResponse::from)
                        .toList(),
                activityRepository.findByDailyReportId(reportId).stream()
                        .map(com.pantheon.service.dto.ActivityResponse::from)
                        .toList(),
                occurrenceRepository.findByDailyReportId(reportId).stream()
                        .map(com.pantheon.service.dto.OccurrenceResponse::from)
                        .toList(),
                materialReceivedRepository.findByDailyReportId(reportId).stream()
                        .map(com.pantheon.service.dto.MaterialReceivedResponse::from)
                        .toList());
    }

    private String describeFunction(ProjectMembership membership) {
        if (membership.getFunction() == ConstructionFunction.SERVICE_PROVIDER && membership.getSpecialty() != null) {
            return membership.getSpecialty();
        }
        ConstructionFunction function = membership.getFunction() != null ? membership.getFunction() : ConstructionFunction.OTHER;
        return function.name();
    }

    private DailyReport requireReport(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        ConstructionSite site = requireSite(report.getConstructionSiteId());
        requireMembership(site.getProjectId(), actingUserId);
        return report;
    }

    private DailyReport requireEditableReport(UUID reportId, UUID actingUserId) {
        DailyReport report = requireReport(reportId, actingUserId);
        if (!report.isEditable()) {
            throw new DailyReportNotEditableException(reportId);
        }
        return report;
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireMembership(UUID projectId, UUID userId) {
        membershipRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotProjectMemberException(projectId));
    }
}
