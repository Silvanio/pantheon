package com.pantheon.service.service;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.dto.OrcamentoLineItemRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoApproval;
import com.pantheon.service.entity.OrcamentoApprovalStatus;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SiteOrcamentoApprovalLevel;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.NoPendingApprovalStepException;
import com.pantheon.service.exception.NotCurrentApprovalStepException;
import com.pantheon.service.exception.OrcamentoEmptyException;
import com.pantheon.service.exception.OrcamentoNotApprovedException;
import com.pantheon.service.exception.OrcamentoNotDraftException;
import com.pantheon.service.exception.OrcamentoNotFoundException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.OrcamentoApprovalStepPendingEvent;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.OrcamentoApprovalRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A construction site's budget/quote: free-text line items, a configurable sequential approval
 * chain, and the Rascunho/Em aprovação/Aprovado/Concluído lifecycle. See
 * {@code orcamento-approval-workflow}.
 */
@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final OrcamentoApprovalRepository approvalRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final AppUserRepository userRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final SiteOrcamentoApprovalLevelService approvalLevelService;
    private final MaterialService materialService;
    private final FornecedorService fornecedorService;
    private final EventPublisher eventPublisher;

    public OrcamentoService(
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            OrcamentoApprovalRepository approvalRepository,
            ConstructionSiteRepository siteRepository,
            SiteMembershipRepository siteMembershipRepository,
            AppUserRepository userRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            SiteOrcamentoApprovalLevelService approvalLevelService,
            MaterialService materialService,
            FornecedorService fornecedorService,
            EventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.lineItemRepository = lineItemRepository;
        this.approvalRepository = approvalRepository;
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.userRepository = userRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.approvalLevelService = approvalLevelService;
        this.materialService = materialService;
        this.fornecedorService = fornecedorService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Orcamento create(
            UUID siteId, UUID actingUserId, List<OrcamentoLineItemRequest> items, FornecedorRequest fornecedorRequest) {
        ConstructionSite site = requireSite(siteId);
        requireManage(siteId, actingUserId);

        Orcamento orcamento = createDraft(site, actingUserId, fornecedorRequest, null);
        for (OrcamentoLineItemRequest item : items) {
            addLineItemInternal(orcamento.getId(), item.name(), item.type(), item.quantity(), item.unitPrice(), null);
        }
        return orcamento;
    }

    /** Called only from {@link PurchaseRequestItemService#convertToOrcamento} — already authorized there. */
    @Transactional
    Orcamento createFromPurchaseRequestItems(
            UUID siteId, UUID actingUserId, List<PurchaseRequestItem> items, UUID purchaseRequestId,
            FornecedorRequest fornecedorRequest) {
        ConstructionSite site = requireSite(siteId);

        Orcamento orcamento = createDraft(site, actingUserId, fornecedorRequest, purchaseRequestId);
        for (PurchaseRequestItem item : items) {
            addLineItemInternal(orcamento.getId(), item.getName(), item.getType(), item.getQuantity(), null, item.getId());
        }
        return orcamento;
    }

    private Orcamento createDraft(
            ConstructionSite site, UUID actingUserId, FornecedorRequest fornecedorRequest, UUID sourcePurchaseRequestId) {
        Fornecedor fornecedor = fornecedorService.findOrCreate(site.getCompanyId(), actingUserId, fornecedorRequest);
        Orcamento orcamento = new Orcamento(
                UUID.randomUUID(), site.getId(), actingUserId, Instant.now(), fornecedor.getCnpj(),
                fornecedor.getName(), fornecedor.getAddress(), fornecedor.getContactName(),
                fornecedor.getContactPhone(), fornecedor.getId(), sourcePurchaseRequestId);
        return orcamentoRepository.save(orcamento);
    }

    @Transactional
    public OrcamentoLineItem addLineItem(UUID orcamentoId, UUID actingUserId, OrcamentoLineItemRequest request) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        requireManage(orcamento.getConstructionSiteId(), actingUserId);
        requireDraft(orcamento);
        return addLineItemInternal(orcamentoId, request.name(), request.type(), request.quantity(), request.unitPrice(), null);
    }

    @Transactional
    public OrcamentoLineItem updateLineItem(
            UUID orcamentoId, UUID lineItemId, UUID actingUserId, OrcamentoLineItemRequest request) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        requireManage(orcamento.getConstructionSiteId(), actingUserId);
        requireDraft(orcamento);

        OrcamentoLineItem item = requireLineItem(orcamentoId, lineItemId);
        item.update(request.name(), request.type(), request.quantity(), request.unitPrice());
        return lineItemRepository.save(item);
    }

    @Transactional
    public void removeLineItem(UUID orcamentoId, UUID lineItemId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        requireManage(orcamento.getConstructionSiteId(), actingUserId);
        requireDraft(orcamento);

        OrcamentoLineItem item = requireLineItem(orcamentoId, lineItemId);
        lineItemRepository.delete(item);
    }

    @Transactional
    public Orcamento submitForApproval(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        requireManage(orcamento.getConstructionSiteId(), actingUserId);
        requireDraft(orcamento);

        List<OrcamentoLineItem> items = lineItemRepository.findByOrcamentoId(orcamentoId);
        if (items.isEmpty()) {
            throw new OrcamentoEmptyException(orcamentoId);
        }

        orcamento.submitForApproval(Instant.now());
        orcamentoRepository.save(orcamento);

        List<SiteOrcamentoApprovalLevel> levels = approvalLevelService.getEffectiveLevels(orcamento.getConstructionSiteId());
        Instant now = Instant.now();
        OrcamentoApproval firstStep = null;
        for (SiteOrcamentoApprovalLevel level : levels) {
            OrcamentoApproval step = approvalRepository.save(new OrcamentoApproval(
                    UUID.randomUUID(), orcamentoId, orcamento.getCurrentApprovalCycle(), level.getStepOrder(),
                    level.getApproverFunction(), now));
            if (firstStep == null || step.getStepOrder() < firstStep.getStepOrder()) {
                firstStep = step;
            }
        }
        if (firstStep != null) {
            notifyStepPending(orcamento, firstStep);
        }
        return orcamento;
    }

    @Transactional
    public Orcamento approveStep(UUID orcamentoId, UUID actingUserId, String comment) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        OrcamentoApproval step = requirePendingStep(orcamento);
        var access = requireStepAuthority(orcamento.getConstructionSiteId(), actingUserId, step);

        UUID decidedBy = access.companyStaff() ? null : access.siteMembership().getId();
        step.approve(decidedBy, comment, Instant.now());
        approvalRepository.save(step);

        var nextStep = approvalRepository.findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                orcamentoId, orcamento.getCurrentApprovalCycle(), OrcamentoApprovalStatus.PENDING);
        if (nextStep.isPresent()) {
            notifyStepPending(orcamento, nextStep.get());
        } else {
            orcamento.approve(Instant.now());
            orcamentoRepository.save(orcamento);
        }
        return orcamento;
    }

    @Transactional
    public Orcamento rejectStep(UUID orcamentoId, UUID actingUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        Orcamento orcamento = requireOrcamento(orcamentoId);
        OrcamentoApproval step = requirePendingStep(orcamento);
        var access = requireStepAuthority(orcamento.getConstructionSiteId(), actingUserId, step);

        UUID decidedBy = access.companyStaff() ? null : access.siteMembership().getId();
        step.reject(decidedBy, reason, Instant.now());
        approvalRepository.save(step);

        orcamento.returnToDraftAfterRejection(reason);
        return orcamentoRepository.save(orcamento);
    }

    @Transactional
    public Orcamento conclude(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        requireManage(orcamento.getConstructionSiteId(), actingUserId);
        if (orcamento.getStatus() != OrcamentoStatus.APPROVED) {
            throw new OrcamentoNotApprovedException(orcamentoId);
        }

        orcamento.complete(Instant.now());
        orcamentoRepository.save(orcamento);

        List<OrcamentoLineItem> items = lineItemRepository.findByOrcamentoId(orcamentoId);
        materialService.createFromOrcamento(orcamento, items);
        return orcamento;
    }

    public List<Orcamento> list(UUID siteId, UUID actingUserId, LocalDate dateFilter, UUID purchaseRequestIdFilter) {
        requireSite(siteId);
        siteAccessService.requireAccess(siteId, actingUserId);
        Instant dayStart = dateFilter == null ? null : dateFilter.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dateFilter == null ? null : dateFilter.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return orcamentoRepository.findFiltered(siteId, dayStart, dayEnd, purchaseRequestIdFilter);
    }

    public Orcamento get(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        siteAccessService.requireAccess(orcamento.getConstructionSiteId(), actingUserId);
        return orcamento;
    }

    /** Read-only convenience lookup for {@code OrcamentoResponse}'s display-only source-name field. */
    public String getSourcePurchaseRequestName(UUID sourcePurchaseRequestId) {
        if (sourcePurchaseRequestId == null) {
            return null;
        }
        return purchaseRequestRepository.findById(sourcePurchaseRequestId).map(PurchaseRequest::getName).orElse(null);
    }

    public List<OrcamentoLineItem> listLineItems(UUID orcamentoId) {
        return lineItemRepository.findByOrcamentoId(orcamentoId);
    }

    public List<OrcamentoApproval> listApprovals(UUID orcamentoId) {
        return approvalRepository.findByOrcamentoIdOrderByCycleNumberAscStepOrderAsc(orcamentoId);
    }

    private OrcamentoLineItem addLineItemInternal(
            UUID orcamentoId, String name, String type, BigDecimal quantity, BigDecimal unitPrice, UUID sourceId) {
        return lineItemRepository.save(
                new OrcamentoLineItem(UUID.randomUUID(), orcamentoId, name, type, quantity, unitPrice, sourceId));
    }

    private OrcamentoApproval requirePendingStep(Orcamento orcamento) {
        return approvalRepository
                .findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                        orcamento.getId(), orcamento.getCurrentApprovalCycle(), OrcamentoApprovalStatus.PENDING)
                .orElseThrow(() -> new NoPendingApprovalStepException(orcamento.getId()));
    }

    private SiteAccessContext requireStepAuthority(UUID siteId, UUID userId, OrcamentoApproval step) {
        SiteAccessContext access = siteAccessService.requireAccess(siteId, userId);
        if (access.companyStaff() || access.function() == step.getApproverFunction()) {
            return access;
        }
        throw new NotCurrentApprovalStepException(step.getOrcamentoId());
    }

    private void notifyStepPending(Orcamento orcamento, OrcamentoApproval step) {
        UUID siteId = orcamento.getConstructionSiteId();
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        List<SiteMembership> approvers = siteMembershipRepository
                .findByConstructionSiteIdAndFunction(siteId, step.getApproverFunction())
                .stream()
                .filter(m -> m.isActive() && m.getUserId() != null)
                .toList();

        for (SiteMembership approver : approvers) {
            userRepository.findById(approver.getUserId()).map(AppUser::getEmail).ifPresent(email ->
                    eventPublisher.publish(OrcamentoApprovalStepPendingEvent.TYPE, new OrcamentoApprovalStepPendingEvent(
                            orcamento.getId(), siteId, site.getName(), step.getApproverFunction().name(), email)));
        }
    }

    private void requireDraft(Orcamento orcamento) {
        if (orcamento.getStatus() != OrcamentoStatus.DRAFT) {
            throw new OrcamentoNotDraftException(orcamento.getId());
        }
    }

    private OrcamentoLineItem requireLineItem(UUID orcamentoId, UUID lineItemId) {
        return lineItemRepository
                .findById(lineItemId)
                .filter(i -> i.getOrcamentoId().equals(orcamentoId))
                .orElseThrow(() -> new OrcamentoNotFoundException(lineItemId));
    }

    private Orcamento requireOrcamento(UUID orcamentoId) {
        return orcamentoRepository.findById(orcamentoId).orElseThrow(() -> new OrcamentoNotFoundException(orcamentoId));
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.ORCAMENTO_MANAGE);
    }
}
