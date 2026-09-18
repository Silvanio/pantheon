package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.pantheon.service.entity.PurchaseRequestItemStatus;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.OrcamentoNotDeletableException;
import com.pantheon.service.exception.OrcamentoNotDraftException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoLineItemRepository lineItemRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestItemRepository purchaseRequestItemRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private FornecedorService fornecedorService;

    private OrcamentoService service;

    private UUID siteId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        service = new OrcamentoService(
                orcamentoRepository, lineItemRepository, siteRepository, purchaseRequestRepository,
                purchaseRequestItemRepository, siteAccessService, permissionService, fornecedorService);

        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(orcamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(fornecedorService.findOrCreate(eq(companyId), any(), any())).thenAnswer(inv -> {
            FornecedorRequest req = inv.getArgument(2);
            return new Fornecedor(
                    UUID.randomUUID(), companyId, req.cnpj(), req.name(), req.address(), req.contactName(),
                    req.contactPhone(), UUID.randomUUID(), Instant.now());
        });
    }

    private ConstructionSite site(UUID id) {
        return new ConstructionSite(
                id, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private FornecedorRequest fornecedorRequest() {
        return new FornecedorRequest("12345678000199", "Fornecedor Teste", null, null, null);
    }

    private Orcamento orcamento(UUID sourcePurchaseRequestId) {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor Teste",
                null, null, null, null, sourcePurchaseRequestId);
    }

    private PurchaseRequestItem purchaseRequestItem(UUID purchaseRequestId) {
        return new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequestId, "Cimento", "Saco", new BigDecimal("10"), "saco",
                UUID.randomUUID(), Instant.now());
    }

    private PurchaseRequest purchaseRequest(PurchaseRequestStatus status) {
        PurchaseRequest purchaseRequest = new PurchaseRequest(
                UUID.randomUUID(), siteId, "Pedido 07/09/2026 #1", UUID.randomUUID(), Instant.now());
        if (status == PurchaseRequestStatus.ORCADO) {
            purchaseRequest.markOrcado();
        }
        return purchaseRequest;
    }

    @Test
    void createPersistsDraftOrcamentoWithLineItems() {
        UUID actingUserId = UUID.randomUUID();

        Orcamento result = service.create(siteId, actingUserId, List.of(
                new OrcamentoLineItemRequest("Cimento", "Saco", new BigDecimal("50"), new BigDecimal("32.5"))),
                fornecedorRequest());

        assertThat(result.getConstructionSiteId()).isEqualTo(siteId);
        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.DRAFT);
        assertThat(result.getFornecedorCnpj()).isEqualTo("12345678000199");
        assertThat(result.getFornecedorNome()).isEqualTo("Fornecedor Teste");
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.ORCAMENTO_MANAGE));
        verify(lineItemRepository).save(argThat(
                item -> item.getName().equals("Cimento") && item.getUnitPrice().equals(new BigDecimal("32.5"))));
    }

    @Test
    void createReusesExistingFornecedorForSameCnpj() {
        UUID actingUserId = UUID.randomUUID();
        Fornecedor existing = new Fornecedor(
                UUID.randomUUID(), companyId, "12345678000199", "Fornecedor Existente", null, null, null,
                UUID.randomUUID(), Instant.now());
        when(fornecedorService.findOrCreate(eq(companyId), any(), any())).thenReturn(existing);

        Orcamento result = service.create(siteId, actingUserId, List.of(), fornecedorRequest());

        assertThat(result.getFornecedorNome()).isEqualTo("Fornecedor Existente");
    }

    @Test
    void addLineItemRejectsWhenOrcamentoIsLocked() {
        Orcamento locked = orcamento(null);
        locked.lock();
        when(orcamentoRepository.findById(locked.getId())).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.addLineItem(
                locked.getId(), UUID.randomUUID(), new OrcamentoLineItemRequest("Cimento", null, BigDecimal.ONE, null)))
                .isInstanceOf(OrcamentoNotDraftException.class);
    }

    @Test
    void createFromPurchaseRequestItemsMovesIniciadoHeaderToOrcado() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequest purchaseRequest = purchaseRequest(PurchaseRequestStatus.INICIADO);
        PurchaseRequestItem item = purchaseRequestItem(purchaseRequest.getId());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createFromPurchaseRequestItems(
                siteId, actingUserId, List.of(item), purchaseRequest.getId(), fornecedorRequest());

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.ORCADO);
        verify(purchaseRequestRepository).save(purchaseRequest);
    }

    @Test
    void createFromPurchaseRequestItemsLeavesAlreadyOrcadoHeaderUnchanged() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequest purchaseRequest = purchaseRequest(PurchaseRequestStatus.ORCADO);
        PurchaseRequestItem item = purchaseRequestItem(purchaseRequest.getId());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        service.createFromPurchaseRequestItems(
                siteId, actingUserId, List.of(item), purchaseRequest.getId(), fornecedorRequest());

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.ORCADO);
        verify(purchaseRequestRepository, never()).save(purchaseRequest);
    }

    @Test
    void lockAllForPurchaseRequestLocksEveryLinkedOrcamento() {
        UUID purchaseRequestId = UUID.randomUUID();
        Orcamento a = orcamento(purchaseRequestId);
        Orcamento b = orcamento(purchaseRequestId);
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId)).thenReturn(List.of(a, b));

        service.lockAllForPurchaseRequest(purchaseRequestId);

        assertThat(a.getStatus()).isEqualTo(OrcamentoStatus.LOCKED);
        assertThat(b.getStatus()).isEqualTo(OrcamentoStatus.LOCKED);
        verify(orcamentoRepository, times(2)).save(any());
    }

    @Test
    void unlockAllForPurchaseRequestUnlocksEveryLinkedOrcamento() {
        UUID purchaseRequestId = UUID.randomUUID();
        Orcamento a = orcamento(purchaseRequestId);
        a.lock();
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequestId)).thenReturn(List.of(a));

        service.unlockAllForPurchaseRequest(purchaseRequestId);

        assertThat(a.getStatus()).isEqualTo(OrcamentoStatus.DRAFT);
    }

    @Test
    void isSelectedReturnsTrueWhenLineItemIsTheSourceItemsSelection() {
        UUID sourceItemId = UUID.randomUUID();
        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                UUID.randomUUID(), UUID.randomUUID(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, sourceItemId);
        PurchaseRequestItem sourceItem = purchaseRequestItem(UUID.randomUUID());
        sourceItem.select(lineItem.getId());
        when(purchaseRequestItemRepository.findById(sourceItemId)).thenReturn(Optional.of(sourceItem));

        assertThat(service.isSelected(lineItem)).isTrue();
    }

    @Test
    void isSelectedReturnsFalseWhenLineItemHasNoSourceItem() {
        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                UUID.randomUUID(), UUID.randomUUID(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, null);

        assertThat(service.isSelected(lineItem)).isFalse();
    }

    @Test
    void listRejectsMemberHiddenFromOrcamentoManage() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.ORCAMENTO_MANAGE))
                .when(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.ORCAMENTO_MANAGE));

        assertThatThrownBy(() -> service.list(siteId, UUID.randomUUID(), null, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void listDelegatesToSearchWithGivenFilters() {
        UUID purchaseRequestId = UUID.randomUUID();
        Page<Orcamento> page = new PageImpl<>(List.of());
        when(orcamentoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<Orcamento> result =
                service.list(siteId, UUID.randomUUID(), LocalDate.now(), purchaseRequestId, "acme", PageRequest.of(0, 20));

        assertThat(result).isSameAs(page);
    }

    @Test
    void listWithNoFiltersPassesNullsToSearch() {
        when(orcamentoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.list(siteId, UUID.randomUUID(), null, null, null, PageRequest.of(0, 20));

        verify(orcamentoRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void deleteRemovesStandaloneDraftOrcamentoAndItsLineItems() {
        Orcamento standalone = orcamento(null);
        List<OrcamentoLineItem> lineItems = List.of(new OrcamentoLineItem(
                UUID.randomUUID(), standalone.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, null));
        when(orcamentoRepository.findById(standalone.getId())).thenReturn(Optional.of(standalone));
        when(lineItemRepository.findByOrcamentoId(standalone.getId())).thenReturn(lineItems);

        service.delete(standalone.getId(), UUID.randomUUID());

        verify(lineItemRepository).deleteAll(lineItems);
        verify(orcamentoRepository).delete(standalone);
        verify(purchaseRequestItemRepository, never()).save(any());
    }

    @Test
    void deleteRejectsLockedOrcamento() {
        Orcamento locked = orcamento(null);
        locked.lock();
        when(orcamentoRepository.findById(locked.getId())).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.delete(locked.getId(), UUID.randomUUID()))
                .isInstanceOf(OrcamentoNotDeletableException.class);
        verify(orcamentoRepository, never()).delete(any(Orcamento.class));
    }

    @Test
    void deleteRejectsMemberWithoutManageAccess() {
        Orcamento standalone = orcamento(null);
        when(orcamentoRepository.findById(standalone.getId())).thenReturn(Optional.of(standalone));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.ORCAMENTO_MANAGE))
                .when(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.ORCAMENTO_MANAGE));

        assertThatThrownBy(() -> service.delete(standalone.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(orcamentoRepository, never()).delete(any(Orcamento.class));
    }

    @Test
    void deleteRevertsConvertedItemsToPendingAndClearsStaleSelection() {
        PurchaseRequest purchaseRequest = purchaseRequest(PurchaseRequestStatus.ORCADO);
        Orcamento converted = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(converted.getId())).thenReturn(Optional.of(converted));

        PurchaseRequestItem convertedItem = purchaseRequestItem(purchaseRequest.getId());
        UUID lineItemId = UUID.randomUUID();
        convertedItem.convertTo(converted.getId(), Instant.now());
        convertedItem.select(lineItemId);
        when(purchaseRequestItemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(convertedItem));

        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                lineItemId, converted.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, convertedItem.getId());
        when(lineItemRepository.findByOrcamentoId(converted.getId())).thenReturn(List.of(lineItem));
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequest.getId())).thenReturn(List.of());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        service.delete(converted.getId(), UUID.randomUUID());

        assertThat(convertedItem.getStatus()).isEqualTo(PurchaseRequestItemStatus.PENDING);
        assertThat(convertedItem.getConvertedToOrcamentoId()).isNull();
        assertThat(convertedItem.getSelectedOrcamentoLineItemId()).isNull();
        verify(purchaseRequestItemRepository).save(convertedItem);
    }

    @Test
    void deleteRevertsHeaderToIniciadoWhenLastLinkedOrcamentoIsRemoved() {
        PurchaseRequest purchaseRequest = purchaseRequest(PurchaseRequestStatus.ORCADO);
        Orcamento onlyLinked = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(onlyLinked.getId())).thenReturn(Optional.of(onlyLinked));
        when(purchaseRequestItemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(onlyLinked.getId())).thenReturn(List.of());
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequest.getId())).thenReturn(List.of());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        service.delete(onlyLinked.getId(), UUID.randomUUID());

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.INICIADO);
        verify(purchaseRequestRepository).save(purchaseRequest);
    }

    @Test
    void deleteLeavesHeaderOrcadoWhenAnotherOrcamentoIsStillLinked() {
        PurchaseRequest purchaseRequest = purchaseRequest(PurchaseRequestStatus.ORCADO);
        Orcamento first = orcamento(purchaseRequest.getId());
        Orcamento second = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(first.getId())).thenReturn(Optional.of(first));
        when(purchaseRequestItemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(first.getId())).thenReturn(List.of());
        when(orcamentoRepository.findBySourcePurchaseRequestId(purchaseRequest.getId())).thenReturn(List.of(second));

        service.delete(first.getId(), UUID.randomUUID());

        assertThat(purchaseRequest.getStatus()).isEqualTo(PurchaseRequestStatus.ORCADO);
        verify(purchaseRequestRepository, never()).findById(purchaseRequest.getId());
    }
}
