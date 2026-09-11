package com.pantheon.service.service;

import com.pantheon.service.dto.PurchaseRequestItemCreationRequest;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.PurchaseRequestNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The "Pedido de Compra" header/document. See {@code purchase-requests}. */
@Service
public class PurchaseRequestService {

    private static final DateTimeFormatter NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository itemRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public PurchaseRequestService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository itemRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.itemRepository = itemRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
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

    public List<PurchaseRequest> list(UUID siteId, UUID actingUserId, LocalDate dateFilter) {
        requireSite(siteId);
        siteAccessService.requireAccess(siteId, actingUserId);
        if (dateFilter == null) {
            return purchaseRequestRepository.findByConstructionSiteIdOrderByCreatedAtDesc(siteId);
        }
        Instant dayStart = dateFilter.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dateFilter.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return purchaseRequestRepository.findByConstructionSiteIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                siteId, dayStart, dayEnd);
    }

    public PurchaseRequest get(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        return purchaseRequest;
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
