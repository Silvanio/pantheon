package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.EquipmentRegistrationRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.EquipmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private EquipmentService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new EquipmentService(equipmentRepository, siteRepository, siteAccessService, permissionService);
        siteId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ConstructionSite site(UUID id) {
        return new ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    @Test
    void createRejectsMemberWithoutManageAccess() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.EQUIPMENT))
                .when(permissionService)
                .requireManage(eq(siteId), any(), eq(PermissionCapability.EQUIPMENT));

        assertThatThrownBy(() -> service.create(
                        siteId, UUID.randomUUID(), new EquipmentRegistrationRequest("Betoneira", null, EquipmentStatus.AVAILABLE)))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }

    @Test
    void listReturnsPagedResultsSortedByCreatedAtDescending() {
        Equipment equipment = new Equipment(
                UUID.randomUUID(), siteId, "Betoneira", null, EquipmentStatus.AVAILABLE, UUID.randomUUID(), Instant.now());
        when(equipmentRepository.findByConstructionSiteId(eq(siteId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(equipment), PageRequest.of(0, 20), 1));

        Page<Equipment> result = service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(equipment);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(equipmentRepository).findByConstructionSiteId(eq(siteId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void listRejectsMemberWithHiddenAccess() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.EQUIPMENT))
                .when(permissionService)
                .requireVisible(eq(siteId), any(), eq(PermissionCapability.EQUIPMENT));

        assertThatThrownBy(() -> service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }
}
