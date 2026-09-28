package com.pantheon.service.service;

import com.pantheon.service.dto.ActivityRequest;
import com.pantheon.service.dto.DailyReportCoreUpdateRequest;
import com.pantheon.service.dto.DailyReportDetailResponse;
import com.pantheon.service.dto.DailyReportResponse;
import com.pantheon.service.dto.EquipmentUsageRequest;
import com.pantheon.service.dto.MaterialReceivedRequest;
import com.pantheon.service.dto.OccurrenceRequest;
import com.pantheon.service.dto.WorkforceEntryRequest;
import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportApproval;
import com.pantheon.service.entity.DailyReportApprovalStatus;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMaterialReceived;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportStatus;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteDailyReportApprovalLevel;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportCoreFieldsRequiredException;
import com.pantheon.service.exception.DailyReportNotDeletableException;
import com.pantheon.service.exception.DailyReportNotEditableException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.DuplicateDailyReportException;
import com.pantheon.service.exception.NoPendingDailyReportApprovalStepException;
import com.pantheon.service.exception.NotCurrentDailyReportApprovalStepException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportApprovalRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final DailyReportApprovalRepository approvalRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final SiteDailyReportApprovalLevelService approvalLevelService;
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
            DailyReportApprovalRepository approvalRepository,
            ConstructionSiteRepository siteRepository,
            SiteMembershipRepository siteMembershipRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            SiteDailyReportApprovalLevelService approvalLevelService,
            StorageService storageService) {
        this.dailyReportRepository = dailyReportRepository;
        this.workforceEntryRepository = workforceEntryRepository;
        this.equipmentUsageRepository = equipmentUsageRepository;
        this.activityRepository = activityRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.materialReceivedRepository = materialReceivedRepository;
        this.mediaRepository = mediaRepository;
        this.attachmentRepository = attachmentRepository;
        this.approvalRepository = approvalRepository;
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.approvalLevelService = approvalLevelService;
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
        if (report.getWeatherCondition() == null || report.getWorkHoursStart() == null || report.getWorkHoursEnd() == null) {
            throw new DailyReportCoreFieldsRequiredException(reportId);
        }
        return dailyReportRepository.save(report);
    }

    /**
     * Submits a draft report for approval: transitions {@code DRAFT} -> {@code PENDING_APPROVAL}
     * and creates one {@link DailyReportApproval} step per the site's effective
     * {@link SiteDailyReportApprovalLevel}s for a new approval cycle — mirrors
     * {@code PurchaseRequestService#submitForApproval}'s step-creation loop. No additional gate
     * beyond the report being {@code DRAFT} with its core fields filled (already enforced by
     * {@link #updateCore}) — Diário de Obra has no per-line selection gate equivalent to Pedido
     * de Compra's.
     */
    @Transactional
    public DailyReport submit(UUID reportId, UUID actingUserId) {
        DailyReport report = requireEditableReport(reportId, actingUserId);
        report.submit(Instant.now());
        dailyReportRepository.save(report);

        List<SiteDailyReportApprovalLevel> levels = approvalLevelService.getEffectiveLevels(report.getConstructionSiteId());
        Instant now = Instant.now();
        for (SiteDailyReportApprovalLevel level : levels) {
            approvalRepository.save(new DailyReportApproval(
                    UUID.randomUUID(), reportId, report.getCurrentApprovalCycle(), level.getStepOrder(),
                    level.getApproverFunction(), now));
        }
        return report;
    }

    public List<DailyReportApproval> listApprovals(UUID reportId) {
        return approvalRepository.findByDailyReportIdOrderByCycleNumberAscStepOrderAsc(reportId);
    }

    /**
     * Approves the current cycle's lowest-order {@code PENDING} step. Approving the last
     * remaining step transitions the report to {@code APPROVED}; otherwise the next step becomes
     * actionable. Mirrors {@code PurchaseRequestService#approveStep} exactly for the
     * authorization mechanism (see {@link #requireStepAuthority}).
     */
    @Transactional
    public DailyReport approveStep(UUID reportId, UUID actingUserId, String comment) {
        DailyReport report = requireExistingReport(reportId);
        DailyReportApproval step = requirePendingStep(report);
        SiteAccessContext access = requireStepAuthority(report.getConstructionSiteId(), actingUserId, step);

        UUID decidedBy = access.siteMembership() != null ? access.siteMembership().getId() : null;
        step.approve(decidedBy, comment, Instant.now());
        approvalRepository.save(step);

        var nextStep = approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                reportId, report.getCurrentApprovalCycle(), DailyReportApprovalStatus.PENDING);
        if (nextStep.isEmpty()) {
            report.approve(Instant.now());
            dailyReportRepository.save(report);
        }
        return report;
    }

    /**
     * Rejects the current cycle's lowest-order {@code PENDING} step, requiring a reason. Returns
     * the report to {@code DRAFT} for revision; a later resubmission starts a new cycle, this
     * cycle's steps kept for history. Mirrors {@code PurchaseRequestService#rejectStep} exactly
     * for the authorization mechanism (see {@link #requireStepAuthority}).
     */
    @Transactional
    public DailyReport rejectStep(UUID reportId, UUID actingUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        DailyReport report = requireExistingReport(reportId);
        DailyReportApproval step = requirePendingStep(report);
        SiteAccessContext access = requireStepAuthority(report.getConstructionSiteId(), actingUserId, step);

        UUID decidedBy = access.siteMembership() != null ? access.siteMembership().getId() : null;
        step.reject(decidedBy, reason, Instant.now());
        approvalRepository.save(step);

        report.reject(Instant.now());
        dailyReportRepository.save(report);
        return report;
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
        // A DRAFT report may still carry approval history from an earlier rejected cycle (old
        // cycles are kept for history, not cleared on rejection) — must be cleared before the
        // report row itself, since daily_report_approval.daily_report_id FKs it.
        approvalRepository.deleteAll(approvalRepository.findByDailyReportIdOrderByCycleNumberAscStepOrderAsc(reportId));

        dailyReportRepository.delete(report);
    }

    /**
     * A member whose resolved {@code DAILY_REPORT} access is plain {@code VIEW} only ever sees a
     * report once it is {@code APPROVED}; {@code MANAGE} and {@code VIEW_AND_APPROVE} see every
     * status — see {@code daily-construction-report}'s "Daily report listing and detail" and this
     * change's design.md Decision 3. Pushed into the query (a status-filtered finder) rather than
     * filtered in memory afterwards, so pagination totals stay correct and no extra per-row work
     * is introduced.
     */
    public Page<DailyReport> list(UUID siteId, UUID actingUserId, Pageable pageable) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.DAILY_REPORT);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (permissionService.resolve(siteId, access, PermissionCapability.DAILY_REPORT) == AccessLevel.VIEW) {
            return dailyReportRepository.findByConstructionSiteIdAndStatus(siteId, DailyReportStatus.APPROVED, sorted);
        }
        return dailyReportRepository.findByConstructionSiteId(siteId, sorted);
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
                        .toList(),
                listApprovals(reportId).stream()
                        .map(com.pantheon.service.dto.DailyReportApprovalResponse::from)
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
        requireApprovedForViewOnly(report, access);
        return report;
    }

    /**
     * No-op unless the resolved access is plain {@code VIEW}, in which case a
     * non-{@code APPROVED} report behaves as if it doesn't exist — same "behaves as if it doesn't
     * exist" 404 pattern as {@code PurchaseRequestService#requireViewAndApproveVisibility}, just
     * gating a different access level on a different rule (see design.md Decision 3).
     */
    private void requireApprovedForViewOnly(DailyReport report, SiteAccessContext access) {
        UUID siteId = report.getConstructionSiteId();
        if (permissionService.resolve(siteId, access, PermissionCapability.DAILY_REPORT) != AccessLevel.VIEW) {
            return;
        }
        if (report.getStatus() != DailyReportStatus.APPROVED) {
            throw new DailyReportNotFoundException(report.getId());
        }
    }

    private DailyReport requireEditableReport(UUID reportId, UUID actingUserId) {
        DailyReport report = requireReport(reportId, actingUserId);
        requireManage(report.getConstructionSiteId(), actingUserId);
        if (!report.isEditable()) {
            throw new DailyReportNotEditableException(reportId);
        }
        return report;
    }

    /** Looks up a report by id with no visibility/permission gate — used by {@link #approveStep}/{@link #rejectStep}, whose own {@link #requireStepAuthority} check is the authorization. */
    private DailyReport requireExistingReport(UUID reportId) {
        return dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
    }

    private DailyReportApproval requirePendingStep(DailyReport report) {
        return approvalRepository
                .findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                        report.getId(), report.getCurrentApprovalCycle(), DailyReportApprovalStatus.PENDING)
                .orElseThrow(() -> new NoPendingDailyReportApprovalStepException(report.getId()));
    }

    /**
     * A user with an active {@link SiteMembership} on this site — company staff or not — must have
     * a function matching {@code step}'s, with no exception, and a {@code DAILY_REPORT} access
     * level of {@code MANAGE} or {@code VIEW_AND_APPROVE}. Only a user with no {@link SiteMembership}
     * at all on this site falls back to the unrestricted company-staff bypass. Copied faithfully
     * from {@code PurchaseRequestService#requireStepAuthority} — same nuance, same edge cases.
     */
    private SiteAccessContext requireStepAuthority(UUID siteId, UUID userId, DailyReportApproval step) {
        SiteAccessContext access = siteAccessService.requireAccess(siteId, userId);
        SiteMembership membership = resolveSiteMembership(siteId, userId, access);

        if (membership != null) {
            boolean authorized = membership.getFunction() == step.getApproverFunction()
                    && permissionService.canApprove(siteId, access, PermissionCapability.DAILY_REPORT);
            if (!authorized) {
                throw new NotCurrentDailyReportApprovalStepException(step.getDailyReportId());
            }
            return new SiteAccessContext(access.companyStaff(), membership);
        }

        if (access.companyStaff()) {
            return access;
        }
        throw new NotCurrentDailyReportApprovalStepException(step.getDailyReportId());
    }

    /** {@code access.siteMembership()} whenever present, else a direct lookup — needed because {@link SiteAccessContext} never populates a membership for company staff, even when one exists. */
    private SiteMembership resolveSiteMembership(UUID siteId, UUID userId, SiteAccessContext access) {
        if (access.siteMembership() != null) {
            return access.siteMembership();
        }
        return siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, userId)
                .filter(SiteMembership::isActive)
                .orElse(null);
    }

    private void requireManage(UUID siteId, UUID actingUserId) {
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireManage(siteId, access, PermissionCapability.DAILY_REPORT);
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }
}
