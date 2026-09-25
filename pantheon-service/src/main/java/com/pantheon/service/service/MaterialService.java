package com.pantheon.service.service;

import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.MaterialDeliveryPhoto;
import com.pantheon.service.entity.MaterialDeliveryStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.MaterialDeliveryStatusOrderException;
import com.pantheon.service.exception.MaterialNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialDeliveryPhotoRepository;
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Delivery-tracking {@link Material} records: created only from a concluded
 * {@link PurchaseRequest} (see {@code PurchaseRequestService#conclude}), never any other way.
 * See {@code material-delivery-tracking}.
 */
@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialDeliveryPhotoRepository photoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final StorageService storageService;

    public MaterialService(
            MaterialRepository materialRepository,
            MaterialDeliveryPhotoRepository photoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            OrcamentoRepository orcamentoRepository,
            PurchaseRequestRepository purchaseRequestRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            StorageService storageService) {
        this.materialRepository = materialRepository;
        this.photoRepository = photoRepository;
        this.lineItemRepository = lineItemRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.storageService = storageService;
    }

    /**
     * Called only from {@code PurchaseRequestService.conclude} — one Material per selected line
     * item, possibly drawn from several different Orcamentos/suppliers, never any other way.
     */
    @Transactional
    void createFromPurchaseRequestSelections(PurchaseRequest purchaseRequest, List<OrcamentoLineItem> selectedLineItems) {
        Instant now = Instant.now();
        for (OrcamentoLineItem item : selectedLineItems) {
            materialRepository.save(new Material(
                    UUID.randomUUID(), purchaseRequest.getConstructionSiteId(), item.getId(), item.getName(),
                    item.getType(), item.getQuantity(), now));
        }
    }

    @Transactional
    public Material markDelivered(UUID materialId, UUID actingUserId) {
        Material material = requireMaterial(materialId);
        requireManage(material.getConstructionSiteId(), actingUserId);
        if (material.getStatus() != MaterialDeliveryStatus.AWAITING_DELIVERY) {
            throw new MaterialDeliveryStatusOrderException(materialId);
        }
        material.markDelivered(actingUserId, Instant.now());
        return materialRepository.save(material);
    }

    @Transactional
    public Material markChecked(UUID materialId, UUID actingUserId, List<MultipartFile> photos) {
        Material material = requireMaterial(materialId);
        requireManage(material.getConstructionSiteId(), actingUserId);
        if (material.getStatus() != MaterialDeliveryStatus.DELIVERED) {
            throw new MaterialDeliveryStatusOrderException(materialId);
        }

        Instant now = Instant.now();
        material.markChecked(actingUserId, now);
        materialRepository.save(material);

        if (photos != null) {
            for (MultipartFile file : photos) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                UUID photoId = UUID.randomUUID();
                String extension = extensionOf(file.getOriginalFilename());
                String key = StorageKeys.materialDeliveryPhotoKey(
                        material.getConstructionSiteId(), materialId, photoId, extension);
                storageService.putObject(key, readBytes(file), file.getContentType());
                photoRepository.save(new MaterialDeliveryPhoto(
                        photoId, materialId, key, file.getContentType(), actingUserId, now));
            }
        }
        return material;
    }

    public List<Material> list(UUID siteId, UUID actingUserId, UUID orcamentoIdFilter) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        requireVisibleToOrcamentoOrPurchaseRequest(siteId, access);
        List<Material> materials = materialRepository.findByConstructionSiteId(siteId);
        if (orcamentoIdFilter == null) {
            return materials;
        }
        List<UUID> lineItemIds =
                lineItemRepository.findByOrcamentoId(orcamentoIdFilter).stream().map(OrcamentoLineItem::getId).toList();
        return materials.stream().filter(m -> lineItemIds.contains(m.getOrcamentoLineItemId())).toList();
    }

    /** Used to assemble an Orcamento's detail view; caller has already authorized access to the Orcamento. */
    public List<Material> listByLineItemIds(List<UUID> lineItemIds) {
        return lineItemIds.isEmpty() ? List.of() : materialRepository.findByOrcamentoLineItemIdIn(lineItemIds);
    }

    /** A Material's originating Pedido de Compra, if it can still be traced. */
    public record SourcePurchaseRequestRef(UUID id, String name) {
    }

    /**
     * Resolves each material's originating Pedido de Compra via
     * Material -&gt; OrcamentoLineItem -&gt; Orcamento -&gt; PurchaseRequest, batched across the whole
     * list to avoid N+1 queries — for the site-wide Materiais screen, which links each row back to
     * its Pedido de Compra. A material with no resolvable chain (shouldn't normally happen) is
     * simply absent from the returned map.
     */
    public Map<UUID, SourcePurchaseRequestRef> resolveSourcePurchaseRequests(List<Material> materials) {
        List<UUID> lineItemIds = materials.stream().map(Material::getOrcamentoLineItemId).distinct().toList();
        Map<UUID, OrcamentoLineItem> lineItemsById = lineItemRepository.findAllById(lineItemIds).stream()
                .collect(Collectors.toMap(OrcamentoLineItem::getId, li -> li));

        List<UUID> orcamentoIds = lineItemsById.values().stream().map(OrcamentoLineItem::getOrcamentoId).distinct().toList();
        Map<UUID, Orcamento> orcamentosById = orcamentoRepository.findAllById(orcamentoIds).stream()
                .collect(Collectors.toMap(Orcamento::getId, o -> o));

        List<UUID> purchaseRequestIds = orcamentosById.values().stream()
                .map(Orcamento::getSourcePurchaseRequestId).filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> purchaseRequestNamesById = purchaseRequestRepository.findAllById(purchaseRequestIds).stream()
                .collect(Collectors.toMap(PurchaseRequest::getId, PurchaseRequest::getName));

        Map<UUID, SourcePurchaseRequestRef> result = new HashMap<>();
        for (Material material : materials) {
            OrcamentoLineItem lineItem = lineItemsById.get(material.getOrcamentoLineItemId());
            Orcamento orcamento = lineItem != null ? orcamentosById.get(lineItem.getOrcamentoId()) : null;
            UUID purchaseRequestId = orcamento != null ? orcamento.getSourcePurchaseRequestId() : null;
            if (purchaseRequestId != null) {
                result.put(material.getId(), new SourcePurchaseRequestRef(
                        purchaseRequestId, purchaseRequestNamesById.get(purchaseRequestId)));
            }
        }
        return result;
    }

    public List<MaterialDeliveryPhoto> listPhotos(UUID materialId) {
        return photoRepository.findByMaterialId(materialId);
    }

    public byte[] getPhotoContent(UUID photoId, UUID actingUserId) {
        MaterialDeliveryPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new InvalidFileException("Photo not found: " + photoId));
        Material material = requireMaterial(photo.getMaterialId());
        var access = siteAccessService.requireAccess(material.getConstructionSiteId(), actingUserId);
        requireVisibleToOrcamentoOrPurchaseRequest(material.getConstructionSiteId(), access);
        return storageService.getObject(photo.getStorageKey());
    }

    /**
     * Materials are surfaced both from the Orçamento detail view and, post-conclusion, from the
     * Pedido de Compra detail view — visible to a member with either capability, not just one.
     */
    private void requireVisibleToOrcamentoOrPurchaseRequest(UUID siteId, SiteAccessContext access) {
        boolean hiddenFromOrcamento =
                permissionService.resolve(siteId, access, PermissionCapability.ORCAMENTO_MANAGE) == AccessLevel.HIDDEN;
        boolean hiddenFromPurchaseRequest =
                permissionService.resolve(siteId, access, PermissionCapability.PURCHASE_REQUEST) == AccessLevel.HIDDEN;
        if (hiddenFromOrcamento && hiddenFromPurchaseRequest) {
            throw new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST);
        }
    }

    private Material requireMaterial(UUID materialId) {
        return materialRepository.findById(materialId).orElseThrow(() -> new MaterialNotFoundException(materialId));
    }

    private void requireSite(UUID siteId) {
        siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }

    private void requireManage(UUID siteId, UUID userId) {
        var access = siteAccessService.requireAccess(siteId, userId);
        permissionService.requireManage(siteId, access, PermissionCapability.ORCAMENTO_MANAGE);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
