package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.PurchaseRequestItemCreationRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.repository.ConstructionSiteRepository;
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

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestItemRepository itemRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private PurchaseRequestService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new PurchaseRequestService(
                purchaseRequestRepository, itemRepository, siteRepository, siteAccessService, permissionService);

        siteId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(purchaseRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    private ConstructionSite site(UUID id) {
        return new ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private List<PurchaseRequestItemCreationRequest> oneItem() {
        return List.of(new PurchaseRequestItemCreationRequest("Cimento", "Saco", new BigDecimal("10"), "saco"));
    }

    @Test
    void firstPurchaseRequestOfTheDayGetsSequenceOne() {
        when(purchaseRequestRepository.countByConstructionSiteIdAndCreatedAtBetween(eq(siteId), any(), any())).thenReturn(0L);

        PurchaseRequest result = service.create(siteId, UUID.randomUUID(), oneItem());

        assertThat(result.getName()).matches("Pedido \\d{2}/\\d{2}/\\d{4} #1");
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

        verify(itemRepository).save(org.mockito.ArgumentMatchers.argThat(
                item -> item.getPurchaseRequestId().equals(result.getId()) && item.getName().equals("Cimento")));
    }
}
