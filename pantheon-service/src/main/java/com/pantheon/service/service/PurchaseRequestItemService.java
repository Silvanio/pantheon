package com.pantheon.service.service;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.exception.ItemsSpanMultiplePurchaseRequestsException;
import com.pantheon.service.exception.OrcamentoLineItemNotLinkedException;
import com.pantheon.service.exception.OrcamentoNotFoundException;
import com.pantheon.service.exception.PurchaseRequestItemNotFoundException;
import com.pantheon.service.exception.PurchaseRequestNotFoundException;
import com.pantheon.service.exception.SelectionNotAllowedException;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Items of a "Pedido de Compra" header: conversion into an Orcamento, and the per-item supplier
 * selection that makes a mixed, multi-supplier purchase possible. See {@code purchase-requests}.
 */
@Service
public class PurchaseRequestItemService {

    private final PurchaseRequestItemRepository itemRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository orcamentoLineItemRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final OrcamentoService orcamentoService;

    public PurchaseRequestItemService(
            PurchaseRequestItemRepository itemRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository orcamentoLineItemRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            OrcamentoService orcamentoService) {
        this.itemRepository = itemRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.orcamentoLineItemRepository = orcamentoLineItemRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.orcamentoService = orcamentoService;
    }

    public List<PurchaseRequestItem> list(UUID purchaseRequestId, UUID actingUserId, PurchaseRequestItemStatus statusFilter) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
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
        }

        Orcamento orcamento =
                orcamentoService.createFromPurchaseRequestItems(siteId, actingUserId, items, purchaseRequestId, fornecedor);

        Instant now = Instant.now();
        for (PurchaseRequestItem item : items) {
            if (item.getStatus() == PurchaseRequestItemStatus.PENDING) {
                item.convertTo(orcamento.getId(), now);
                itemRepository.save(item);
            }
        }
        return orcamento;
    }

    /**
     * Sets or clears {@code itemId}'s {@code selectedOrcamentoLineItemId}, only while the header
     * is Orçado. See {@code purchase-requests}' "Per-item supplier selection".
     */
    @Transactional
    public PurchaseRequestItem setSelection(UUID itemId, UUID actingUserId, UUID orcamentoLineItemId) {
        PurchaseRequestItem item = requireItem(itemId);
        PurchaseRequest purchaseRequest = requirePurchaseRequest(item.getPurchaseRequestId());
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);

        if (purchaseRequest.getStatus() != PurchaseRequestStatus.ORCADO) {
            throw new SelectionNotAllowedException(itemId);
        }

        if (orcamentoLineItemId == null) {
            item.clearSelection();
        } else {
            OrcamentoLineItem lineItem = orcamentoLineItemRepository
                    .findById(orcamentoLineItemId)
                    .orElseThrow(() -> new OrcamentoLineItemNotLinkedException(orcamentoLineItemId));
            Orcamento orcamento = orcamentoRepository
                    .findById(lineItem.getOrcamentoId())
                    .orElseThrow(() -> new OrcamentoNotFoundException(lineItem.getOrcamentoId()));

            boolean sameHeader = purchaseRequest.getId().equals(orcamento.getSourcePurchaseRequestId());
            boolean itemMatches = lineItem.getSourcePurchaseRequestItemId() == null
                    || Objects.equals(lineItem.getSourcePurchaseRequestItemId(), itemId);
            if (!sameHeader || !itemMatches) {
                throw new OrcamentoLineItemNotLinkedException(orcamentoLineItemId);
            }
            item.select(orcamentoLineItemId);
        }
        return itemRepository.save(item);
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
