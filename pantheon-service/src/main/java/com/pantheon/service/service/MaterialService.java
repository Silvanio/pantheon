package com.pantheon.service.service;

import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.MaterialDeliveryPhoto;
import com.pantheon.service.entity.MaterialDeliveryStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.MaterialDeliveryStatusOrderException;
import com.pantheon.service.exception.MaterialNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialDeliveryPhotoRepository;
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Delivery-tracking {@link Material} records: created only from a concluded {@link Orcamento}
 * (see {@link OrcamentoService#conclude}), never any other way. See
 * {@code material-delivery-tracking}.
 */
@Service
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialDeliveryPhotoRepository photoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final StorageService storageService;

    public MaterialService(
            MaterialRepository materialRepository,
            MaterialDeliveryPhotoRepository photoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            StorageService storageService) {
        this.materialRepository = materialRepository;
        this.photoRepository = photoRepository;
        this.lineItemRepository = lineItemRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.storageService = storageService;
    }

    /** Called only from {@link OrcamentoService#conclude} — one Material per line item, never any other way. */
    @Transactional
    void createFromOrcamento(Orcamento orcamento, List<OrcamentoLineItem> lineItems) {
        Instant now = Instant.now();
        for (OrcamentoLineItem item : lineItems) {
            materialRepository.save(new Material(
                    UUID.randomUUID(), orcamento.getConstructionSiteId(), item.getId(), item.getName(),
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
        siteAccessService.requireAccess(siteId, actingUserId);
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

    public List<MaterialDeliveryPhoto> listPhotos(UUID materialId) {
        return photoRepository.findByMaterialId(materialId);
    }

    public byte[] getPhotoContent(UUID photoId, UUID actingUserId) {
        MaterialDeliveryPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new InvalidFileException("Photo not found: " + photoId));
        Material material = requireMaterial(photo.getMaterialId());
        siteAccessService.requireAccess(material.getConstructionSiteId(), actingUserId);
        return storageService.getObject(photo.getStorageKey());
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
