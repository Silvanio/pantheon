package com.pantheon.service.service;

import com.pantheon.service.dto.PurchaseRequestComparisonResponse;
import com.pantheon.service.dto.PurchaseRequestItemCreationRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestApproval;
import com.pantheon.service.entity.PurchaseRequestApprovalStatus;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SitePurchaseRequestApprovalLevel;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NoPendingApprovalStepException;
import com.pantheon.service.exception.NotCurrentApprovalStepException;
import com.pantheon.service.exception.PurchaseRequestNotConferidoException;
import com.pantheon.service.exception.PurchaseRequestNotFoundException;
import com.pantheon.service.exception.PurchaseRequestNotOrcadoException;
import com.pantheon.service.exception.PurchaseRequestSelectionIncompleteException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.PurchaseRequestApprovalStepPendingEvent;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestApprovalRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.PurchaseRequestSpecifications;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "Pedido de Compra" header/document: creation, listing, the item-selection comparison view,
 * and — since this change — the whole multi-step approval/conclusion lifecycle that used to live
 * on {@code Orcamento} (see {@code purchase-requests} and {@code purchase-request-approval-workflow}).
 */
@Service
public class PurchaseRequestService {

    private static final DateTimeFormatter NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository itemRepository;
    private final PurchaseRequestApprovalRepository approvalRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final AppUserRepository userRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository orcamentoLineItemRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final SitePurchaseRequestApprovalLevelService approvalLevelService;
    private final OrcamentoService orcamentoService;
    private final MaterialService materialService;
    private final EventPublisher eventPublisher;

    public PurchaseRequestService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository itemRepository,
            PurchaseRequestApprovalRepository approvalRepository,
            ConstructionSiteRepository siteRepository,
            SiteMembershipRepository siteMembershipRepository,
            AppUserRepository userRepository,
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository orcamentoLineItemRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            SitePurchaseRequestApprovalLevelService approvalLevelService,
            OrcamentoService orcamentoService,
            MaterialService materialService,
            EventPublisher eventPublisher) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.itemRepository = itemRepository;
        this.approvalRepository = approvalRepository;
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.userRepository = userRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.orcamentoLineItemRepository = orcamentoLineItemRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.approvalLevelService = approvalLevelService;
        this.orcamentoService = orcamentoService;
        this.materialService = materialService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PurchaseRequest create(UUID siteId, UUID actingUserId, List<PurchaseRequestItemCreationRequest> items) {
        requireSite(siteId);
        requireManage(siteId, actingUserId);

        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long sequence = purchaseRequestRepository.countByConstructionSiteIdAndCreatedAtBetween(siteId, dayStart, dayEnd) + 1;
        String name = "Pedido " + NAME_DATE_FORMAT.format(today) + " #" + sequence;

        PurchaseRequest purchaseRequest =
                purchaseRequestRepository.save(new PurchaseRequest(UUID.randomUUID(), siteId, name, actingUserId, now));

        for (PurchaseRequestItemCreationRequest item : items) {
            itemRepository.save(new PurchaseRequestItem(
                    UUID.randomUUID(), siteId, purchaseRequest.getId(), item.name(), item.type(), item.quantity(),
                    item.unit(), actingUserId, now));
        }
        return purchaseRequest;
    }

    public Page<PurchaseRequest> list(
            UUID siteId, UUID actingUserId, LocalDate dateFilter, PurchaseRequestStatus statusFilter, Pageable pageable) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.PURCHASE_REQUEST);

        Instant dayStart = dateFilter != null ? dateFilter.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant dayEnd = dateFilter != null ? dateFilter.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Specification<PurchaseRequest> spec = PurchaseRequestSpecifications.siteId(siteId);
        Specification<PurchaseRequest> statusSpec = PurchaseRequestSpecifications.status(statusFilter);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }
        Specification<PurchaseRequest> dateSpec = PurchaseRequestSpecifications.createdOn(dayStart, dayEnd);
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return purchaseRequestRepository.findAll(spec, sorted);
    }

    public PurchaseRequest get(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
        return purchaseRequest;
    }

    /** Every Orcamento converted from this header — used to populate the response's linked-Orcamento summaries. */
    public List<Orcamento> listLinkedOrcamentos(UUID purchaseRequestId) {
        return orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId);
    }

    public List<PurchaseRequestApproval> listApprovals(UUID purchaseRequestId) {
        return approvalRepository.findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(purchaseRequestId);
    }

    @Transactional
    public PurchaseRequest submitForApproval(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.ORCADO) {
            throw new PurchaseRequestNotOrcadoException(purchaseRequestId);
        }

        List<PurchaseRequestItem> items = itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        boolean everyItemSelected = items.stream().allMatch(item -> item.getSelectedOrcamentoLineItemId() != null);
        if (!everyItemSelected) {
            throw new PurchaseRequestSelectionIncompleteException(purchaseRequestId);
        }

        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequestRepository.save(purchaseRequest);

        List<SitePurchaseRequestApprovalLevel> levels =
                approvalLevelService.getEffectiveLevels(purchaseRequest.getConstructionSiteId());
        Instant now = Instant.now();
        PurchaseRequestApproval firstStep = null;
        for (SitePurchaseRequestApprovalLevel level : levels) {
            PurchaseRequestApproval step = approvalRepository.save(new PurchaseRequestApproval(
                    UUID.randomUUID(), purchaseRequestId, purchaseRequest.getCurrentApprovalCycle(),
                    level.getStepOrder(), level.getApproverFunction(), now));
            if (firstStep == null || step.getStepOrder() < firstStep.getStepOrder()) {
                firstStep = step;
            }
        }
        if (firstStep != null) {
            notifyStepPending(purchaseRequest, firstStep);
        }
        return purchaseRequest;
    }

    @Transactional
    public PurchaseRequest approveStep(UUID purchaseRequestId, UUID actingUserId, String comment) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        PurchaseRequestApproval step = requirePendingStep(purchaseRequest);
        var access = requireStepAuthority(purchaseRequest.getConstructionSiteId(), actingUserId, step);

        UUID decidedBy = access.companyStaff() ? null : access.siteMembership().getId();
        step.approve(decidedBy, comment, Instant.now());
        approvalRepository.save(step);

        var nextStep = approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequestId, purchaseRequest.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING);
        if (nextStep.isPresent()) {
            notifyStepPending(purchaseRequest, nextStep.get());
        } else {
            purchaseRequest.approve(Instant.now());
            purchaseRequestRepository.save(purchaseRequest);
            orcamentoService.lockAllForPurchaseRequest(purchaseRequestId);
        }
        return purchaseRequest;
    }

    @Transactional
    public PurchaseRequest rejectStep(UUID purchaseRequestId, UUID actingUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        PurchaseRequestApproval step = requirePendingStep(purchaseRequest);
        var access = requireStepAuthority(purchaseRequest.getConstructionSiteId(), actingUserId, step);

        UUID decidedBy = access.companyStaff() ? null : access.siteMembership().getId();
        step.reject(decidedBy, reason, Instant.now());
        approvalRepository.save(step);

        purchaseRequest.returnToOrcadoAfterRejection(reason);
        purchaseRequestRepository.save(purchaseRequest);
        orcamentoService.unlockAllForPurchaseRequest(purchaseRequestId);
        return purchaseRequest;
    }

    @Transactional
    public PurchaseRequest conclude(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.CONFERIDO) {
            throw new PurchaseRequestNotConferidoException(purchaseRequestId);
        }

        List<PurchaseRequestItem> items = itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        List<UUID> selectedLineItemIds =
                items.stream().map(PurchaseRequestItem::getSelectedOrcamentoLineItemId).filter(Objects::nonNull).toList();
        List<OrcamentoLineItem> selectedLineItems = orcamentoLineItemRepository.findAllById(selectedLineItemIds);

        purchaseRequest.complete(Instant.now());
        purchaseRequestRepository.save(purchaseRequest);

        materialService.createFromPurchaseRequestSelections(purchaseRequest, selectedLineItems);
        return purchaseRequest;
    }

    /** Read-only, derived comparison grid: rows are the header's items, columns are its linked Orcamentos. */
    public PurchaseRequestComparisonResponse getComparison(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);

        List<PurchaseRequestItem> items = itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        List<Orcamento> orcamentos = orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId);
        List<UUID> orcamentoIds = orcamentos.stream().map(Orcamento::getId).toList();

        List<PurchaseRequestComparisonResponse.ColumnResponse> columns = orcamentos.stream()
                .map(o -> new PurchaseRequestComparisonResponse.ColumnResponse(o.getId(), o.getFornecedorNome()))
                .toList();

        List<PurchaseRequestComparisonResponse.RowResponse> rows = new ArrayList<>();
        for (PurchaseRequestItem item : items) {
            List<OrcamentoLineItem> matches = orcamentoIds.isEmpty()
                    ? List.of()
                    : orcamentoLineItemRepository.findByOrcamentoIdInAndSourcePurchaseRequestItemId(orcamentoIds, item.getId());
            List<PurchaseRequestComparisonResponse.CellResponse> cells = matches.stream()
                    .map(lineItem -> new PurchaseRequestComparisonResponse.CellResponse(
                            lineItem.getOrcamentoId(), lineItem.getId(), lineItem.getUnitPrice(), lineItem.getQuantity(),
                            lineItem.getId().equals(item.getSelectedOrcamentoLineItemId())))
                    .toList();
            rows.add(new PurchaseRequestComparisonResponse.RowResponse(
                    item.getId(), item.getName(), item.getQuantity(), item.getUnit(), cells));
        }

        return new PurchaseRequestComparisonResponse(columns, rows);
    }

    private PurchaseRequestApproval requirePendingStep(PurchaseRequest purchaseRequest) {
        return approvalRepository
                .findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                        purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING)
                .orElseThrow(() -> new NoPendingApprovalStepException(purchaseRequest.getId()));
    }

    private SiteAccessContext requireStepAuthority(UUID siteId, UUID userId, PurchaseRequestApproval step) {
        SiteAccessContext access = siteAccessService.requireAccess(siteId, userId);
        if (access.companyStaff() || access.function() == step.getApproverFunction()) {
            return access;
        }
        throw new NotCurrentApprovalStepException(step.getPurchaseRequestId());
    }

    private void notifyStepPending(PurchaseRequest purchaseRequest, PurchaseRequestApproval step) {
        UUID siteId = purchaseRequest.getConstructionSiteId();
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        List<SiteMembership> approvers = siteMembershipRepository
                .findByConstructionSiteIdAndFunction(siteId, step.getApproverFunction())
                .stream()
                .filter(m -> m.isActive() && m.getUserId() != null)
                .toList();

        for (SiteMembership approver : approvers) {
            userRepository.findById(approver.getUserId()).map(AppUser::getEmail).ifPresent(email ->
                    eventPublisher.publish(PurchaseRequestApprovalStepPendingEvent.TYPE, new PurchaseRequestApprovalStepPendingEvent(
                            purchaseRequest.getId(), siteId, site.getName(), step.getApproverFunction().name(), email)));
        }
    }

    private PurchaseRequest requirePurchaseRequest(UUID purchaseRequestId) {
        return purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new PurchaseRequestNotFoundException(purchaseRequestId));
    }

    private void requireSite(UUID siteId) {
        siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.PURCHASE_REQUEST);
    }
}
