package com.pantheon.service.service;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import com.pantheon.service.exception.ItemsSpanMultiplePurchaseRequestsException;
import com.pantheon.service.exception.PurchaseRequestItemAlreadyConvertedException;
import com.pantheon.service.exception.PurchaseRequestItemNotFoundException;
import com.pantheon.service.exception.PurchaseRequestNotFoundException;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Items of a "Pedido de Compra" header, and their conversion into an Orcamento. See {@code purchase-requests}. */
@Service
public class PurchaseRequestItemService {

    private final PurchaseRequestItemRepository itemRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final OrcamentoService orcamentoService;

    public PurchaseRequestItemService(
            PurchaseRequestItemRepository itemRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            OrcamentoService orcamentoService) {
        this.itemRepository = itemRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.orcamentoService = orcamentoService;
    }

    public List<PurchaseRequestItem> list(UUID purchaseRequestId, UUID actingUserId, PurchaseRequestItemStatus statusFilter) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        return statusFilter != null
                ? itemRepository.findByPurchaseRequestIdAndStatusOrderByCreatedAtDesc(purchaseRequestId, statusFilter)
                : itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
    }

    @Transactional
    public Orcamento convertToOrcamento(
            UUID purchaseRequestId, UUID actingUserId, List<UUID> itemIds, FornecedorRequest fornecedor) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        UUID siteId = purchaseRequest.getConstructionSiteId();
        requireManage(siteId, actingUserId);

        List<PurchaseRequestItem> items = itemIds.stream().map(this::requireItem).toList();
        for (PurchaseRequestItem item : items) {
            if (!item.getPurchaseRequestId().equals(purchaseRequestId)) {
                throw new ItemsSpanMultiplePurchaseRequestsException();
            }
            if (item.getStatus() != PurchaseRequestItemStatus.PENDING) {
                throw new PurchaseRequestItemAlreadyConvertedException(item.getId());
            }
        }

        Orcamento orcamento =
                orcamentoService.createFromPurchaseRequestItems(siteId, actingUserId, items, purchaseRequestId, fornecedor);

        Instant now = Instant.now();
        for (PurchaseRequestItem item : items) {
            item.convertTo(orcamento.getId(), now);
            itemRepository.save(item);
        }
        return orcamento;
    }

    private PurchaseRequestItem requireItem(UUID itemId) {
        return itemRepository.findById(itemId).orElseThrow(() -> new PurchaseRequestItemNotFoundException(itemId));
    }

    private PurchaseRequest requirePurchaseRequest(UUID purchaseRequestId) {
        return purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> new PurchaseRequestNotFoundException(purchaseRequestId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.PURCHASE_REQUEST);
    }
}
