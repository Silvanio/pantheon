package com.pantheon.service.service;

import com.pantheon.service.dto.ActivityRequest;
import com.pantheon.service.dto.DailyReportCoreUpdateRequest;
import com.pantheon.service.dto.DailyReportDetailResponse;
import com.pantheon.service.dto.DailyReportResponse;
import com.pantheon.service.dto.EquipmentUsageRequest;
import com.pantheon.service.dto.MaterialReceivedRequest;
import com.pantheon.service.dto.OccurrenceRequest;
import com.pantheon.service.dto.WorkforceEntryRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMaterialReceived;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportNotDeletableException;
import com.pantheon.service.exception.DailyReportNotEditableException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.DuplicateDailyReportException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportEquipmentUsageRepository;
import com.pantheon.service.repository.DailyReportMaterialReceivedRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportOccurrenceRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.storage.StorageService;
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
    private final DailyReportMediaRepository mediaRepository;
    private final DailyReportAttachmentRepository attachmentRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final StorageService storageService;

    public DailyReportService(
            DailyReportRepository dailyReportRepository,
            DailyReportWorkforceEntryRepository workforceEntryRepository,
            DailyReportEquipmentUsageRepository equipmentUsageRepository,
            DailyReportActivityRepository activityRepository,
            DailyReportOccurrenceRepository occurrenceRepository,
            DailyReportMaterialReceivedRepository materialReceivedRepository,
            DailyReportMediaRepository mediaRepository,
            DailyReportAttachmentRepository attachmentRepository,
            ConstructionSiteRepository siteRepository,
            SiteMembershipRepository siteMembershipRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            StorageService storageService) {
        this.dailyReportRepository = dailyReportRepository;
        this.workforceEntryRepository = workforceEntryRepository;
        this.equipmentUsageRepository = equipmentUsageRepository;
        this.activityRepository = activityRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.materialReceivedRepository = materialReceivedRepository;
        this.mediaRepository = mediaRepository;
        this.attachmentRepository = attachmentRepository;
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.storageService = storageService;
    }

    @Transactional
    public DailyReport create(UUID siteId, UUID actingUserId, LocalDate reportDate) {
        requireSite(siteId);
        requireManage(siteId, actingUserId);

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
            roleDescription = siteMembershipRepository
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
                UUID.randomUUID(), report.getId(), request.materialName(), request.unit(), request.quantity(),
                Instant.now());
        return materialReceivedRepository.save(received);
    }

    public List<DailyReportMaterialReceived> listMaterialsReceived(UUID reportId, UUID actingUserId) {
        requireReport(reportId, actingUserId);
        return materialReceivedRepository.findByDailyReportId(reportId);
    }

    /** Only while still DRAFT — a submitted report is part of the site's record and signatures may depend on it. */
    @Transactional
    public void delete(UUID reportId, UUID actingUserId) {
        DailyReport report = requireReport(reportId, actingUserId);
        requireManage(report.getConstructionSiteId(), actingUserId);
        if (!report.isEditable()) {
            throw new DailyReportNotDeletableException(reportId);
        }

        List<DailyReportMedia> media = mediaRepository.findByDailyReportIdOrderByCreatedAtAsc(reportId);
        for (DailyReportMedia item : media) {
            storageService.deleteObject(item.getStorageKey());
        }
        mediaRepository.deleteAll(media);

        List<DailyReportAttachment> attachments = attachmentRepository.findByDailyReportIdOrderByCreatedAtAsc(reportId);
        for (DailyReportAttachment attachment : attachments) {
            storageService.deleteObject(attachment.getStorageKey());
        }
        attachmentRepository.deleteAll(attachments);

        workforceEntryRepository.deleteAll(workforceEntryRepository.findByDailyReportId(reportId));
        equipmentUsageRepository.deleteAll(equipmentUsageRepository.findByDailyReportId(reportId));
        activityRepository.deleteAll(activityRepository.findByDailyReportId(reportId));
        occurrenceRepository.deleteAll(occurrenceRepository.findByDailyReportId(reportId));
        materialReceivedRepository.deleteAll(materialReceivedRepository.findByDailyReportId(reportId));

        dailyReportRepository.delete(report);
    }

    public List<DailyReport> list(UUID siteId, UUID actingUserId) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.DAILY_REPORT);
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

    private String describeFunction(SiteMembership membership) {
        if (membership.getServiceProviderTrade() != null) {
            return membership.getServiceProviderTrade();
        }
        return membership.getFunction().name();
    }

    private DailyReport requireReport(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        var access = siteAccessService.requireAccess(report.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(report.getConstructionSiteId(), access, PermissionCapability.DAILY_REPORT);
        return report;
    }

    private DailyReport requireEditableReport(UUID reportId, UUID actingUserId) {
        DailyReport report = requireReport(reportId, actingUserId);
        requireManage(report.getConstructionSiteId(), actingUserId);
        if (!report.isEditable()) {
            throw new DailyReportNotEditableException(reportId);
        }
        return report;
    }

    private void requireManage(UUID siteId, UUID actingUserId) {
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireManage(siteId, access, PermissionCapability.DAILY_REPORT);
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }
}
