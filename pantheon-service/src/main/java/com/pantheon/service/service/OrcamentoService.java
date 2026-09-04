package com.pantheon.service.service;

import com.pantheon.service.dto.OrcamentoCreationRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.AttachmentKind;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MaterialRequest;
import com.pantheon.service.entity.MaterialRequestStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoAttachment;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.MaterialRequestNotFoundException;
import com.pantheon.service.exception.NotSiteClientException;
import com.pantheon.service.exception.OrcamentoNotFoundException;
import com.pantheon.service.exception.OrcamentoNotSentException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.OrcamentoSentEvent;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRequestRepository;
import com.pantheon.service.repository.OrcamentoAttachmentRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
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
 * The client-facing budget ("orçamento") drafted against a {@link MaterialRequest}: draft with
 * priced line items, send to the site's client, client approve/reject, payment-proof/invoice
 * attachments. See {@code material-request-workflow}'s "Budget (orçamento)" requirements.
 */
@Service
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final OrcamentoAttachmentRepository attachmentRepository;
    private final MaterialRequestRepository materialRequestRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final AppUserRepository userRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;
    private final StorageService storageService;
    private final EventPublisher eventPublisher;

    public OrcamentoService(
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            OrcamentoAttachmentRepository attachmentRepository,
            MaterialRequestRepository materialRequestRepository,
            ConstructionSiteRepository siteRepository,
            SiteMembershipRepository siteMembershipRepository,
            AppUserRepository userRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            StorageService storageService,
            EventPublisher eventPublisher) {
        this.orcamentoRepository = orcamentoRepository;
        this.lineItemRepository = lineItemRepository;
        this.attachmentRepository = attachmentRepository;
        this.materialRequestRepository = materialRequestRepository;
        this.siteRepository = siteRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.userRepository = userRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Orcamento create(UUID materialRequestId, UUID actingUserId, OrcamentoCreationRequest request) {
        MaterialRequest materialRequest = requireRequest(materialRequestId);
        requireRequestAccess(materialRequest, actingUserId);

        Orcamento orcamento = new Orcamento(UUID.randomUUID(), materialRequestId, actingUserId, Instant.now());
        orcamentoRepository.save(orcamento);

        request.items().forEach(item -> lineItemRepository.save(new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), item.materialRequestItemId(), item.unitPrice())));

        return orcamento;
    }

    @Transactional
    public Orcamento send(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        MaterialRequest materialRequest = requireRequest(orcamento.getMaterialRequestId());
        requireRequestAccess(materialRequest, actingUserId);

        orcamento.send(Instant.now());
        orcamentoRepository.save(orcamento);

        publishOrcamentoSent(orcamento, materialRequest);
        return orcamento;
    }

    private void publishOrcamentoSent(Orcamento orcamento, MaterialRequest materialRequest) {
        UUID siteId = materialRequest.getConstructionSiteId();
        ConstructionSite site =
                siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
        List<SiteMembership> clients = siteMembershipRepository.findByConstructionSiteId(siteId).stream()
                .filter(m -> m.isActive() && m.getFunction() == ConstructionFunction.CLIENT && m.getUserId() != null)
                .toList();

        for (SiteMembership client : clients) {
            userRepository.findById(client.getUserId()).map(AppUser::getEmail).ifPresent(email -> eventPublisher.publish(
                    OrcamentoSentEvent.TYPE,
                    new OrcamentoSentEvent(orcamento.getId(), materialRequest.getId(), siteId, site.getName(), email)));
        }
    }

    @Transactional
    public Orcamento approve(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        MaterialRequest materialRequest = requireRequest(orcamento.getMaterialRequestId());
        requireClient(materialRequest.getConstructionSiteId(), actingUserId);
        requireSent(orcamento);

        Instant now = Instant.now();
        orcamento.approve(now);
        orcamentoRepository.save(orcamento);

        if (materialRequest.getStatus() == MaterialRequestStatus.PENDING) {
            materialRequest.approve(actingUserId, now);
            materialRequestRepository.save(materialRequest);
        }
        return orcamento;
    }

    @Transactional
    public Orcamento reject(UUID orcamentoId, UUID actingUserId, String reason) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        MaterialRequest materialRequest = requireRequest(orcamento.getMaterialRequestId());
        requireClient(materialRequest.getConstructionSiteId(), actingUserId);
        requireSent(orcamento);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }

        orcamento.reject(reason, Instant.now());
        return orcamentoRepository.save(orcamento);
    }

    @Transactional
    public OrcamentoAttachment uploadAttachment(
            UUID orcamentoId, UUID actingUserId, AttachmentKind kind, MultipartFile file) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        MaterialRequest materialRequest = requireRequest(orcamento.getMaterialRequestId());
        siteAccessService.requireAccess(materialRequest.getConstructionSiteId(), actingUserId);

        if (file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }

        UUID attachmentId = UUID.randomUUID();
        String extension = extensionOf(file.getOriginalFilename());
        String key = StorageKeys.orcamentoAttachmentKey(materialRequest.getId(), orcamentoId, attachmentId, extension);
        storageService.putObject(key, readBytes(file), file.getContentType());

        OrcamentoAttachment attachment = new OrcamentoAttachment(
                attachmentId, orcamentoId, kind, key, file.getContentType(), file.getOriginalFilename(),
                actingUserId, Instant.now());
        return attachmentRepository.save(attachment);
    }

    public List<Orcamento> list(UUID materialRequestId, UUID actingUserId) {
        MaterialRequest materialRequest = requireRequest(materialRequestId);
        siteAccessService.requireAccess(materialRequest.getConstructionSiteId(), actingUserId);
        return orcamentoRepository.findByMaterialRequestId(materialRequestId);
    }

    public Orcamento get(UUID orcamentoId, UUID actingUserId) {
        Orcamento orcamento = requireOrcamento(orcamentoId);
        MaterialRequest materialRequest = requireRequest(orcamento.getMaterialRequestId());
        siteAccessService.requireAccess(materialRequest.getConstructionSiteId(), actingUserId);
        return orcamento;
    }

    public List<OrcamentoLineItem> listLineItems(UUID orcamentoId) {
        return lineItemRepository.findByOrcamentoId(orcamentoId);
    }

    public List<OrcamentoAttachment> listAttachments(UUID orcamentoId) {
        return attachmentRepository.findByOrcamentoId(orcamentoId);
    }

    public byte[] getAttachmentContent(UUID attachmentId, UUID actingUserId) {
        OrcamentoAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new InvalidFileException("Attachment not found: " + attachmentId));
        Orcamento orcamento = requireOrcamento(attachment.getOrcamentoId());
        MaterialRequest materialRequest = requireRequest(orcamento.getMaterialRequestId());
        siteAccessService.requireAccess(materialRequest.getConstructionSiteId(), actingUserId);
        return storageService.getObject(attachment.getStorageKey());
    }

    private void requireRequestAccess(MaterialRequest materialRequest, UUID userId) {
        var access = siteAccessService.requireAccess(materialRequest.getConstructionSiteId(), userId);
        permissionService.requireManage(materialRequest.getConstructionSiteId(), access, PermissionCapability.MATERIAL_REQUEST);
    }

    private void requireClient(UUID constructionSiteId, UUID userId) {
        boolean isClient = siteMembershipRepository
                .findByConstructionSiteIdAndUserId(constructionSiteId, userId)
                .filter(m -> m.isActive() && m.getFunction() == ConstructionFunction.CLIENT)
                .isPresent();
        if (!isClient) {
            throw new NotSiteClientException(constructionSiteId);
        }
    }

    private void requireSent(Orcamento orcamento) {
        if (orcamento.getStatus() != OrcamentoStatus.SENT) {
            throw new OrcamentoNotSentException(orcamento.getId());
        }
    }

    private Orcamento requireOrcamento(UUID orcamentoId) {
        return orcamentoRepository.findById(orcamentoId).orElseThrow(() -> new OrcamentoNotFoundException(orcamentoId));
    }

    private MaterialRequest requireRequest(UUID materialRequestId) {
        return materialRequestRepository
                .findById(materialRequestId)
                .orElseThrow(() -> new MaterialRequestNotFoundException(materialRequestId));
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
