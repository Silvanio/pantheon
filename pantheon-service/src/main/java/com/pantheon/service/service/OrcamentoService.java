package com.pantheon.service.service;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.dto.OrcamentoLineItemRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.OrcamentoNotDraftException;
import com.pantheon.service.exception.OrcamentoNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.OrcamentoSpecifications;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A construction site's budget/quote from a single supplier: free-text line items and the
 * Rascunho/Bloqueado lifecycle, which follows its originating Pedido de Compra's approval state
 * (see {@link #lockAllForPurchaseRequest}/{@link #unlockAllForPurchaseRequest}). The approval
 * chain itself now lives on {@code PurchaseRequestService}. See {@code orcamento-management}.
 */
@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final ConstructionSiteRepository siteRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final FornecedorService fornecedorService;

    public OrcamentoService(
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            ConstructionSiteRepository siteRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository purchaseRequestItemRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            FornecedorService fornecedorService) {
        this.orcamentoRepository = orcamentoRepository;
        this.lineItemRepository = lineItemRepository;
        this.siteRepository = siteRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestItemRepository = purchaseRequestItemRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.fornecedorService = fornecedorService;
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

        purchaseRequestRepository.findById(purchaseRequestId).ifPresent(purchaseRequest -> {
            if (purchaseRequest.getStatus() == PurchaseRequestStatus.INICIADO) {
                purchaseRequest.markOrcado();
                purchaseRequestRepository.save(purchaseRequest);
            }
        });
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

    /** Called from {@code PurchaseRequestService.approveStep} when a cycle's final step is approved. */
    @Transactional
    public void lockAllForPurchaseRequest(UUID purchaseRequestId) {
        for (Orcamento orcamento : orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId)) {
            orcamento.lock();
            orcamentoRepository.save(orcamento);
        }
    }

    /** Called from {@code PurchaseRequestService.rejectStep}. */
    @Transactional
    public void unlockAllForPurchaseRequest(UUID purchaseRequestId) {
        for (Orcamento orcamento : orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId)) {
            orcamento.unlock();
            orcamentoRepository.save(orcamento);
        }
    }

    public Page<Orcamento> list(
            UUID siteId, UUID actingUserId, LocalDate dateFilter, UUID purchaseRequestIdFilter, String supplierFilter,
            Pageable pageable) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.ORCAMENTO_MANAGE);

        Instant dayStart = dateFilter != null ? dateFilter.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant dayEnd = dateFilter != null ? dateFilter.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Specification<Orcamento> spec = OrcamentoSpecifications.siteId(siteId);
        Specification<Orcamento> sourceSpec = OrcamentoSpecifications.sourcePurchaseRequestId(purchaseRequestIdFilter);
        if (sourceSpec != null) {
            spec = spec.and(sourceSpec);
        }
        Specification<Orcamento> supplierSpec = OrcamentoSpecifications.supplierContains(supplierFilter);
        if (supplierSpec != null) {
            spec = spec.and(supplierSpec);
        }
        Specification<Orcamento> dateSpec = OrcamentoSpecifications.createdOn(dayStart, dayEnd);
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }

        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return orcamentoRepository.findAll(spec, sorted);
    }

    public Orcamento get(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        var access = siteAccessService.requireAccess(orcamento.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(orcamento.getConstructionSiteId(), access, PermissionCapability.ORCAMENTO_MANAGE);
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

    /** Whether {@code lineItem} is currently the selected fulfillment of its source Pedido-de-Compra item, if any. */
    public boolean isSelected(OrcamentoLineItem lineItem) {
        if (lineItem.getSourcePurchaseRequestItemId() == null) {
            return false;
        }
        return purchaseRequestItemRepository
                .findById(lineItem.getSourcePurchaseRequestItemId())
                .map(item -> lineItem.getId().equals(item.getSelectedOrcamentoLineItemId()))
                .orElse(false);
    }

    /** Batch form of {@link #isSelected(OrcamentoLineItem)}, keyed by line item id, for detail/listing assembly. */
    public Map<UUID, Boolean> selectedFlags(List<OrcamentoLineItem> lineItems) {
        Map<UUID, Boolean> flags = new HashMap<>();
        for (OrcamentoLineItem lineItem : lineItems) {
            flags.put(lineItem.getId(), isSelected(lineItem));
        }
        return flags;
    }

    private OrcamentoLineItem addLineItemInternal(
            UUID orcamentoId, String name, String type, BigDecimal quantity, BigDecimal unitPrice, UUID sourceId) {
        return lineItemRepository.save(
                new OrcamentoLineItem(UUID.randomUUID(), orcamentoId, name, type, quantity, unitPrice, sourceId));
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
