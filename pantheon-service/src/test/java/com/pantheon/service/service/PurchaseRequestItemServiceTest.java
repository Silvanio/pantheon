package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.pantheon.service.exception.SelectionNotAllowedException;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestItemServiceTest {

    @Mock
    private PurchaseRequestItemRepository itemRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoLineItemRepository orcamentoLineItemRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private OrcamentoService orcamentoService;

    private PurchaseRequestItemService service;

    private UUID siteId;
    private UUID purchaseRequestId;
    private PurchaseRequest purchaseRequest;

    @BeforeEach
    void setUp() {
        service = new PurchaseRequestItemService(
                itemRepository, purchaseRequestRepository, orcamentoRepository, orcamentoLineItemRepository,
                siteAccessService, permissionService, orcamentoService);

        siteId = UUID.randomUUID();
        purchaseRequestId = UUID.randomUUID();
        purchaseRequest = new PurchaseRequest(purchaseRequestId, siteId, "Pedido 07/09/2026 #1", UUID.randomUUID(), Instant.now());
        lenient().when(purchaseRequestRepository.findById(purchaseRequestId)).thenReturn(Optional.of(purchaseRequest));
        lenient().when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    private PurchaseRequestItem item(PurchaseRequestItemStatus status) {
        return item(purchaseRequestId, status);
    }

    private PurchaseRequestItem item(UUID ownerPurchaseRequestId, PurchaseRequestItemStatus status) {
        PurchaseRequestItem item = new PurchaseRequestItem(
                UUID.randomUUID(), siteId, ownerPurchaseRequestId, "Cimento", "Saco", new BigDecimal("10"), "saco",
                UUID.randomUUID(), Instant.now());
        if (status == PurchaseRequestItemStatus.CONVERTED) {
            item.convertTo(UUID.randomUUID(), Instant.now());
        }
        return item;
    }

    private FornecedorRequest fornecedorRequest() {
        return new FornecedorRequest("12345678000199", "Fornecedor Teste", null, null, null, null, null);
    }

    private Orcamento orcamento(UUID sourcePurchaseRequestId) {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor Teste",
                null, null, null, null, null, null, sourcePurchaseRequestId);
    }

    @Test
    void listFiltersByStatusWhenProvided() {
        UUID actingUserId = UUID.randomUUID();
        when(itemRepository.findByPurchaseRequestIdAndStatusOrderByCreatedAtDesc(purchaseRequestId, PurchaseRequestItemStatus.PENDING))
                .thenReturn(List.of(item(PurchaseRequestItemStatus.PENDING)));

        List<PurchaseRequestItem> result = service.list(purchaseRequestId, actingUserId, PurchaseRequestItemStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(siteAccessService).requireAccess(siteId, actingUserId);
    }

    @Test
    void convertToOrcamentoMarksItemsConvertedAndCreatesOrcamento() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem pending = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        Orcamento orcamento = new Orcamento(
                UUID.randomUUID(), siteId, actingUserId, Instant.now(), "12345678000199", "Fornecedor Teste", null,
                null, null, null, null, null, purchaseRequestId);
        when(orcamentoService.createFromPurchaseRequestItems(
                siteId, actingUserId, List.of(pending), purchaseRequestId, fornecedorRequest()))
                .thenReturn(orcamento);

        Orcamento result = service.convertToOrcamento(
                purchaseRequestId, actingUserId, List.of(pending.getId()), fornecedorRequest());

        assertThat(result).isEqualTo(orcamento);
        assertThat(pending.getStatus()).isEqualTo(PurchaseRequestItemStatus.CONVERTED);
        assertThat(pending.getConvertedToOrcamentoId()).isEqualTo(orcamento.getId());
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));
    }

    @Test
    void convertToOrcamentoAllowsReconvertingAnAlreadyConvertedItemForASecondQuote() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem converted = item(PurchaseRequestItemStatus.CONVERTED);
        UUID firstOrcamentoId = converted.getConvertedToOrcamentoId();
        when(itemRepository.findById(converted.getId())).thenReturn(Optional.of(converted));

        Orcamento secondOrcamento = new Orcamento(
                UUID.randomUUID(), siteId, actingUserId, Instant.now(), "12345678000199", "Fornecedor B", null,
                null, null, null, null, null, purchaseRequestId);
        when(orcamentoService.createFromPurchaseRequestItems(
                siteId, actingUserId, List.of(converted), purchaseRequestId, fornecedorRequest()))
                .thenReturn(secondOrcamento);

        Orcamento result = service.convertToOrcamento(
                purchaseRequestId, actingUserId, List.of(converted.getId()), fornecedorRequest());

        assertThat(result).isEqualTo(secondOrcamento);
        assertThat(converted.getStatus()).isEqualTo(PurchaseRequestItemStatus.CONVERTED);
        assertThat(converted.getConvertedToOrcamentoId()).isEqualTo(firstOrcamentoId);
        verify(itemRepository, never()).save(converted);
    }

    @Test
    void convertToOrcamentoRejectsSelectionSpanningMultipleHeaders() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem fromThisHeader = item(purchaseRequestId, PurchaseRequestItemStatus.PENDING);
        PurchaseRequestItem fromOtherHeader = item(UUID.randomUUID(), PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(fromThisHeader.getId())).thenReturn(Optional.of(fromThisHeader));
        when(itemRepository.findById(fromOtherHeader.getId())).thenReturn(Optional.of(fromOtherHeader));

        assertThatThrownBy(() -> service.convertToOrcamento(
                purchaseRequestId, actingUserId, List.of(fromThisHeader.getId(), fromOtherHeader.getId()),
                fornecedorRequest()))
                .isInstanceOf(ItemsSpanMultiplePurchaseRequestsException.class);
    }

    @Test
    void sameHeaderCanSpawnASecondOrcamentoFromRemainingItems() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem firstBatch = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(firstBatch.getId())).thenReturn(Optional.of(firstBatch));
        Orcamento firstOrcamento = new Orcamento(
                UUID.randomUUID(), siteId, actingUserId, Instant.now(), "12345678000199", "Fornecedor Teste", null,
                null, null, null, null, null, purchaseRequestId);
        when(orcamentoService.createFromPurchaseRequestItems(
                siteId, actingUserId, List.of(firstBatch), purchaseRequestId, fornecedorRequest()))
                .thenReturn(firstOrcamento);

        service.convertToOrcamento(purchaseRequestId, actingUserId, List.of(firstBatch.getId()), fornecedorRequest());

        PurchaseRequestItem secondBatch = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(secondBatch.getId())).thenReturn(Optional.of(secondBatch));
        Orcamento secondOrcamento = new Orcamento(
                UUID.randomUUID(), siteId, actingUserId, Instant.now(), "12345678000199", "Fornecedor Teste", null,
                null, null, null, null, null, purchaseRequestId);
        when(orcamentoService.createFromPurchaseRequestItems(
                siteId, actingUserId, List.of(secondBatch), purchaseRequestId, fornecedorRequest()))
                .thenReturn(secondOrcamento);

        Orcamento result = service.convertToOrcamento(
                purchaseRequestId, actingUserId, List.of(secondBatch.getId()), fornecedorRequest());

        assertThat(result).isEqualTo(secondOrcamento);
        assertThat(result).isNotEqualTo(firstOrcamento);
    }

    @Test
    void setSelectionBlockedWhenHeaderIsNotOrcado() {
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem item = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.setSelection(item.getId(), actingUserId, UUID.randomUUID()))
                .isInstanceOf(SelectionNotAllowedException.class);
    }

    @Test
    void setSelectionHappyPathRecordsSelection() {
        purchaseRequest.markOrcado();
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem item = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        Orcamento orcamento = orcamento(purchaseRequestId);
        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, item.getId());
        when(orcamentoLineItemRepository.findById(lineItem.getId())).thenReturn(Optional.of(lineItem));
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        PurchaseRequestItem result = service.setSelection(item.getId(), actingUserId, lineItem.getId());

        assertThat(result.getSelectedOrcamentoLineItemId()).isEqualTo(lineItem.getId());
    }

    @Test
    void setSelectionClearsWhenTargetIsNull() {
        purchaseRequest.markOrcado();
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem item = item(PurchaseRequestItemStatus.PENDING);
        item.select(UUID.randomUUID());
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        PurchaseRequestItem result = service.setSelection(item.getId(), actingUserId, null);

        assertThat(result.getSelectedOrcamentoLineItemId()).isNull();
    }

    @Test
    void setSelectionRejectsLineItemFromUnrelatedOrcamento() {
        purchaseRequest.markOrcado();
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem item = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        Orcamento unrelatedOrcamento = orcamento(UUID.randomUUID());
        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                UUID.randomUUID(), unrelatedOrcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, item.getId());
        when(orcamentoLineItemRepository.findById(lineItem.getId())).thenReturn(Optional.of(lineItem));
        when(orcamentoRepository.findById(unrelatedOrcamento.getId())).thenReturn(Optional.of(unrelatedOrcamento));

        assertThatThrownBy(() -> service.setSelection(item.getId(), actingUserId, lineItem.getId()))
                .isInstanceOf(OrcamentoLineItemNotLinkedException.class);
    }

    @Test
    void setSelectionRejectsLineItemQuotedAgainstADifferentItem() {
        purchaseRequest.markOrcado();
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem item = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        Orcamento orcamento = orcamento(purchaseRequestId);
        OrcamentoLineItem lineItem = new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, UUID.randomUUID());
        when(orcamentoLineItemRepository.findById(lineItem.getId())).thenReturn(Optional.of(lineItem));
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        assertThatThrownBy(() -> service.setSelection(item.getId(), actingUserId, lineItem.getId()))
                .isInstanceOf(OrcamentoLineItemNotLinkedException.class);
    }

    @Test
    void setSelectionAllowsUntracedLineItemAgainstAnyItemOfTheHeader() {
        purchaseRequest.markOrcado();
        UUID actingUserId = UUID.randomUUID();
        PurchaseRequestItem item = item(PurchaseRequestItemStatus.PENDING);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        Orcamento orcamento = orcamento(purchaseRequestId);
        OrcamentoLineItem untracedLineItem = new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Item ad hoc", null, BigDecimal.ONE, BigDecimal.ONE, null);
        when(orcamentoLineItemRepository.findById(untracedLineItem.getId())).thenReturn(Optional.of(untracedLineItem));
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        PurchaseRequestItem result = service.setSelection(item.getId(), actingUserId, untracedLineItem.getId());

        assertThat(result.getSelectedOrcamentoLineItemId()).isEqualTo(untracedLineItem.getId());
    }
}
