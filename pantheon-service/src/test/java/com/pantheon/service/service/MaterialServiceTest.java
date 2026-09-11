package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.MaterialDeliveryStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.exception.MaterialDeliveryStatusOrderException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialDeliveryPhotoRepository;
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.storage.StorageService;
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
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialDeliveryPhotoRepository photoRepository;

    @Mock
    private OrcamentoLineItemRepository lineItemRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private StorageService storageService;

    private MaterialService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new MaterialService(
                materialRepository, photoRepository, lineItemRepository, siteRepository, siteAccessService,
                permissionService, storageService);

        siteId = UUID.randomUUID();
        lenient().when(materialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
    }

    private Material material(UUID lineItemId) {
        return new Material(UUID.randomUUID(), siteId, lineItemId, "Cimento", "Saco", new BigDecimal("10"), Instant.now());
    }

    @Test
    void createFromOrcamentoCreatesOneMaterialPerLineItem() {
        Orcamento orcamento = new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor Teste",
                null, null, null, null, null);
        List<OrcamentoLineItem> items = List.of(
                new OrcamentoLineItem(UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, null),
                new OrcamentoLineItem(UUID.randomUUID(), orcamento.getId(), "Areia", "m3", BigDecimal.ONE, BigDecimal.ONE, null));

        service.createFromOrcamento(orcamento, items);

        verify(materialRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void markDeliveredRequiresAwaitingDeliveryStatus() {
        Material delivered = material(UUID.randomUUID());
        delivered.markDelivered(UUID.randomUUID(), Instant.now());
        when(materialRepository.findById(delivered.getId())).thenReturn(Optional.of(delivered));

        assertThatThrownBy(() -> service.markDelivered(delivered.getId(), UUID.randomUUID()))
                .isInstanceOf(MaterialDeliveryStatusOrderException.class);
    }

    @Test
    void markDeliveredTransitionsAwaitingToDelivered() {
        Material awaiting = material(UUID.randomUUID());
        when(materialRepository.findById(awaiting.getId())).thenReturn(Optional.of(awaiting));

        Material result = service.markDelivered(awaiting.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(MaterialDeliveryStatus.DELIVERED);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.ORCAMENTO_MANAGE));
    }

    @Test
    void markCheckedCannotSkipDeliveredStatus() {
        Material awaiting = material(UUID.randomUUID());
        when(materialRepository.findById(awaiting.getId())).thenReturn(Optional.of(awaiting));

        assertThatThrownBy(() -> service.markChecked(awaiting.getId(), UUID.randomUUID(), List.of()))
                .isInstanceOf(MaterialDeliveryStatusOrderException.class);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void markCheckedTransitionsDeliveredToChecked() {
        Material delivered = material(UUID.randomUUID());
        delivered.markDelivered(UUID.randomUUID(), Instant.now());
        when(materialRepository.findById(delivered.getId())).thenReturn(Optional.of(delivered));

        Material result = service.markChecked(delivered.getId(), UUID.randomUUID(), null);

        assertThat(result.getStatus()).isEqualTo(MaterialDeliveryStatus.DELIVERED_AND_CHECKED);
    }
}
