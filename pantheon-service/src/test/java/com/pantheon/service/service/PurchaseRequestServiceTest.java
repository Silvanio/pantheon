package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.PurchaseRequestComparisonResponse;
import com.pantheon.service.dto.PurchaseRequestItemCreationRequest;
import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
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
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.NotCurrentApprovalStepException;
import com.pantheon.service.exception.InvalidFileException;
import com.pantheon.service.exception.PurchaseRequestInvoiceNotFoundException;
import com.pantheon.service.exception.PurchaseRequestNotConferidoException;
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
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.storage.StorageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestItemRepository itemRepository;

    @Mock
    private PurchaseRequestApprovalRepository approvalRepository;

    @Mock
    private PurchaseRequestInvoiceRepository invoiceRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoLineItemRepository orcamentoLineItemRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private SitePurchaseRequestApprovalLevelService approvalLevelService;

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private MaterialService materialService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private StorageService storageService;

    @Mock
    private PushNotificationService pushNotificationService;

    private PurchaseRequestService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new PurchaseRequestService(
                purchaseRequestRepository, itemRepository, approvalRepository, invoiceRepository, siteRepository,
                siteMembershipRepository, userRepository, orcamentoRepository, orcamentoLineItemRepository,
                siteAccessService, permissionService, approvalLevelService, orcamentoService, materialService,
                eventPublisher, storageService, pushNotificationService);

        siteId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    private ConstructionSite site(UUID id) {
        return new ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private List<PurchaseRequestItemCreationRequest> oneItem() {
        return List.of(new PurchaseRequestItemCreationRequest("Cimento", "Saco", new BigDecimal("10"), "saco"));
    }

    private PurchaseRequest purchaseRequest() {
        return new PurchaseRequest(UUID.randomUUID(), siteId, "Pedido 07/09/2026 #1", UUID.randomUUID(), Instant.now());
    }

    private PurchaseRequestItem item(UUID purchaseRequestId) {
        return new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequestId, "Cimento", "Saco", new BigDecimal("10"), "saco",
                UUID.randomUUID(), Instant.now());
    }

    private SiteMembership activeMember(UUID userId, ConstructionFunction function) {
        SiteMembership member = SiteMembership.invited(UUID.randomUUID(), siteId, userId, function, null, null, Instant.now());
        member.accept();
        return member;
    }

    @Test
    void firstPurchaseRequestOfTheDayGetsSequenceOne() {
        when(purchaseRequestRepository.countByConstructionSiteIdAndCreatedAtBetween(eq(siteId), any(), any())).thenReturn(0L);

        PurchaseRequest result = service.create(siteId, UUID.randomUUID(), oneItem());

        assertThat(result.getName()).matches("Pedido \\d{2}/\\d{2}/\\d{4} #1");
        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.INICIADO);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));
    }

    @Test
    void secondPurchaseRequestOfTheSameDayGetsSequenceTwo() {
        when(purchaseRequestRepository.countByConstructionSiteIdAndCreatedAtBetween(eq(siteId), any(), any())).thenReturn(1L);

        PurchaseRequest result = service.create(siteId, UUID.randomUUID(), oneItem());

        assertThat(result.getName()).matches("Pedido \\d{2}/\\d{2}/\\d{4} #2");
    }

    @Test
    void itemsArePersistedLinkedToTheNewHeader() {
        when(purchaseRequestRepository.countByConstructionSiteIdAndCreatedAtBetween(eq(siteId), any(), any())).thenReturn(0L);

        PurchaseRequest result = service.create(siteId, UUID.randomUUID(), oneItem());

        verify(itemRepository).save(argThat(
                item -> item.getPurchaseRequestId().equals(result.getId()) && item.getName().equals("Cimento")));
    }

    @Test
    void createWithNoItemsPersistsEmptyHeaderInIniciado() {
        when(purchaseRequestRepository.countByConstructionSiteIdAndCreatedAtBetween(eq(siteId), any(), any())).thenReturn(0L);

        PurchaseRequest result = service.create(siteId, UUID.randomUUID(), List.of());

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.INICIADO);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void addItemsToIniciadoHeaderPersistsThemAndReturnsThem() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        List<PurchaseRequestItem> added = service.addItems(purchaseRequest.getId(), UUID.randomUUID(), oneItem());

        assertThat(added).hasSize(1);
        assertThat(added.get(0).getPurchaseRequestId()).isEqualTo(purchaseRequest.getId());
        assertThat(added.get(0).getName()).isEqualTo("Cimento");
        verify(itemRepository).save(argThat(item -> item.getPurchaseRequestId().equals(purchaseRequest.getId())));
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));
    }

    @Test
    void addItemsWhileOrcadoSucceedsAndResetsHeaderToIniciado() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        List<PurchaseRequestItem> added = service.addItems(purchaseRequest.getId(), UUID.randomUUID(), oneItem());

        assertThat(added).hasSize(1);
        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.INICIADO);
        verify(itemRepository).save(argThat(item -> item.getPurchaseRequestId().equals(purchaseRequest.getId())));
        verify(purchaseRequestRepository).save(purchaseRequest);
    }

    @Test
    void addItemsWhileConferidoOrConcluidoSucceedsAndResetsHeaderToIniciado() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.approve(Instant.now());
        purchaseRequest.complete(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        List<PurchaseRequestItem> added = service.addItems(purchaseRequest.getId(), UUID.randomUUID(), oneItem());

        assertThat(added).hasSize(1);
        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.INICIADO);
        verify(purchaseRequestRepository).save(purchaseRequest);
    }

    @Test
    void addItemsRejectsMemberWithoutManageAccess() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.addItems(purchaseRequest.getId(), UUID.randomUUID(), oneItem()))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void listRejectsMemberHiddenFromPurchaseRequest() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.list(siteId, UUID.randomUUID(), null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void listDelegatesToSearchWithGivenFilters() {
        Page<PurchaseRequest> page = new PageImpl<>(List.of());
        when(purchaseRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<PurchaseRequest> result =
                service.list(siteId, UUID.randomUUID(), LocalDate.now(), PurchaseRequestStatus.ORCADO, PageRequest.of(0, 20));

        assertThat(result).isSameAs(page);
    }

    @Test
    void listFiltersToPurchaseRequestsVisibleToViewAndApproveMember() {
        // Regression: submitForApproval creates every level's step PENDING up front (see below),
        // so visibility must key off the currently-actionable (lowest-order PENDING) step, not
        // merely "does a step for my function exist" — a client whose step is second-in-line
        // must not see the header while the engineer's first step is still outstanding.
        PurchaseRequest notYetRelevant = purchaseRequest();
        notYetRelevant.markOrcado();
        notYetRelevant.submitForApproval(Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                notYetRelevant.getId(), notYetRelevant.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(new PurchaseRequestApproval(
                        UUID.randomUUID(), notYetRelevant.getId(), notYetRelevant.getCurrentApprovalCycle(), 1,
                        ConstructionFunction.ENGINEER, Instant.now())));

        PurchaseRequest pendingForMe = purchaseRequest();
        pendingForMe.markOrcado();
        pendingForMe.submitForApproval(Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                pendingForMe.getId(), pendingForMe.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(new PurchaseRequestApproval(
                        UUID.randomUUID(), pendingForMe.getId(), pendingForMe.getCurrentApprovalCycle(), 2,
                        ConstructionFunction.CLIENT, Instant.now())));

        PurchaseRequest concluded = purchaseRequest();
        concluded.markOrcado();
        concluded.submitForApproval(Instant.now());
        concluded.approve(Instant.now());
        concluded.complete(Instant.now());
        Page<PurchaseRequest> page = new PageImpl<>(List.of(notYetRelevant, pendingForMe, concluded));
        when(purchaseRequestRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        UUID clientUserId = UUID.randomUUID();
        SiteMembership clientMembership = activeMember(clientUserId, ConstructionFunction.CLIENT);
        when(siteAccessService.requireAccess(siteId, clientUserId)).thenReturn(new SiteAccessContext(false, clientMembership));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);

        Page<PurchaseRequest> result = service.list(siteId, clientUserId, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(pendingForMe, concluded);
    }

    @Test
    void getRejectsViewAndApproveMemberBeforeTheirTurn() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        // Both steps already exist (created up front), but the ENGINEER's is still the one actionable.
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(new PurchaseRequestApproval(
                        UUID.randomUUID(), purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), 1,
                        ConstructionFunction.ENGINEER, Instant.now())));

        UUID clientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, clientUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(clientUserId, ConstructionFunction.CLIENT)));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);

        assertThatThrownBy(() -> service.get(purchaseRequest.getId(), clientUserId))
                .isInstanceOf(PurchaseRequestNotFoundException.class);
    }

    @Test
    void getAllowsViewAndApproveMemberOnceItsTheirTurn() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(new PurchaseRequestApproval(
                        UUID.randomUUID(), purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), 2,
                        ConstructionFunction.CLIENT, Instant.now())));

        UUID clientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, clientUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(clientUserId, ConstructionFunction.CLIENT)));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);

        assertThat(service.get(purchaseRequest.getId(), clientUserId)).isSameAs(purchaseRequest);
    }

    @Test
    void getAllowsViewAndApproveMemberAfterTheyAlreadyDecided() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        when(approvalRepository.existsByPurchaseRequestIdAndCycleNumberAndApproverFunctionAndStatusNot(
                purchaseRequest.getId(), purchaseRequest.getCurrentApprovalCycle(), ConstructionFunction.CLIENT,
                PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(true);

        UUID clientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, clientUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(clientUserId, ConstructionFunction.CLIENT)));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);

        assertThat(service.get(purchaseRequest.getId(), clientUserId)).isSameAs(purchaseRequest);
    }

    @Test
    void getAlwaysAllowsViewAndApproveMemberOnceConcluded() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequest.approve(Instant.now());
        purchaseRequest.complete(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        UUID clientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, clientUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(clientUserId, ConstructionFunction.CLIENT)));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);

        assertThat(service.get(purchaseRequest.getId(), clientUserId)).isSameAs(purchaseRequest);
    }

    @Test
    void submitForApprovalRejectsWhenNotOrcado() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        assertThatThrownBy(() -> service.submitForApproval(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(PurchaseRequestNotOrcadoException.class);
    }

    @Test
    void submitForApprovalRejectsIncompleteSelection() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(item(purchaseRequest.getId())));

        assertThatThrownBy(() -> service.submitForApproval(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(PurchaseRequestSelectionIncompleteException.class);
    }

    @Test
    void submitForApprovalCreatesStepsFromDefaultLevelAndNotifies() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        PurchaseRequestItem selectedItem = item(purchaseRequest.getId());
        selectedItem.select(UUID.randomUUID());
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(selectedItem));
        when(approvalLevelService.getEffectiveLevels(siteId)).thenReturn(List.of(new SitePurchaseRequestApprovalLevel(
                UUID.randomUUID(), siteId, 1, ConstructionFunction.ENGINEER, Instant.now())));

        UUID engineerUserId = UUID.randomUUID();
        when(siteMembershipRepository.findByConstructionSiteIdAndFunction(siteId, ConstructionFunction.ENGINEER))
                .thenReturn(List.of(activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(userRepository.findById(engineerUserId)).thenReturn(Optional.of(
                new AppUser(engineerUserId, "eng@example.com", "Eng", "hash", null, Instant.now(), Instant.now())));

        PurchaseRequest result = service.submitForApproval(purchaseRequest.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.ORCADO);
        assertThat(result.getCurrentApprovalCycle()).isEqualTo(1);
        verify(approvalRepository).save(
                argThat(a -> a.getStepOrder() == 1 && a.getApproverFunction() == ConstructionFunction.ENGINEER));
        verify(eventPublisher).publish(eq(PurchaseRequestApprovalStepPendingEvent.TYPE), any());
    }

    @Test
    void approveStepAdvancesToNextStepWithoutFinalizing() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step1 = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        PurchaseRequestApproval step2 = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 2, ConstructionFunction.CLIENT, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(step1), Optional.of(step2));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(true);
        when(siteMembershipRepository.findByConstructionSiteIdAndFunction(siteId, ConstructionFunction.CLIENT))
                .thenReturn(List.of());

        PurchaseRequest result = service.approveStep(purchaseRequest.getId(), engineerUserId, null);

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.ORCADO);
        assertThat(step1.getStatus()).isEqualTo(PurchaseRequestApprovalStatus.APPROVED);
        verify(orcamentoService, never()).lockAllForPurchaseRequest(any());
    }

    @Test
    void approveStepFinalizesAndLocksOrcamentosWhenLastStep() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval onlyStep = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(onlyStep), Optional.empty());

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(true);

        PurchaseRequest result = service.approveStep(purchaseRequest.getId(), engineerUserId, null);

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.CONFERIDO);
        verify(orcamentoService).lockAllForPurchaseRequest(purchaseRequest.getId());
    }

    @Test
    void approveStepBlocksNonMatchingFunction() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID architectUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, architectUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(architectUserId, ConstructionFunction.ARCHITECT)));

        assertThatThrownBy(() -> service.approveStep(purchaseRequest.getId(), architectUserId, null))
                .isInstanceOf(NotCurrentApprovalStepException.class);
    }

    @Test
    void companyStaffWithNoSiteMembershipCanAlwaysActOnApprovalStep() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(step), Optional.empty());

        UUID staffUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, staffUserId)).thenReturn(new SiteAccessContext(true, null));
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, staffUserId)).thenReturn(Optional.empty());

        PurchaseRequest result = service.approveStep(purchaseRequest.getId(), staffUserId, "ok");

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.CONFERIDO);
        assertThat(step.getDecidedBySiteMembershipId()).isNull();
    }

    @Test
    void companyStaffWithNonMatchingSiteMembershipIsBlockedDespiteBeingStaff() {
        // Regression: a company owner registered as CLIENT on one of their own sites used to bypass
        // the ENGINEER-only step just by being company staff. Holding an active SiteMembership there
        // now always requires the function match, with no staff exception.
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID staffClientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, staffClientUserId)).thenReturn(new SiteAccessContext(true, null));
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, staffClientUserId))
                .thenReturn(Optional.of(activeMember(staffClientUserId, ConstructionFunction.CLIENT)));

        assertThatThrownBy(() -> service.approveStep(purchaseRequest.getId(), staffClientUserId, "ok"))
                .isInstanceOf(NotCurrentApprovalStepException.class);
    }

    @Test
    void companyStaffWithMatchingSiteMembershipApprovesAsThatMembership() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.CLIENT, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING))
                .thenReturn(Optional.of(step), Optional.empty());

        UUID staffClientUserId = UUID.randomUUID();
        SiteMembership clientMembership = activeMember(staffClientUserId, ConstructionFunction.CLIENT);
        when(siteAccessService.requireAccess(siteId, staffClientUserId)).thenReturn(new SiteAccessContext(true, null));
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, staffClientUserId))
                .thenReturn(Optional.of(clientMembership));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(true);

        PurchaseRequest result = service.approveStep(purchaseRequest.getId(), staffClientUserId, "ok");

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.CONFERIDO);
        assertThat(step.getDecidedBySiteMembershipId()).isEqualTo(clientMembership.getId());
    }

    @Test
    void viewOnlyAccessBlocksApprovalEvenOnFunctionMatch() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(false);

        assertThatThrownBy(() -> service.approveStep(purchaseRequest.getId(), engineerUserId, null))
                .isInstanceOf(NotCurrentApprovalStepException.class);
    }

    @Test
    void rejectStepRequiresAReason() {
        assertThatThrownBy(() -> service.rejectStep(UUID.randomUUID(), UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectStepReturnsToOrcadoAndUnlocksOrcamentos() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByPurchaseRequestIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                purchaseRequest.getId(), 1, PurchaseRequestApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST)))
                .thenReturn(true);

        PurchaseRequest result = service.rejectStep(purchaseRequest.getId(), engineerUserId, "Preço muito alto");

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.ORCADO);
        assertThat(result.getLastRejectionReason()).isEqualTo("Preço muito alto");
        assertThat(step.getStatus()).isEqualTo(PurchaseRequestApprovalStatus.REJECTED);
        verify(orcamentoService).unlockAllForPurchaseRequest(purchaseRequest.getId());
    }

    @Test
    void concludeRequiresConferidoStatus() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        assertThatThrownBy(() -> service.conclude(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(PurchaseRequestNotConferidoException.class);
    }

    @Test
    void concludeRejectsMemberWithoutApproveAccess() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.conclude(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void concludeChecksApproveAccessNotStrictManage() {
        // conclude() must gate on "can approve" (MANAGE or VIEW_AND_APPROVE), not requireManage's
        // strict MANAGE-only check — otherwise a VIEW_AND_APPROVE client could approve every step
        // but still be unable to conclude the header they just finished approving.
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequest.approve(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());

        UUID clientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, clientUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(clientUserId, ConstructionFunction.CLIENT)));

        PurchaseRequest result = service.conclude(purchaseRequest.getId(), clientUserId);

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.CONCLUIDO);
        verify(permissionService).requireApprove(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));
        verify(permissionService, never()).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));
    }

    @Test
    void concludeCreatesMaterialsFromSelectedLineItemsAcrossOrcamentos() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequest.approve(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        UUID lineItemAId = UUID.randomUUID();
        UUID lineItemBId = UUID.randomUUID();
        PurchaseRequestItem itemA = item(purchaseRequest.getId());
        itemA.select(lineItemAId);
        PurchaseRequestItem itemB = item(purchaseRequest.getId());
        itemB.select(lineItemBId);
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(itemA, itemB));

        OrcamentoLineItem lineItemA = new OrcamentoLineItem(
                lineItemAId, UUID.randomUUID(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, itemA.getId());
        OrcamentoLineItem lineItemB = new OrcamentoLineItem(
                lineItemBId, UUID.randomUUID(), "Areia", "m3", BigDecimal.ONE, BigDecimal.TEN, itemB.getId());
        when(orcamentoLineItemRepository.findAllById(List.of(lineItemAId, lineItemBId)))
                .thenReturn(List.of(lineItemA, lineItemB));

        PurchaseRequest result = service.conclude(purchaseRequest.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(PurchaseRequestStatus.CONCLUIDO);
        verify(materialService).createFromPurchaseRequestSelections(purchaseRequest, List.of(lineItemA, lineItemB));
    }

    @Test
    void getComparisonPopulatesBothSuppliersAndFlagsSelectedCell() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestItem prItem = item(purchaseRequest.getId());
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of(prItem));

        Orcamento orcamentoA = new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "111", "Fornecedor A", null, null, null,
                null, null, null, purchaseRequest.getId());
        Orcamento orcamentoB = new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "222", "Fornecedor B", null, null, null,
                null, null, null, purchaseRequest.getId());
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequest.getId()))
                .thenReturn(List.of(orcamentoA, orcamentoB));

        OrcamentoLineItem quoteA = new OrcamentoLineItem(
                UUID.randomUUID(), orcamentoA.getId(), "Cimento", "Saco", new BigDecimal("10"), new BigDecimal("30"),
                prItem.getId());
        prItem.select(quoteA.getId());
        OrcamentoLineItem quoteB = new OrcamentoLineItem(
                UUID.randomUUID(), orcamentoB.getId(), "Cimento", "Saco", new BigDecimal("10"), new BigDecimal("35"),
                prItem.getId());
        when(orcamentoLineItemRepository.findByOrcamentoIdInAndSourcePurchaseRequestItemId(
                List.of(orcamentoA.getId(), orcamentoB.getId()), prItem.getId()))
                .thenReturn(List.of(quoteA, quoteB));

        PurchaseRequestComparisonResponse comparison = service.getComparison(purchaseRequest.getId(), UUID.randomUUID());

        assertThat(comparison.columns()).hasSize(2);
        assertThat(comparison.rows()).hasSize(1);
        var row = comparison.rows().get(0);
        assertThat(row.cells()).hasSize(2);
        assertThat(row.cells()).anySatisfy(cell -> {
            if (cell.orcamentoId().equals(orcamentoA.getId())) {
                assertThat(cell.selected()).isTrue();
                assertThat(cell.unitPrice()).isEqualByComparingTo("30");
            }
        });
        assertThat(row.cells()).anySatisfy(cell -> {
            if (cell.orcamentoId().equals(orcamentoB.getId())) {
                assertThat(cell.selected()).isFalse();
                assertThat(cell.unitPrice()).isEqualByComparingTo("35");
            }
        });
    }

    @Test
    void getComparisonLeavesCellEmptyWhenOrcamentoDidNotQuoteTheItem() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        PurchaseRequestItem prItem = item(purchaseRequest.getId());
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of(prItem));

        Orcamento orcamentoA = new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "111", "Fornecedor A", null, null, null,
                null, null, null, purchaseRequest.getId());
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequest.getId())).thenReturn(List.of(orcamentoA));
        when(orcamentoLineItemRepository.findByOrcamentoIdInAndSourcePurchaseRequestItemId(
                List.of(orcamentoA.getId()), prItem.getId())).thenReturn(List.of());

        PurchaseRequestComparisonResponse comparison = service.getComparison(purchaseRequest.getId(), UUID.randomUUID());

        assertThat(comparison.rows().get(0).cells()).isEmpty();
    }

    @Test
    void deleteRemovesIniciadoHeaderAndItsItems() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        List<PurchaseRequestItem> items = List.of(item(purchaseRequest.getId()));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(items);

        service.delete(purchaseRequest.getId(), UUID.randomUUID());

        verify(itemRepository).deleteAll(items);
        verify(purchaseRequestRepository).delete(purchaseRequest);
    }

    @Test
    void deleteRejectsWhenConcluido() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequest.approve(Instant.now());
        purchaseRequest.complete(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        assertThatThrownBy(() -> service.delete(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(PurchaseRequestNotDeletableException.class);
        verify(purchaseRequestRepository, never()).delete(any(PurchaseRequest.class));
    }

    @Test
    void deleteWhileOrcadoCascadesLinkedOrcamentosAndApprovalHistory() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        Orcamento orcamento = new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "111", "Fornecedor A", null, null, null,
                null, null, null, purchaseRequest.getId());
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequest.getId())).thenReturn(List.of(orcamento));
        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, UUID.randomUUID());
        when(orcamentoLineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of(lineItem));

        PurchaseRequestItem item = item(purchaseRequest.getId());
        item.convertTo(orcamento.getId(), Instant.now());
        item.select(lineItem.getId());
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of(item));

        PurchaseRequestApproval approval = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(purchaseRequest.getId()))
                .thenReturn(List.of(approval));

        service.delete(purchaseRequest.getId(), UUID.randomUUID());

        // The item's own FK columns must be cleared before its referenced Orcamento/line item
        // are deleted, otherwise the delete would violate that FK against a real database.
        assertThat(item.getConvertedToOrcamentoId()).isNull();
        assertThat(item.getSelectedOrcamentoLineItemId()).isNull();
        InOrder order = inOrder(itemRepository, orcamentoLineItemRepository, orcamentoRepository, purchaseRequestRepository);
        order.verify(itemRepository).saveAll(List.of(item));
        order.verify(orcamentoLineItemRepository).deleteAll(List.of(lineItem));
        order.verify(orcamentoRepository).deleteAll(List.of(orcamento));
        order.verify(itemRepository).deleteAll(List.of(item));
        order.verify(purchaseRequestRepository).delete(purchaseRequest);
        verify(approvalRepository).deleteAll(List.of(approval));
    }

    @Test
    void deleteRejectsMemberWithoutManageAccess() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.delete(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(purchaseRequestRepository, never()).delete(any(PurchaseRequest.class));
    }

    @Test
    void deleteAlsoDeletesAttachedInvoiceStorageObjectsAndRows() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        PurchaseRequestInvoice invoice = new PurchaseRequestInvoice(
                UUID.randomUUID(), purchaseRequest.getId(), "some/key.pdf", "application/pdf", "nf.pdf",
                UUID.randomUUID(), Instant.now());
        when(invoiceRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(invoice));

        service.delete(purchaseRequest.getId(), UUID.randomUUID());

        verify(storageService).deleteObject("some/key.pdf");
        verify(invoiceRepository).deleteAll(List.of(invoice));
    }

    @Test
    void uploadInvoicePersistsEntityAndStoresFile() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "nf-123.pdf", "application/pdf", "conteudo".getBytes());

        PurchaseRequestInvoice result = service.uploadInvoice(purchaseRequest.getId(), UUID.randomUUID(), file);

        assertThat(result.getPurchaseRequestId()).isEqualTo(purchaseRequest.getId());
        assertThat(result.getOriginalName()).isEqualTo("nf-123.pdf");
        assertThat(result.getContentType()).isEqualTo("application/pdf");
        verify(storageService).putObject(eq(result.getStorageKey()), any(), eq("application/pdf"));
    }

    @Test
    void uploadInvoiceAllowedRegardlessOfHeaderStatus() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequest.approve(Instant.now());
        purchaseRequest.complete(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "nf.pdf", "application/pdf", "conteudo".getBytes());

        PurchaseRequestInvoice result = service.uploadInvoice(purchaseRequest.getId(), UUID.randomUUID(), file);

        assertThat(result.getOriginalName()).isEqualTo("nf.pdf");
    }

    @Test
    void uploadInvoiceRejectsDisallowedExtension() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        MockMultipartFile file = new MockMultipartFile("file", "clipe.mp4", "video/mp4", "conteudo".getBytes());

        assertThatThrownBy(() -> service.uploadInvoice(purchaseRequest.getId(), UUID.randomUUID(), file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void uploadInvoiceRejectsEmptyFile() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        MockMultipartFile file = new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.uploadInvoice(purchaseRequest.getId(), UUID.randomUUID(), file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void listInvoicesRejectsMemberHiddenFromPurchaseRequest() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.listInvoices(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void getInvoiceContentThrowsWhenInvoiceMissing() {
        UUID invoiceId = UUID.randomUUID();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInvoiceContent(invoiceId, UUID.randomUUID()))
                .isInstanceOf(PurchaseRequestInvoiceNotFoundException.class);
    }

    @Test
    void getInvoiceContentReturnsStoredBytesAndMetadata() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        PurchaseRequestInvoice invoice = new PurchaseRequestInvoice(
                UUID.randomUUID(), purchaseRequest.getId(), "some/key.pdf", "application/pdf", "nf.pdf",
                UUID.randomUUID(), Instant.now());
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(storageService.getObject("some/key.pdf")).thenReturn("conteudo".getBytes());

        var content = service.getInvoiceContent(invoice.getId(), UUID.randomUUID());

        assertThat(content.originalName()).isEqualTo("nf.pdf");
        assertThat(content.contentType()).isEqualTo("application/pdf");
        assertThat(content.bytes()).isEqualTo("conteudo".getBytes());
    }

    @Test
    void deleteInvoiceRemovesStorageObjectAndRow() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        PurchaseRequestInvoice invoice = new PurchaseRequestInvoice(
                UUID.randomUUID(), purchaseRequest.getId(), "some/key.pdf", "application/pdf", "nf.pdf",
                UUID.randomUUID(), Instant.now());
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        service.deleteInvoice(invoice.getId(), UUID.randomUUID());

        verify(storageService).deleteObject("some/key.pdf");
        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void deleteInvoiceRejectsMemberWithoutManageAccess() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        PurchaseRequestInvoice invoice = new PurchaseRequestInvoice(
                UUID.randomUUID(), purchaseRequest.getId(), "some/key.pdf", "application/pdf", "nf.pdf",
                UUID.randomUUID(), Instant.now());
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.deleteInvoice(invoice.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void resolveDisplayNamePrefersDisplayNameOverEmail() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "user@example.com", "Nome Completo", null, null, Instant.now(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(service.resolveDisplayName(userId)).isEqualTo("Nome Completo");
    }

    @Test
    void resolveDisplayNameFallsBackToEmailWhenNoDisplayName() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "user@example.com", null, null, null, Instant.now(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(service.resolveDisplayName(userId)).isEqualTo("user@example.com");
    }

    @Test
    void resolveDisplayNameReturnsNullWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(service.resolveDisplayName(userId)).isNull();
    }

    @Test
    void resolveDisplayNamesBatchResolvesEveryRequestedUser() {
        UUID userIdA = UUID.randomUUID();
        UUID userIdB = UUID.randomUUID();
        AppUser userA = new AppUser(userIdA, "a@example.com", "Fulano A", null, null, Instant.now(), Instant.now());
        AppUser userB = new AppUser(userIdB, "b@example.com", null, null, null, Instant.now(), Instant.now());
        when(userRepository.findAllById(List.of(userIdA, userIdB))).thenReturn(List.of(userA, userB));

        var result = service.resolveDisplayNames(List.of(userIdA, userIdB));

        assertThat(result).containsEntry(userIdA, "Fulano A").containsEntry(userIdB, "b@example.com");
    }
}
