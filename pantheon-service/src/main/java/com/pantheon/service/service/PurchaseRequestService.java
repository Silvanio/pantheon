package com.pantheon.service.service;

import com.pantheon.service.dto.PurchaseRequestComparisonResponse;
import com.pantheon.service.dto.PurchaseRequestItemCreationRequest;
import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestApproval;
import com.pantheon.service.entity.PurchaseRequestApprovalStatus;
import com.pantheon.service.entity.PurchaseRequestInvoice;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SitePurchaseRequestApprovalLevel;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.NoPendingApprovalStepException;
import com.pantheon.service.exception.NotCurrentApprovalStepException;
import com.pantheon.service.exception.PurchaseRequestInvoiceNotFoundException;
import com.pantheon.service.exception.PurchaseRequestNotConferidoException;
import com.pantheon.service.exception.PurchaseRequestNotIniciadoException;
import com.pantheon.service.exception.PurchaseRequestNotDeletableException;
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
import com.pantheon.service.repository.PurchaseRequestInvoiceRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.PurchaseRequestSpecifications;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The "Pedido de Compra" header/document: creation, listing, the item-selection comparison view,
 * and — since this change — the whole multi-step approval/conclusion lifecycle that used to live
 * on {@code Orcamento} (see {@code purchase-requests} and {@code purchase-request-approval-workflow}).
 */
@Service
public class PurchaseRequestService {

    private static final DateTimeFormatter NAME_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> INVOICE_ALLOWED_EXTENSIONS = Set.of("pdf", "xml", "jpg", "jpeg", "png");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository itemRepository;
    private final PurchaseRequestApprovalRepository approvalRepository;
    private final PurchaseRequestInvoiceRepository invoiceRepository;
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
    private final StorageService storageService;
    private final PushNotificationService pushNotificationService;

    public PurchaseRequestService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository itemRepository,
            PurchaseRequestApprovalRepository approvalRepository,
            PurchaseRequestInvoiceRepository invoiceRepository,
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
            EventPublisher eventPublisher,
            StorageService storageService,
            PushNotificationService pushNotificationService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.itemRepository = itemRepository;
        this.approvalRepository = approvalRepository;
        this.invoiceRepository = invoiceRepository;
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
        this.storageService = storageService;
        this.pushNotificationService = pushNotificationService;
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

        for (PurchaseRequestItemCreationRequest item : items == null ? List.<PurchaseRequestItemCreationRequest>of() : items) {
            itemRepository.save(new PurchaseRequestItem(
                    UUID.randomUUID(), siteId, purchaseRequest.getId(), item.name(), item.type(), item.quantity(),
                    item.unit(), actingUserId, now));
        }
        return purchaseRequest;
    }

    @Transactional
    public List<PurchaseRequestItem> addItems(
            UUID purchaseRequestId, UUID actingUserId, List<PurchaseRequestItemCreationRequest> items) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);
        if (purchaseRequest.getStatus() != PurchaseRequestStatus.INICIADO) {
            throw new PurchaseRequestNotIniciadoException(purchaseRequestId);
        }

        Instant now = Instant.now();
        List<PurchaseRequestItem> added = new ArrayList<>();
        for (PurchaseRequestItemCreationRequest item : items) {
            added.add(itemRepository.save(new PurchaseRequestItem(
                    UUID.randomUUID(), purchaseRequest.getConstructionSiteId(), purchaseRequest.getId(), item.name(),
                    item.type(), item.quantity(), item.unit(), actingUserId, now)));
        }
        return added;
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
        Page<PurchaseRequest> page = purchaseRequestRepository.findAll(spec, sorted);

        if (permissionService.resolve(siteId, access, PermissionCapability.PURCHASE_REQUEST) == AccessLevel.VIEW_AND_APPROVE) {
            SiteMembership membership = resolveSiteMembership(siteId, actingUserId, access);
            List<PurchaseRequest> visible = membership == null
                    ? List.of()
                    : page.getContent().stream().filter(pr -> isVisibleToViewAndApprove(pr, membership)).toList();
            return new PageImpl<>(visible, pageable, visible.size());
        }
        return page;
    }

    public PurchaseRequest get(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
        requireViewAndApproveVisibility(purchaseRequest, actingUserId, access);
        return purchaseRequest;
    }

    /** {@code access.siteMembership()} whenever present, else a direct lookup — needed because {@link SiteAccessContext} never populates a membership for company staff, even when one exists. */
    private SiteMembership resolveSiteMembership(UUID siteId, UUID userId, SiteAccessContext access) {
        if (access.siteMembership() != null) {
            return access.siteMembership();
        }
        return siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, userId)
                .filter(SiteMembership::isActive)
                .orElse(null);
    }

    /**
     * {@code VIEW_AND_APPROVE} only sees a Pedido de Compra once it's concluded, once their
     * function already decided a step in the current cycle, or once their function is the
     * currently actionable (lowest-order {@code PENDING}) step. Every level's step is created
     * {@code PENDING} up front at submission time (see {@code submitForApproval}), so a plain
     * "does a step for my function exist" check would leak visibility to every approver from the
     * moment of submission — it must check the specific step that's actionable right now instead.
     */
    private boolean isVisibleToViewAndApprove(PurchaseRequest purchaseRequest, SiteMembership membership) {
        if (purchaseRequest.getStatus() == PurchaseRequestStatus.CONCLUIDO) {
            return true;
        }
        boolean alreadyDecided = approvalRepository.existsByPurchaseRequestIdAndCycleNumberAndApproverFunctionAndStatusNot(
                purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), membership.getFunction(),
                PurchaseRequestApprovalStatus.PENDING);
        if (alreadyDecided) {
            return true;
        }
        return approvalRepository
                .findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                        purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING)
                .map(step -> step.getApproverFunction() == membership.getFunction())
                .orElse(false);
    }

    /** No-op unless the resolved access is {@code VIEW_AND_APPROVE}, in which case an irrelevant Pedido de Compra behaves as if it doesn't exist. */
    private void requireViewAndApproveVisibility(PurchaseRequest purchaseRequest, UUID actingUserId, SiteAccessContext access) {
        UUID siteId = purchaseRequest.getConstructionSiteId();
        if (permissionService.resolve(siteId, access, PermissionCapability.PURCHASE_REQUEST) != AccessLevel.VIEW_AND_APPROVE) {
            return;
        }
        SiteMembership membership = resolveSiteMembership(siteId, actingUserId, access);
        if (membership == null || !isVisibleToViewAndApprove(purchaseRequest, membership)) {
            throw new PurchaseRequestNotFoundException(purchaseRequest.getId());
        }
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

        UUID decidedBy = access.siteMembership() != null ? access.siteMembership().getId() : null;
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

        UUID decidedBy = access.siteMembership() != null ? access.siteMembership().getId() : null;
        step.reject(decidedBy, reason, Instant.now());
        approvalRepository.save(step);

        purchaseRequest.returnToOrcadoAfterRejection(reason);
        purchaseRequestRepository.save(purchaseRequest);
        orcamentoService.unlockAllForPurchaseRequest(purchaseRequestId);
        pushNotificationService.sendToUser(
                purchaseRequest.getCreatedBy(),
                "Pedido de compra rejeitado",
                purchaseRequest.getName() + " foi rejeitado: " + reason,
                Map.of("purchaseRequestId", purchaseRequest.getId().toString()));
        return purchaseRequest;
    }

    /** Anyone who can act on an approval step — {@code MANAGE} or {@code VIEW_AND_APPROVE} — may conclude, not just full managers. */
    @Transactional
    public PurchaseRequest conclude(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        UUID siteId = purchaseRequest.getConstructionSiteId();
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireApprove(siteId, access, PermissionCapability.PURCHASE_REQUEST);
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
        pushNotificationService.sendToUser(
                purchaseRequest.getCreatedBy(),
                "Pedido de compra concluído",
                purchaseRequest.getName() + " foi concluído.",
                Map.of("purchaseRequestId", purchaseRequest.getId().toString()));
        return purchaseRequest;
    }

    /**
     * Deletable at any stage before {@code CONCLUIDO}, cascading to every Orcamento linked to it
     * (and their line items), its whole approval history, invoices, and items. Once
     * {@code CONCLUIDO}, {@link com.pantheon.service.entity.Material} delivery-tracking rows hold
     * a hard FK into the linked Orcamentos' line items, so it can no longer be deleted.
     *
     * <p>{@code PurchaseRequestItem} and {@code OrcamentoLineItem} reference each other
     * ({@code selectedOrcamentoLineItemId}/{@code convertedToOrcamentoId} one way,
     * {@code sourcePurchaseRequestItemId} the other), so items must have those two columns
     * cleared before the Orcamentos/line items are deleted — otherwise deleting the referenced
     * line item/Orcamento first violates the item's own FK.
     */
    @Transactional
    public void delete(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);
        if (purchaseRequest.getStatus() == PurchaseRequestStatus.CONCLUIDO) {
            throw new PurchaseRequestNotDeletableException(purchaseRequestId);
        }

        List<PurchaseRequestItem> items = itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        for (PurchaseRequestItem item : items) {
            item.clearSelection();
            item.revertConversion();
        }
        itemRepository.saveAll(items);

        List<Orcamento> orcamentos = orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId);
        for (Orcamento orcamento : orcamentos) {
            orcamentoLineItemRepository.deleteAll(orcamentoLineItemRepository.findByOrcamentoId(orcamento.getId()));
        }
        orcamentoRepository.deleteAll(orcamentos);

        approvalRepository.deleteAll(
                approvalRepository.findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(purchaseRequestId));

        List<PurchaseRequestInvoice> invoices = invoiceRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        for (PurchaseRequestInvoice invoice : invoices) {
            storageService.deleteObject(invoice.getStorageKey());
        }
        invoiceRepository.deleteAll(invoices);

        itemRepository.deleteAll(items);
        purchaseRequestRepository.delete(purchaseRequest);
    }

    /** No status gate — an invoice can legitimately arrive before, during, or after approval/conclusion. */
    @Transactional
    public PurchaseRequestInvoice uploadInvoice(UUID purchaseRequestId, UUID actingUserId, MultipartFile file) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!INVOICE_ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "Only these file types are accepted: " + String.join(", ", INVOICE_ALLOWED_EXTENSIONS));
        }

        UUID invoiceId = UUID.randomUUID();
        String key = StorageKeys.purchaseRequestInvoiceKey(
                purchaseRequest.getConstructionSiteId(), purchaseRequestId, invoiceId, extension);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String originalName = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                ? file.getOriginalFilename()
                : "arquivo." + extension;
        storageService.putObject(key, readBytes(file), contentType);

        return invoiceRepository.save(new PurchaseRequestInvoice(
                invoiceId, purchaseRequestId, key, contentType, originalName, actingUserId, Instant.now()));
    }

    public List<PurchaseRequestInvoice> listInvoices(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
        requireViewAndApproveVisibility(purchaseRequest, actingUserId, access);
        return invoiceRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
    }

    public FileContent getInvoiceContent(UUID invoiceId, UUID actingUserId) {
        PurchaseRequestInvoice invoice = requireInvoice(invoiceId);
        PurchaseRequest purchaseRequest = requirePurchaseRequest(invoice.getPurchaseRequestId());
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
        requireViewAndApproveVisibility(purchaseRequest, actingUserId, access);
        return new FileContent(storageService.getObject(invoice.getStorageKey()), invoice.getContentType(), invoice.getOriginalName());
    }

    @Transactional
    public void deleteInvoice(UUID invoiceId, UUID actingUserId) {
        PurchaseRequestInvoice invoice = requireInvoice(invoiceId);
        PurchaseRequest purchaseRequest = requirePurchaseRequest(invoice.getPurchaseRequestId());
        requireManage(purchaseRequest.getConstructionSiteId(), actingUserId);

        storageService.deleteObject(invoice.getStorageKey());
        invoiceRepository.delete(invoice);
    }

    public record FileContent(byte[] bytes, String contentType, String originalName) {
    }

    private PurchaseRequestInvoice requireInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new PurchaseRequestInvoiceNotFoundException(invoiceId));
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "bin";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Read-only, derived comparison grid: rows are the header's items, columns are its linked Orcamentos. */
    public PurchaseRequestComparisonResponse getComparison(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requirePurchaseRequest(purchaseRequestId);
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
        requireViewAndApproveVisibility(purchaseRequest, actingUserId, access);

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

    /**
     * A user with an active {@link SiteMembership} on this site — company staff or not — must have
     * a function matching {@code step}'s, with no exception, and a {@code PURCHASE_REQUEST} access
     * level of {@code MANAGE} or {@code VIEW_AND_APPROVE}. Only a user with no {@link SiteMembership}
     * at all on this site falls back to the unrestricted company-staff bypass.
     */
    private SiteAccessContext requireStepAuthority(UUID siteId, UUID userId, PurchaseRequestApproval step) {
        SiteAccessContext access = siteAccessService.requireAccess(siteId, userId);
        SiteMembership membership = resolveSiteMembership(siteId, userId, access);

        if (membership != null) {
            boolean authorized = membership.getFunction() == step.getApproverFunction()
                    && permissionService.canApprove(siteId, access, PermissionCapability.PURCHASE_REQUEST);
            if (!authorized) {
                throw new NotCurrentApprovalStepException(step.getPurchaseRequestId());
            }
            return new SiteAccessContext(access.companyStaff(), membership);
        }

        if (access.companyStaff()) {
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
            pushNotificationService.sendToUser(
                    approver.getUserId(),
                    "Pedido de compra aguardando aprovação",
                    purchaseRequest.getName() + " (" + site.getName() + ") está aguardando sua aprovação.",
                    Map.of("purchaseRequestId", purchaseRequest.getId().toString()));
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
