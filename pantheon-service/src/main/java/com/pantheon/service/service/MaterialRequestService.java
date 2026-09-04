package com.pantheon.service.service;

import com.pantheon.service.dto.MaterialRequestCreationRequest;
import com.pantheon.service.dto.MaterialRequestItemCreationRequest;
import com.pantheon.service.entity.MaterialRequest;
import com.pantheon.service.entity.MaterialRequestItem;
import com.pantheon.service.entity.MaterialRequestStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.ReceiptVerification;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DuplicateReceiptVerificationException;
import com.pantheon.service.exception.MaterialRequestItemNotFoundException;
import com.pantheon.service.exception.MaterialRequestNotApprovedException;
import com.pantheon.service.exception.MaterialRequestNotFoundException;
import com.pantheon.service.exception.MaterialRequestNotPendingException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRequestItemRepository;
import com.pantheon.service.repository.MaterialRequestRepository;
import com.pantheon.service.repository.ReceiptVerificationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialRequestService {

    private final MaterialRequestRepository requestRepository;
    private final MaterialRequestItemRepository itemRepository;
    private final ReceiptVerificationRepository verificationRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public MaterialRequestService(
            MaterialRequestRepository requestRepository,
            MaterialRequestItemRepository itemRepository,
            ReceiptVerificationRepository verificationRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.requestRepository = requestRepository;
        this.itemRepository = itemRepository;
        this.verificationRepository = verificationRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    @Transactional
    public MaterialRequest create(UUID siteId, UUID actingUserId, MaterialRequestCreationRequest request) {
        requireSite(siteId);
        requireCapability(siteId, actingUserId, PermissionCapability.MATERIAL_REQUEST);

        MaterialRequest materialRequest = new MaterialRequest(UUID.randomUUID(), siteId, actingUserId, Instant.now());
        requestRepository.save(materialRequest);

        for (MaterialRequestItemCreationRequest itemRequest : request.items()) {
            itemRepository.save(new MaterialRequestItem(
                    UUID.randomUUID(), materialRequest.getId(), itemRequest.materialId(),
                    itemRequest.requestedQuantity()));
        }
        return materialRequest;
    }

    @Transactional
    public MaterialRequest approve(UUID requestId, UUID actingUserId) {
        MaterialRequest request = requireRequest(requestId);
        requireCapability(request.getConstructionSiteId(), actingUserId, PermissionCapability.MATERIAL_APPROVAL);
        requirePending(request);

        request.approve(actingUserId, Instant.now());
        return requestRepository.save(request);
    }

    @Transactional
    public MaterialRequest reject(UUID requestId, UUID actingUserId, String reason) {
        MaterialRequest request = requireRequest(requestId);
        requireCapability(request.getConstructionSiteId(), actingUserId, PermissionCapability.MATERIAL_APPROVAL);
        requirePending(request);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }

        request.reject(actingUserId, reason, Instant.now());
        return requestRepository.save(request);
    }

    @Transactional
    public ReceiptVerification recordVerification(
            UUID requestId, UUID actingUserId, UUID itemId, BigDecimal receivedQuantity, String note) {
        MaterialRequest request = requireRequest(requestId);
        siteAccessService.requireAccess(request.getConstructionSiteId(), actingUserId);

        if (request.getStatus() != MaterialRequestStatus.APPROVED
                && request.getStatus() != MaterialRequestStatus.PARTIALLY_RECEIVED
                && request.getStatus() != MaterialRequestStatus.RECEIVED) {
            throw new MaterialRequestNotApprovedException(requestId);
        }

        MaterialRequestItem item = itemRepository
                .findById(itemId)
                .filter(i -> i.getMaterialRequestId().equals(requestId))
                .orElseThrow(() -> new MaterialRequestItemNotFoundException(itemId));

        if (verificationRepository.findByMaterialRequestItemId(itemId).isPresent()) {
            throw new DuplicateReceiptVerificationException(itemId);
        }

        ReceiptVerification verification = new ReceiptVerification(
                UUID.randomUUID(), itemId, receivedQuantity, actingUserId, note, Instant.now());
        verificationRepository.save(verification);

        List<MaterialRequestItem> allItems = itemRepository.findByMaterialRequestId(requestId);
        List<ReceiptVerification> allVerifications = verificationRepository.findByMaterialRequestItemIdIn(
                allItems.stream().map(MaterialRequestItem::getId).toList());
        boolean fullyReceived = allVerifications.size() >= allItems.size();
        request.updateReceiptStatus(fullyReceived, Instant.now());
        requestRepository.save(request);

        return verification;
    }

    public List<MaterialRequest> list(UUID siteId, UUID actingUserId, MaterialRequestStatus statusFilter) {
        requireSite(siteId);
        siteAccessService.requireAccess(siteId, actingUserId);
        return statusFilter != null
                ? requestRepository.findByConstructionSiteIdAndStatusOrderByCreatedAtDesc(siteId, statusFilter)
                : requestRepository.findByConstructionSiteIdOrderByCreatedAtDesc(siteId);
    }

    public MaterialRequest getRequest(UUID requestId, UUID actingUserId) {
        MaterialRequest request = requireRequest(requestId);
        siteAccessService.requireAccess(request.getConstructionSiteId(), actingUserId);
        return request;
    }

    public List<MaterialRequestItem> listItems(UUID requestId) {
        return itemRepository.findByMaterialRequestId(requestId);
    }

    public List<ReceiptVerification> listVerifications(List<UUID> itemIds) {
        return itemIds.isEmpty() ? List.of() : verificationRepository.findByMaterialRequestItemIdIn(itemIds);
    }

    private MaterialRequest requireRequest(UUID requestId) {
        return requestRepository.findById(requestId).orElseThrow(() -> new MaterialRequestNotFoundException(requestId));
    }

    private void requireSite(UUID siteId) {
        siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireCapability(UUID siteId, UUID userId, PermissionCapability capability) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, capability);
    }

    private void requirePending(MaterialRequest request) {
        if (request.getStatus() != MaterialRequestStatus.PENDING) {
            throw new MaterialRequestNotPendingException(request.getId());
        }
    }
}
