package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.FornecedorPaymentMethod;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestApproval;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.OrcamentoNotLinkedToPurchaseRequestException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestApprovalRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
    private AppUserRepository userRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private PurchaseRequestApprovalRepository approvalRepository;

    @Mock
    private PdfBrandingService brandingService;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    private PurchaseRequestPdfService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new PurchaseRequestPdfService(
                purchaseRequestRepository, itemRepository, orcamentoRepository, lineItemRepository, userRepository,
                siteMembershipRepository, approvalRepository, brandingService, siteAccessService, permissionService);
        siteId = UUID.randomUUID();
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(brandingService.resolve(eq(siteId))).thenReturn(new PdfBrandingService.Branding("Empresa Teste", "Obra Teste", null));
        lenient().when(brandingService.renderHeaderHtml(any())).thenReturn("<div class=\"pdf-header\">Empresa Teste — Obra Teste</div>");
    }

    private PurchaseRequest purchaseRequest() {
        return new PurchaseRequest(UUID.randomUUID(), siteId, "Pedido 07/09/2026 #1", UUID.randomUUID(), Instant.now());
    }

    private Orcamento orcamento(UUID sourcePurchaseRequestId) {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", "Fornecedor Teste",
                null, null, null, null, null, null, sourcePurchaseRequestId);
    }

    private Orcamento orcamento(
            UUID sourcePurchaseRequestId, String fornecedorNome, FornecedorPaymentMethod paymentMethod, String pixKey) {
        return new Orcamento(
                UUID.randomUUID(), siteId, UUID.randomUUID(), Instant.now(), "12345678000199", fornecedorNome,
                null, null, null, paymentMethod, pixKey, null, sourcePurchaseRequestId);
    }

    private String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(document);
        }
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

    @Test
    void generateShowsPaymentMethodAndPixKeyForPixSupplier() throws IOException {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        Orcamento orcamento = orcamento(
                purchaseRequest.getId(), "Fornecedor Pix", FornecedorPaymentMethod.PIX, "chave@pix.com");
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of());

        byte[] pdf = service.generate(purchaseRequest.getId(), orcamento.getId(), UUID.randomUUID());

        String text = textOf(pdf);
        assertThat(text).contains("Forma de pagamento").contains("Pix");
        assertThat(text).contains("Chave Pix").contains("chave@pix.com");
    }

    @Test
    void generateOmitsPixKeyForNonPixSupplier() throws IOException {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        Orcamento orcamento = orcamento(
                purchaseRequest.getId(), "Fornecedor Boleto", FornecedorPaymentMethod.BOLETO, null);
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of());

        byte[] pdf = service.generate(purchaseRequest.getId(), orcamento.getId(), UUID.randomUUID());

        String text = textOf(pdf);
        assertThat(text).contains("Forma de pagamento").contains("Boleto");
        assertThat(text).doesNotContain("Chave Pix");
    }

    @Test
    void generateSummaryProducesAValidPdfAcrossMultipleSuppliers() throws IOException {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        Orcamento orcamentoA = orcamento(
                purchaseRequest.getId(), "Fornecedor Pix", FornecedorPaymentMethod.PIX, "chave@pix.com");
        Orcamento orcamentoB = orcamento(
                purchaseRequest.getId(), "Fornecedor Dinheiro", FornecedorPaymentMethod.DINHEIRO, null);

        PurchaseRequestItem itemA = new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequest.getId(), "Cimento", "Saco", BigDecimal.TEN, "saco",
                UUID.randomUUID(), Instant.now());
        OrcamentoLineItem lineItemA = new OrcamentoLineItem(
                UUID.randomUUID(), orcamentoA.getId(), "Cimento", "Saco", BigDecimal.TEN, new BigDecimal("30"), itemA.getId());
        itemA.select(lineItemA.getId());

        PurchaseRequestItem itemB = new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequest.getId(), "Areia", "m3", BigDecimal.ONE, "m3",
                UUID.randomUUID(), Instant.now());
        OrcamentoLineItem lineItemB = new OrcamentoLineItem(
                UUID.randomUUID(), orcamentoB.getId(), "Areia", "m3", BigDecimal.ONE, new BigDecimal("50"), itemB.getId());
        itemB.select(lineItemB.getId());

        PurchaseRequestItem unselectedItem = new PurchaseRequestItem(
                UUID.randomUUID(), siteId, purchaseRequest.getId(), "Brita", "m3", BigDecimal.ONE, "m3",
                UUID.randomUUID(), Instant.now());

        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId()))
                .thenReturn(List.of(itemA, itemB, unselectedItem));
        when(lineItemRepository.findAllById(List.of(lineItemA.getId(), lineItemB.getId())))
                .thenReturn(List.of(lineItemA, lineItemB));
        when(orcamentoRepository.findAllById(any())).thenReturn(List.of(orcamentoA, orcamentoB));

        byte[] pdf = service.generateSummary(purchaseRequest.getId(), UUID.randomUUID());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        String text = textOf(pdf);
        assertThat(text).contains("Pix");
        assertThat(text).contains("chave@pix.com");
        assertThat(text).contains("Dinheiro");
    }

    @Test
    void generateRendersSuccessfullyWithAnSvgLogo() {
        // End-to-end regression for the SVG-support wiring: without a registered SVGDrawer,
        // openhtmltopdf can't decode an "image/svg+xml" data URI and used to silently drop it —
        // this proves the whole render pipeline (including a real <img> pointing at SVG) completes.
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><circle r=\"4\" cx=\"5\" cy=\"5\"/></svg>";
        String svgDataUri = "data:image/svg+xml;base64,"
                + java.util.Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        PdfBrandingService.Branding svgBranding = new PdfBrandingService.Branding("Empresa Teste", "Obra Teste", svgDataUri);
        when(brandingService.resolve(eq(siteId))).thenReturn(svgBranding);
        when(brandingService.renderHeaderHtml(svgBranding))
                .thenReturn(new PdfBrandingService(null, null, null).renderHeaderHtml(svgBranding));
        Orcamento orcamento = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of());

        byte[] pdf = service.generate(purchaseRequest.getId(), orcamento.getId(), UUID.randomUUID());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void generateShowsBrandingHeaderRequesterAndInProgressIndicator() throws IOException {
        UUID requesterId = UUID.randomUUID();
        PurchaseRequest purchaseRequest = new PurchaseRequest(
                UUID.randomUUID(), siteId, "Pedido 07/09/2026 #1", requesterId, Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(
                new AppUser(requesterId, "solicitante@example.com", "Solicitante Um", "hash", null, Instant.now(), Instant.now())));
        Orcamento orcamento = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of());

        byte[] pdf = service.generate(purchaseRequest.getId(), orcamento.getId(), UUID.randomUUID());

        String text = textOf(pdf);
        assertThat(text).contains("Empresa Teste").contains("Obra Teste");
        assertThat(text).contains("Aberto por").contains("Solicitante Um");
        assertThat(text).contains("Em andamento");
    }

    @Test
    void generateShowsApprovalDecisionsAndFinalizationDate() throws IOException {
        PurchaseRequest purchaseRequest = purchaseRequest();
        purchaseRequest.markOrcado();
        purchaseRequest.submitForApproval(Instant.now());
        purchaseRequest.approve(Instant.now());
        purchaseRequest.complete(Instant.now());
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));

        UUID membershipId = UUID.randomUUID();
        UUID approverUserId = UUID.randomUUID();
        PurchaseRequestApproval step = new PurchaseRequestApproval(
                UUID.randomUUID(), purchaseRequest.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        step.approve(membershipId, null, Instant.now());
        when(approvalRepository.findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(purchaseRequest.getId()))
                .thenReturn(List.of(step));
        when(siteMembershipRepository.findById(membershipId)).thenReturn(Optional.of(
                SiteMembership.invited(membershipId, siteId, approverUserId, ConstructionFunction.ENGINEER, null, null, Instant.now())));
        when(userRepository.findById(approverUserId)).thenReturn(Optional.of(
                new AppUser(approverUserId, "engenheira@example.com", "Engenheira Um", "hash", null, Instant.now(), Instant.now())));
        when(userRepository.findById(purchaseRequest.getCreatedBy())).thenReturn(Optional.empty());

        Orcamento orcamento = orcamento(purchaseRequest.getId());
        when(orcamentoRepository.findById(orcamento.getId())).thenReturn(Optional.of(orcamento));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());
        when(lineItemRepository.findByOrcamentoId(orcamento.getId())).thenReturn(List.of());

        byte[] pdf = service.generate(purchaseRequest.getId(), orcamento.getId(), UUID.randomUUID());

        String text = textOf(pdf);
        assertThat(text).contains("Engenheira Um");
        assertThat(text).contains("Aprovado em");
        assertThat(text).contains("Finalizado em");
        assertThat(text).doesNotContain("Em andamento");
    }

    @Test
    void generateSummaryShowsBrandingHeader() throws IOException {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        when(itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequest.getId())).thenReturn(List.of());

        byte[] pdf = service.generateSummary(purchaseRequest.getId(), UUID.randomUUID());

        String text = textOf(pdf);
        assertThat(text).contains("Empresa Teste").contains("Obra Teste");
    }

    @Test
    void generateSummaryRejectsMemberWithoutVisibility() {
        PurchaseRequest purchaseRequest = purchaseRequest();
        when(purchaseRequestRepository.findById(purchaseRequest.getId())).thenReturn(Optional.of(purchaseRequest));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.PURCHASE_REQUEST))
                .when(permissionService).requireVisible(eq(siteId), any(), eq(PermissionCapability.PURCHASE_REQUEST));

        assertThatThrownBy(() -> service.generateSummary(purchaseRequest.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }
}
