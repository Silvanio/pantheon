package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.exception.OrcamentoNotLinkedToPurchaseRequestException;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class PurchaseRequestPdfServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestItemRepository itemRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoLineItemRepository lineItemRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private PurchaseRequestPdfService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new PurchaseRequestPdfService(
                purchaseRequestRepository, itemRepository, orcamentoRepository, lineItemRepository, siteAccessService,
                permissionService);
        siteId = UUID.randomUUID();
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    private PurchaseRequest purchaseRequest() {
        return new PurchaseRequest(UUID.randomUUID(), siteId, "Pedido 07/09/2026 #1", UUID.randomUUID(), Instant.now());
    }

    private Orcamento orcamento(UUID sourcePurchaseRequestId) {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor Teste",
                null, null, null, null, null, null, sourcePurchaseRequestId);
    }

    @Test
    void generateRejectsOrcamentoNotLinkedToPurchaseRequest() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        Orcamento unrelated = orcamento(UUID.randomUUID());
        when(orcamentoRepository.findById(unrelated.getId())).thenReturn(Optional.of(unrelated));

        assertThatThrownBy(() -> service.generate(purchaseRequest.getId(), unrelated.getId(), UUID.randomUUID()))
                .isInstanceOf(OrcamentoNotLinkedToPurchaseRequestException.class);
    }

    @Test
    void generateProducesAValidNonEmptyPdfContainingOnlySelectedItems() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        Orcamento orcamento = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        PurchaseRequestItem selectedItem = new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequest.getId(), "Cimento", "Saco", BigDecimal.TEN, "saco",
                UUID.randomUUID(), Instant.now());
        OrcamentoLineItem selectedLineItem = new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, selectedItem.getId());
        selectedItem.select(selectedLineItem.getId());

        // Never selected for any supplier: must not appear in the rendered PDF.
        PurchaseRequestItem unselectedItem = new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequest.getId(), "Areia", "m3", BigDecimal.ONE, "m3",
                UUID.randomUUID(), Instant.now());

        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(selectedItem, unselectedItem));
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of(selectedLineItem));

        byte[] pdf = service.generate(purchaseRequest.getId(), orcamento.getId(), UUID.randomUUID());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
