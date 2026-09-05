package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.OrcamentoCreationRequest;
import com.pantheon.service.dto.OrcamentoLineItemRequest;
import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.MaterialRequest;
import com.pantheon.service.entity.MaterialRequestStatus;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.NotSiteClientException;
import com.pantheon.service.exception.OrcamentoNotSentException;
import com.pantheon.service.messaging.EventPublisher;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.MaterialRequestRepository;
import com.pantheon.service.repository.OrcamentoAttachmentRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.repository.AppUserRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoLineItemRepository lineItemRepository;

    @Mock
    private OrcamentoAttachmentRepository attachmentRepository;

    @Mock
    private MaterialRequestRepository materialRequestRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private StorageService storageService;

    @Mock
    private EventPublisher eventPublisher;

    private OrcamentoService service;

    private UUID siteId;
    private UUID requestId;
    private MaterialRequest materialRequest;

    @BeforeEach
    void setUp() {
        service = new OrcamentoService(
                orcamentoRepository, lineItemRepository, attachmentRepository, materialRequestRepository, siteRepository,
                siteMembershipRepository, userRepository, siteAccessService, permissionService, storageService,
                eventPublisher);

        siteId = UUID.randomUUID();
        requestId = UUID.randomUUID();
        materialRequest = new MaterialRequest(requestId, siteId, UUID.randomUUID(), Instant.now());
        lenient().when(materialRequestRepository.findById(requestId)).thenReturn(Optional.of(materialRequest));
        lenient().when(materialRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orcamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(siteAccessService.requireAccess(any(), any())).thenReturn(new SiteAccessContext(true, null));
    }

    @Test
    void createDraftsOrcamentoWithPricedLineItems() {
        UUID itemId = UUID.randomUUID();
        UUID actingUserId = UUID.randomUUID();

        Orcamento result = service.create(
                requestId, actingUserId, new OrcamentoCreationRequest(List.of(new OrcamentoLineItemRequest(itemId, new BigDecimal("10.50")))));

        assertThat(result.getMaterialRequestId()).isEqualTo(requestId);
        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.DRAFT);
        verify(permissionService).requireManage(siteId, new SiteAccessContext(true, null), PermissionCapability.MATERIAL_REQUEST);
    }

    @Test
    void sendTransitionsToSentAndNotifiesActiveClients() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), requestId, UUID.randomUUID(), Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        ConstructionSite site = new ConstructionSite(
                siteId, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));

        UUID clientUserId = UUID.randomUUID();
        SiteMembership client = SiteMembership.invited(
                UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, Instant.now());
        client.accept();
        when(siteMembershipRepository.findByConstructionSiteId(siteId)).thenReturn(List.of(client));
        when(userRepository.findById(clientUserId)).thenReturn(
                Optional.of(new AppUser(clientUserId, "cliente@example.com", "Cliente", "hash", null, Instant.now(), Instant.now())));

        Orcamento result = service.send(orcamento.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.SENT);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("orcamento-sent"), any());
    }

    @Test
    void approveRequiresActiveClientMembership() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), requestId, UUID.randomUUID(), Instant.now());
        orcamento.send(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        UUID nonClientUserId = UUID.randomUUID();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, nonClientUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(orcamento.getId(), nonClientUserId)).isInstanceOf(NotSiteClientException.class);
    }

    @Test
    void approveRequiresSentStatus() {
        Orcamento draft = new Orcamento(UUID.randomUUID(), requestId, UUID.randomUUID(), Instant.now());
        when(orcamentoRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        UUID clientUserId = UUID.randomUUID();
        SiteMembership client = SiteMembership.invited(UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, Instant.now());
        client.accept();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, clientUserId)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.approve(draft.getId(), clientUserId)).isInstanceOf(OrcamentoNotSentException.class);
    }

    @Test
    void approvingSentOrcamentoApprovesThePendingMaterialRequest() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), requestId, UUID.randomUUID(), Instant.now());
        orcamento.send(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        UUID clientUserId = UUID.randomUUID();
        SiteMembership client = SiteMembership.invited(UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, Instant.now());
        client.accept();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, clientUserId)).thenReturn(Optional.of(client));

        Orcamento result = service.approve(orcamento.getId(), clientUserId);

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.APPROVED);
        assertThat(materialRequest.getStatus()).isEqualTo(MaterialRequestStatus.APPROVED);
        verify(materialRequestRepository, times(1)).save(materialRequest);
    }

    @Test
    void rejectRequiresAReason() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), requestId, UUID.randomUUID(), Instant.now());
        orcamento.send(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        UUID clientUserId = UUID.randomUUID();
        SiteMembership client = SiteMembership.invited(UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, Instant.now());
        client.accept();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, clientUserId)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.reject(orcamento.getId(), clientUserId, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectWithReasonLeavesMaterialRequestPendingForARevision() {
        Orcamento orcamento = new Orcamento(UUID.randomUUID(), requestId, UUID.randomUUID(), Instant.now());
        orcamento.send(Instant.now());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));

        UUID clientUserId = UUID.randomUUID();
        SiteMembership client = SiteMembership.invited(UUID.randomUUID(), siteId, clientUserId, ConstructionFunction.CLIENT, null, Instant.now());
        client.accept();
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, clientUserId)).thenReturn(Optional.of(client));

        Orcamento result = service.reject(orcamento.getId(), clientUserId, "Preço muito alto");

        assertThat(result.getStatus()).isEqualTo(OrcamentoStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Preço muito alto");
        assertThat(materialRequest.getStatus()).isEqualTo(MaterialRequestStatus.PENDING);
    }
}
