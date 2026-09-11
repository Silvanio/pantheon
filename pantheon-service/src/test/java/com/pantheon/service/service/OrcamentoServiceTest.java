package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.dto.OrcamentoLineItemRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoApproval;
import com.pantheon.service.entity.OrcamentoApprovalStatus;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.entity.SiteOrcamentoApprovalLevel;
import com.pantheon.service.exception.NotCurrentApprovalStepException;
import com.pantheon.service.exception.OrcamentoEmptyException;
import com.pantheon.service.exception.OrcamentoNotApprovedException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.messaging.OrcamentoApprovalStepPendingEvent;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.OrcamentoApprovalRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
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
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoLineItemRepository lineItemRepository;

    @Mock
    private OrcamentoApprovalRepository approvalRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private SiteOrcamentoApprovalLevelService approvalLevelService;

    @Mock
    private MaterialService materialService;

    @Mock
    private FornecedorService fornecedorService;

    @Mock
    private EventPublisher eventPublisher;

    private OrcamentoService service;

    private UUID siteId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        service = new OrcamentoService(
                orcamentoRepository, lineItemRepository, approvalRepository, siteRepository, siteMembershipRepository,
                userRepository, purchaseRequestRepository, siteAccessService, permissionService, approvalLevelService,
                materialService, fornecedorService, eventPublisher);

        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(orcamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(lineItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
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

    private SiteMembership activeMember(UUID userId, ConstructionFunction function) {
        SiteMembership member = SiteMembership.invited(UUID.randomUUID(), siteId, userId, function, null, Instant.now());
        member.accept();
        return member;
    }

    private FornecedorRequest fornecedorRequest() {
        return new FornecedorRequest("12345678000199", "Fornecedor Teste", null, null, null);
    }

    private Orcamento orcamento() {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor Teste",
                null, null, null, null, null);
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
    void submitForApprovalRejectsEmptyOrcamento() {
        Orcamento orcamento = orcamento();
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.submitForApproval(orcamento.getId(), UUID.randomUUID()))
                .isInstanceOf(OrcamentoEmptyException.class);
    }

    @Test
    void submitForApprovalCreatesStepsFromDefaultLevelAndNotifies() {
        Orcamento orcamento = orcamento();
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of(new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, null)));
        when(approvalLevelService.getEffectiveLevels(siteId)).thenReturn(List.of(new SiteOrcamentoApprovalLevel(
                UUID.randomUUID(), siteId, 1, ConstructionFunction.ENGINEER, Instant.now())));

        UUID engineerUserId = UUID.randomUUID();
        when(siteMembershipRepository.findByConstructionSiteIdAndFunction(siteId, ConstructionFunction.ENGINEER))
                .thenReturn(List.of(activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(userRepository.findById(engineerUserId)).thenReturn(Optional.of(
                new AppUser(engineerUserId, "eng@example.com", "Eng", "hash", null, Instant.now(), Instant.now())));

        Orcamento result = service.submitForApproval(orcamento.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.IN_APPROVAL);
        assertThat(result.getCurrentApprovalCycle()).isEqualTo(1);
        verify(approvalRepository).save(
                argThat(a -> a.getStepOrder() == 1 && a.getApproverFunction() == ConstructionFunction.ENGINEER));
        verify(eventPublisher).publish(eq(OrcamentoApprovalStepPendingEvent.TYPE), any());
    }

    @Test
    void approveStepAdvancesToNextStepWithoutApprovingOrcamento() {
        Orcamento orcamento = orcamento();
        orcamento.submitForApproval(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        OrcamentoApproval step1 = new OrcamentoApproval(
                UUID.randomUUID(), orcamento.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        OrcamentoApproval step2 = new OrcamentoApproval(
                UUID.randomUUID(), orcamento.getId(), 1, 2, ConstructionFunction.CLIENT, Instant.now());
        when(approvalRepository.findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                orcamento.getId(), 1, OrcamentoApprovalStatus.PENDING))
                .thenReturn(Optional.of(step1), Optional.of(step2));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(siteMembershipRepository.findByConstructionSiteIdAndFunction(siteId, ConstructionFunction.CLIENT))
                .thenReturn(List.of());

        Orcamento result = service.approveStep(orcamento.getId(), engineerUserId, null);

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.IN_APPROVAL);
        assertThat(step1.getStatus()).isEqualTo(OrcamentoApprovalStatus.APPROVED);
    }

    @Test
    void approveStepApprovesOrcamentoWhenLastStep() {
        Orcamento orcamento = orcamento();
        orcamento.submitForApproval(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        OrcamentoApproval onlyStep = new OrcamentoApproval(
                UUID.randomUUID(), orcamento.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                orcamento.getId(), 1, OrcamentoApprovalStatus.PENDING))
                .thenReturn(Optional.of(onlyStep), Optional.empty());

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));

        Orcamento result = service.approveStep(orcamento.getId(), engineerUserId, null);

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.APPROVED);
    }

    @Test
    void approveStepBlocksNonMatchingFunction() {
        Orcamento orcamento = orcamento();
        orcamento.submitForApproval(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        OrcamentoApproval step = new OrcamentoApproval(
                UUID.randomUUID(), orcamento.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                orcamento.getId(), 1, OrcamentoApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID architectUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, architectUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(architectUserId, ConstructionFunction.ARCHITECT)));

        assertThatThrownBy(() -> service.approveStep(orcamento.getId(), architectUserId, null))
                .isInstanceOf(NotCurrentApprovalStepException.class);
    }

    @Test
    void companyStaffCanAlwaysActOnApprovalStep() {
        Orcamento orcamento = orcamento();
        orcamento.submitForApproval(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        OrcamentoApproval step = new OrcamentoApproval(
                UUID.randomUUID(), orcamento.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                orcamento.getId(), 1, OrcamentoApprovalStatus.PENDING))
                .thenReturn(Optional.of(step), Optional.empty());

        UUID staffUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, staffUserId)).thenReturn(new SiteAccessContext(true, null));

        Orcamento result = service.approveStep(orcamento.getId(), staffUserId, "ok");

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.APPROVED);
        assertThat(step.getDecidedBySiteMembershipId()).isNull();
    }

    @Test
    void rejectStepRequiresAReason() {
        assertThatThrownBy(() -> service.rejectStep(UUID.randomUUID(), UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectStepReturnsOrcamentoToDraftWithReason() {
        Orcamento orcamento = orcamento();
        orcamento.submitForApproval(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        OrcamentoApproval step = new OrcamentoApproval(
                UUID.randomUUID(), orcamento.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                orcamento.getId(), 1, OrcamentoApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));

        Orcamento result = service.rejectStep(orcamento.getId(), engineerUserId, "Preço muito alto");

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.DRAFT);
        assertThat(result.getLastRejectionReason()).isEqualTo("Preço muito alto");
        assertThat(step.getStatus()).isEqualTo(OrcamentoApprovalStatus.REJECTED);
    }

    @Test
    void concludeRequiresApprovedStatus() {
        Orcamento draft = orcamento();
        when(orcamentoRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.conclude(draft.getId(), UUID.randomUUID()))
                .isInstanceOf(OrcamentoNotApprovedException.class);
    }

    @Test
    void concludeCreatesMaterialsFromLineItems() {
        Orcamento orcamento = orcamento();
        orcamento.submitForApproval(Instant.now());
        orcamento.approve(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        List<OrcamentoLineItem> items = List.of(new OrcamentoLineItem(
                UUID.randomUUID(), orcamento.getId(), "Cimento", "Saco", BigDecimal.TEN, BigDecimal.ONE, null));
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(items);

        Orcamento result = service.conclude(orcamento.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.COMPLETED);
        verify(materialService).createFromOrcamento(orcamento, items);
    }
}
