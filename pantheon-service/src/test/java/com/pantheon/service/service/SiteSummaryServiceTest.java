package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestStatus;
import com.pantheon.service.entity.SiteDocumentProject;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.EquipmentRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteDocumentProjectRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.TaskCardRepository;
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

@ExtendWith(MockitoExtension.class)
class SiteSummaryServiceTest {

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private DailyReportService dailyReportService;

    @Mock
    private PurchaseRequestService purchaseRequestService;

    @Mock
    private OrcamentoService orcamentoService;

    @Mock
    private EquipmentService equipmentService;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private TaskCardRepository taskCardRepository;

    @Mock
    private SiteDocumentProjectRepository siteDocumentProjectRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private SiteSummaryService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new SiteSummaryService(
                scheduleService, dailyReportService, purchaseRequestService, orcamentoService, equipmentService,
                equipmentRepository, purchaseRequestRepository, orcamentoRepository, taskCardRepository,
                siteDocumentProjectRepository, siteMembershipRepository, siteRepository, siteAccessService,
                permissionService);
        siteId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient()
                .when(purchaseRequestRepository.countByConstructionSiteIdAndStatusAndSubmittedAtIsNotNull(
                        eq(siteId), eq(PurchaseRequestStatus.ORCADO)))
                .thenReturn(0L);
        lenient()
                .when(orcamentoRepository.countByConstructionSiteIdAndStatus(eq(siteId), eq(OrcamentoStatus.DRAFT)))
                .thenReturn(0L);
    }

    private ConstructionSite site(UUID id) {
        return new ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private PurchaseRequest purchaseRequest() {
        return new PurchaseRequest(UUID.randomUUID(), siteId, "Pedido 07/09/2026 #1", UUID.randomUUID(), Instant.now());
    }

    private Orcamento orcamento() {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor A",
                "Endereco", "Contato", "11999999999", null, null, null, null);
    }

    private TaskCard taskCard() {
        return new TaskCard(
                UUID.randomUUID(), siteId, UUID.randomUUID(), "Tarefa", null, null, 0, UUID.randomUUID(), Instant.now(),
                Instant.now());
    }

    private SiteDocumentProject siteDocumentProject() {
        return new SiteDocumentProject(
                UUID.randomUUID(), siteId, null, "Projeto A", null, UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                Instant.now());
    }

    private DailyReport dailyReport() {
        return new DailyReport(UUID.randomUUID(), siteId, LocalDate.now(), 1, UUID.randomUUID(), Instant.now());
    }

    private Equipment equipment() {
        return new Equipment(
                UUID.randomUUID(), siteId, "Betoneira", null, EquipmentStatus.AVAILABLE, UUID.randomUUID(), Instant.now());
    }

    @Test
    void fullAccessMemberSeesEverySection() {
        when(scheduleService.computeProgress(siteId)).thenReturn(42);
        when(dailyReportService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dailyReport()), PageRequest.of(0, 1), 5));
        when(purchaseRequestService.list(eq(siteId), any(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(purchaseRequest()), PageRequest.of(0, 3), 7));
        when(orcamentoService.list(eq(siteId), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orcamento()), PageRequest.of(0, 3), 3));
        when(equipmentService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(equipment()), PageRequest.of(0, 1), 9));
        when(equipmentRepository.countByConstructionSiteIdAndStatus(siteId, EquipmentStatus.UNAVAILABLE)).thenReturn(2L);
        when(purchaseRequestRepository.countByConstructionSiteIdAndStatusAndSubmittedAtIsNotNull(
                        siteId, PurchaseRequestStatus.ORCADO))
                .thenReturn(2L);
        when(orcamentoRepository.countByConstructionSiteIdAndStatus(siteId, OrcamentoStatus.DRAFT)).thenReturn(1L);
        when(siteDocumentProjectRepository.countByConstructionSiteId(siteId)).thenReturn(4L);
        when(siteDocumentProjectRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId))
                .thenReturn(List.of(siteDocumentProject()));
        when(taskCardRepository.countByConstructionSiteId(siteId)).thenReturn(6L);
        when(taskCardRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)).thenReturn(List.of(taskCard()));
        when(siteMembershipRepository.countByConstructionSiteId(siteId)).thenReturn(3L);

        var result = service.build(siteId, UUID.randomUUID());

        assertThat(result.schedulePercentComplete()).isEqualTo(42);
        assertThat(result.dailyReports().total()).isEqualTo(5);
        assertThat(result.purchaseRequests().total()).isEqualTo(7);
        assertThat(result.purchaseRequests().recent()).hasSize(1);
        assertThat(result.purchaseRequests().awaitingApproval()).isEqualTo(2);
        assertThat(result.orcamentos().total()).isEqualTo(3);
        assertThat(result.orcamentos().draft()).isEqualTo(1);
        assertThat(result.equipment().total()).isEqualTo(9);
        assertThat(result.equipment().unavailable()).isEqualTo(2);
        assertThat(result.projects().total()).isEqualTo(4);
        assertThat(result.tasks().total()).isEqualTo(6);
        assertThat(result.teamMembersCount()).isEqualTo(3);
    }

    @Test
    void hiddenEquipmentOmitsOnlyEquipmentSection() {
        when(scheduleService.computeProgress(siteId)).thenReturn(null);
        when(dailyReportService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(purchaseRequestService.list(eq(siteId), any(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));
        when(orcamentoService.list(eq(siteId), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));
        when(equipmentService.list(eq(siteId), any(), any(Pageable.class)))
                .thenThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.EQUIPMENT));
        when(siteDocumentProjectRepository.countByConstructionSiteId(siteId)).thenReturn(0L);
        when(siteDocumentProjectRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)).thenReturn(List.of());
        when(taskCardRepository.countByConstructionSiteId(siteId)).thenReturn(0L);
        when(taskCardRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)).thenReturn(List.of());
        when(siteMembershipRepository.countByConstructionSiteId(siteId)).thenReturn(1L);

        var result = service.build(siteId, UUID.randomUUID());

        assertThat(result.equipment()).isNull();
        assertThat(result.dailyReports()).isNotNull();
        assertThat(result.purchaseRequests()).isNotNull();
        assertThat(result.orcamentos()).isNotNull();
        assertThat(result.projects()).isNotNull();
        assertThat(result.tasks()).isNotNull();
        assertThat(result.teamMembersCount()).isEqualTo(1);
    }

    @Test
    void hiddenTasksOmitsOnlyTasksSection() {
        when(scheduleService.computeProgress(siteId)).thenReturn(null);
        when(dailyReportService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(purchaseRequestService.list(eq(siteId), any(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));
        when(orcamentoService.list(eq(siteId), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));
        when(equipmentService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(siteDocumentProjectRepository.countByConstructionSiteId(siteId)).thenReturn(0L);
        when(siteDocumentProjectRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)).thenReturn(List.of());
        lenient()
                .doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.TASKS))
                .when(permissionService)
                .requireVisible(eq(siteId), any(), eq(PermissionCapability.TASKS));
        when(siteMembershipRepository.countByConstructionSiteId(siteId)).thenReturn(1L);

        var result = service.build(siteId, UUID.randomUUID());

        assertThat(result.tasks()).isNull();
        assertThat(result.projects()).isNotNull();
    }

    @Test
    void requestsOnlyThreeRecentItemsForPurchaseRequests() {
        when(scheduleService.computeProgress(siteId)).thenReturn(null);
        when(dailyReportService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(purchaseRequestService.list(eq(siteId), any(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));
        when(orcamentoService.list(eq(siteId), any(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 3), 0));
        when(equipmentService.list(eq(siteId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));
        when(siteDocumentProjectRepository.countByConstructionSiteId(siteId)).thenReturn(0L);
        when(siteDocumentProjectRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)).thenReturn(List.of());
        when(taskCardRepository.countByConstructionSiteId(siteId)).thenReturn(0L);
        when(taskCardRepository.findTop5ByConstructionSiteIdOrderByCreatedAtDesc(siteId)).thenReturn(List.of());
        when(siteMembershipRepository.countByConstructionSiteId(siteId)).thenReturn(0L);

        service.build(siteId, UUID.randomUUID());

        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(purchaseRequestService)
                .list(eq(siteId), any(), isNull(), isNull(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
    }
}
