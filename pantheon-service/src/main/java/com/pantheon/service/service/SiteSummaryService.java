package com.pantheon.service.service;

import com.pantheon.service.dto.CountStatResponse;
import com.pantheon.service.dto.DailyReportSummaryResponse;
import com.pantheon.service.dto.EquipmentSummaryResponse;
import com.pantheon.service.dto.OrcamentoSummaryResponse;
import com.pantheon.service.dto.PurchaseRequestSummaryResponse;
import com.pantheon.service.dto.RecentItemResponse;
import com.pantheon.service.dto.SiteSummaryResponse;
import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.EquipmentRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteDocumentProjectRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskCardRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Aggregates the obra summary shown first on entering an obra. Every field of
 * {@link SiteSummaryResponse} is {@code null} when the caller's resolved access to the matching
 * capability is {@code HIDDEN} — each section reuses its own capability's existing read path
 * (throwing {@link ForbiddenCapabilityException} on {@code HIDDEN}), caught here and mapped to
 * "omit this section" rather than letting the whole request fail. Daily reports/Purchase
 * requests/Orçamentos/Equipment reuse their already-paginated, createdAt-sorted service {@code
 * list} methods; Schedule/Tasks/Projetos/Team have no comparable lightweight read path, so this
 * service does its own visibility check for those four and queries their repositories directly.
 * {@link #build} resolves the caller's {@link SiteAccessContext} and every capability's {@link
 * AccessLevel} (via {@link SitePermissionService#resolveAll}) exactly once and hands the
 * resulting map down to those four sections, instead of each of them independently re-resolving
 * site access and re-querying {@code site_permission_override} for its own capability.
 */
@Service
public class SiteSummaryService {

    private final ScheduleService scheduleService;
    private final DailyReportService dailyReportService;
    private final PurchaseRequestService purchaseRequestService;
    private final OrcamentoService orcamentoService;
    private final EquipmentService equipmentService;
    private final EquipmentRepository equipmentRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final TaskCardRepository taskCardRepository;
    private final SiteDocumentProjectRepository siteDocumentProjectRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public SiteSummaryService(
            ScheduleService scheduleService,
            DailyReportService dailyReportService,
            PurchaseRequestService purchaseRequestService,
            OrcamentoService orcamentoService,
            EquipmentService equipmentService,
            EquipmentRepository equipmentRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            OrcamentoRepository orcamentoRepository,
            TaskCardRepository taskCardRepository,
            SiteDocumentProjectRepository siteDocumentProjectRepository,
            SiteMembershipRepository siteMembershipRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.scheduleService = scheduleService;
        this.dailyReportService = dailyReportService;
        this.purchaseRequestService = purchaseRequestService;
        this.orcamentoService = orcamentoService;
        this.equipmentService = equipmentService;
        this.equipmentRepository = equipmentRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.taskCardRepository = taskCardRepository;
        this.siteDocumentProjectRepository = siteDocumentProjectRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public SiteSummaryResponse build(UUID siteId, UUID actingUserId) {
        requireSite(siteId);
        SiteAccessContext access = siteAccessService.requireAccess(siteId, actingUserId);
        Map<PermissionCapability, AccessLevel> permissions = permissionService.resolveAll(siteId, access);
        return new SiteSummaryResponse(
                scheduleSummary(siteId, permissions),
                dailyReportsSummary(siteId, actingUserId),
                purchaseRequestsSummary(siteId, actingUserId),
                orcamentosSummary(siteId, actingUserId),
                equipmentSummary(siteId, actingUserId),
                projectsSummary(siteId, permissions),
                tasksSummary(siteId, permissions),
                teamSummary(siteId, permissions));
    }

    private Integer scheduleSummary(UUID siteId, Map<PermissionCapability, AccessLevel> permissions) {
        if (isHidden(permissions, PermissionCapability.SCHEDULE)) {
            return null;
        }
        return scheduleService.computeProgress(siteId);
    }

    private DailyReportSummaryResponse dailyReportsSummary(UUID siteId, UUID actingUserId) {
        try {
            Page<DailyReport> page = dailyReportService.list(siteId, actingUserId, PageRequest.of(0, 1));
            var lastReportDate = page.getContent().isEmpty() ? null : page.getContent().get(0).getReportDate();
            return new DailyReportSummaryResponse(page.getTotalElements(), lastReportDate);
        } catch (ForbiddenCapabilityException e) {
            return null;
        }
    }

    private PurchaseRequestSummaryResponse purchaseRequestsSummary(UUID siteId, UUID actingUserId) {
        try {
            Page<PurchaseRequest> page = purchaseRequestService.list(siteId, actingUserId, null, null, PageRequest.of(0, 3));
            List<RecentItemResponse> recent = page.getContent().stream()
                    .map(pr -> new RecentItemResponse(pr.getId(), pr.getName(), pr.getCreatedAt()))
                    .toList();
            long awaitingApproval = purchaseRequestRepository.countByConstructionSiteIdAndStatusAndSubmittedAtIsNotNull(
                    siteId, PurchaseRequestStatus.ORCADO);
            return new PurchaseRequestSummaryResponse(page.getTotalElements(), awaitingApproval, recent);
        } catch (ForbiddenCapabilityException e) {
            return null;
        }
    }

    private OrcamentoSummaryResponse orcamentosSummary(UUID siteId, UUID actingUserId) {
        try {
            Page<Orcamento> page = orcamentoService.list(siteId, actingUserId, null, null, null, PageRequest.of(0, 3));
            List<RecentItemResponse> recent = page.getContent().stream()
                    .map(o -> new RecentItemResponse(o.getId(), o.getFornecedorNome(), o.getCreatedAt()))
                    .toList();
            long draft = orcamentoRepository.countByConstructionSiteIdAndStatus(siteId, OrcamentoStatus.DRAFT);
            return new OrcamentoSummaryResponse(page.getTotalElements(), draft, recent);
        } catch (ForbiddenCapabilityException e) {
            return null;
        }
    }

    private EquipmentSummaryResponse equipmentSummary(UUID siteId, UUID actingUserId) {
        try {
            Page<Equipment> page = equipmentService.list(siteId, actingUserId, PageRequest.of(0, 1));
            long unavailable = equipmentRepository.countByConstructionSiteIdAndStatus(siteId, EquipmentStatus.UNAVAILABLE);
            return new EquipmentSummaryResponse(page.getTotalElements(), unavailable);
        } catch (ForbiddenCapabilityException e) {
            return null;
        }
    }

    private CountStatResponse projectsSummary(UUID siteId, Map<PermissionCapability, AccessLevel> permissions) {
        if (isHidden(permissions, PermissionCapability.DOCUMENT_PROJECTS)) {
            return null;
        }
        long total = siteDocumentProjectRepository.countByConstructionSiteId(siteId);
        List<RecentItemResponse> recent = siteDocumentProjectRepository
                .findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)
                .stream()
                .limit(3)
                .map(p -> new RecentItemResponse(p.getId(), p.getName(), p.getCreatedAt()))
                .toList();
        return new CountStatResponse(total, recent);
    }

    private CountStatResponse tasksSummary(UUID siteId, Map<PermissionCapability, AccessLevel> permissions) {
        if (isHidden(permissions, PermissionCapability.TASKS)) {
            return null;
        }
        long total = taskCardRepository.countByConstructionSiteId(siteId);
        List<RecentItemResponse> recent = taskCardRepository
                .findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)
                .stream()
                .limit(3)
                .map(c -> new RecentItemResponse(c.getId(), c.getTitle(), c.getCreatedAt()))
                .toList();
        return new CountStatResponse(total, recent);
    }

    private Long teamSummary(UUID siteId, Map<PermissionCapability, AccessLevel> permissions) {
        if (isHidden(permissions, PermissionCapability.TEAM_MANAGE)) {
            return null;
        }
        return siteMembershipRepository.countByConstructionSiteId(siteId);
    }

    /** Whether {@code capability}'s already-resolved access (from {@link #build}'s single {@code resolveAll} call) is {@code HIDDEN}. */
    private boolean isHidden(Map<PermissionCapability, AccessLevel> permissions, PermissionCapability capability) {
        return permissions.get(capability) == AccessLevel.HIDDEN;
    }

    private void requireSite(UUID siteId) {
        siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }
}
