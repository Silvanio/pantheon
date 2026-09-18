package com.pantheon.service.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.FornecedorPaymentMethod;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestApproval;
import com.pantheon.service.entity.PurchaseRequestApprovalStatus;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.OrcamentoNotFoundException;
import com.pantheon.service.exception.OrcamentoNotLinkedToPurchaseRequestException;
import com.pantheon.service.exception.PurchaseRequestNotFoundException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestApprovalRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Renders, on demand (never cached), a supplier-scoped PDF of a Pedido de Compra's currently
 * selected items for one of its linked Orcamentos — see {@code purchase-requests}' "Per-supplier
 * Pedido de Compra PDF". Server-side HTML-to-PDF via openhtmltopdf, the same library already used
 * for daily-report exports ({@code DailyReportPdfService}).
 */
@Service
public class PurchaseRequestPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository itemRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final AppUserRepository userRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final PurchaseRequestApprovalRepository approvalRepository;
    private final PdfBrandingService brandingService;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public PurchaseRequestPdfService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository itemRepository,
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            AppUserRepository userRepository,
            SiteMembershipRepository siteMembershipRepository,
            PurchaseRequestApprovalRepository approvalRepository,
            PdfBrandingService brandingService,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.itemRepository = itemRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.lineItemRepository = lineItemRepository;
        this.userRepository = userRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.approvalRepository = approvalRepository;
        this.brandingService = brandingService;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public byte[] generate(UUID purchaseRequestId, UUID orcamentoId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requireVisiblePurchaseRequest(purchaseRequestId, actingUserId);

        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new OrcamentoNotFoundException(orcamentoId));
        if (!purchaseRequestId.equals(orcamento.getSourcePurchaseRequestId())) {
            throw new OrcamentoNotLinkedToPurchaseRequestException(orcamentoId, purchaseRequestId);
        }

        Map<UUID, OrcamentoLineItem> lineItemsById = lineItemRepository.findByOrcamentoId(orcamentoId).stream()
                .collect(Collectors.toMap(OrcamentoLineItem::getId, li -> li));
        List<PurchaseRequestItem> items = itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        List<OrcamentoLineItem> selectedForThisSupplier = items.stream()
                .map(PurchaseRequestItem::getSelectedOrcamentoLineItemId)
                .filter(Objects::nonNull)
                .map(lineItemsById::get)
                .filter(Objects::nonNull)
                .toList();

        String html = buildHtml(purchaseRequest, orcamento, selectedForThisSupplier);
        return renderPdf(html);
    }

    /** Every item, whichever supplier (if any) currently fulfills it, with per-supplier subtotals — see purchase-requests' "Pedido de Compra consolidated summary PDF". */
    public byte[] generateSummary(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = requireVisiblePurchaseRequest(purchaseRequestId, actingUserId);

        List<PurchaseRequestItem> items = itemRepository.findByPurchaseRequestIdOrderByCreatedAtDesc(purchaseRequestId);
        List<UUID> selectedLineItemIds =
                items.stream().map(PurchaseRequestItem::getSelectedOrcamentoLineItemId).filter(Objects::nonNull).toList();
        Map<UUID, OrcamentoLineItem> lineItemsById = lineItemRepository.findAllById(selectedLineItemIds).stream()
                .collect(Collectors.toMap(OrcamentoLineItem::getId, li -> li));
        List<UUID> orcamentoIds = lineItemsById.values().stream().map(OrcamentoLineItem::getOrcamentoId).distinct().toList();
        Map<UUID, Orcamento> orcamentosById = orcamentoRepository.findAllById(orcamentoIds).stream()
                .collect(Collectors.toMap(Orcamento::getId, o -> o));

        String html = buildSummaryHtml(purchaseRequest, items, lineItemsById, orcamentosById);
        return renderPdf(html);
    }

    private String buildHtml(PurchaseRequest purchaseRequest, Orcamento orcamento, List<OrcamentoLineItem> items) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        html.append("<style>").append(PdfBrandingService.STYLE).append("</style></head><body>");

        html.append(brandingService.renderHeaderHtml(brandingService.resolve(purchaseRequest.getConstructionSiteId())));
        html.append("<h1>").append(escape(purchaseRequest.getName())).append("</h1>")
                .append("<span class=\"badge\">Pedido de compra</span>");
        html.append(buildRequestMetaHtml(purchaseRequest));

        html.append("<div class=\"meta-card\">");
        html.append("<div class=\"meta-row\"><span class=\"label\">Fornecedor</span><span class=\"value\">")
                .append(escape(orcamento.getFornecedorNome())).append("</span></div>");
        html.append("<div class=\"meta-row\"><span class=\"label\">CNPJ</span><span class=\"value\">")
                .append(escape(orcamento.getFornecedorCnpj())).append("</span></div>");
        if (orcamento.getFornecedorEndereco() != null) {
            html.append("<div class=\"meta-row\"><span class=\"label\">Endereço</span><span class=\"value\">")
                    .append(escape(orcamento.getFornecedorEndereco())).append("</span></div>");
        }
        if (orcamento.getFornecedorContatoNome() != null || orcamento.getFornecedorContatoTelefone() != null) {
            html.append("<div class=\"meta-row\"><span class=\"label\">Contato</span><span class=\"value\">")
                    .append(escape(orcamento.getFornecedorContatoNome()))
                    .append(orcamento.getFornecedorContatoTelefone() != null
                            ? " — " + escape(orcamento.getFornecedorContatoTelefone()) : "")
                    .append("</span></div>");
        }
        if (orcamento.getFornecedorFormaPagamento() != null) {
            html.append("<div class=\"meta-row\"><span class=\"label\">Forma de pagamento</span><span class=\"value\">")
                    .append(paymentMethodLabel(orcamento.getFornecedorFormaPagamento())).append("</span></div>");
            if (orcamento.getFornecedorFormaPagamento() == FornecedorPaymentMethod.PIX) {
                html.append("<div class=\"meta-row\"><span class=\"label\">Chave Pix</span><span class=\"value\">")
                        .append(escape(orcamento.getFornecedorPixKey())).append("</span></div>");
            }
        }
        html.append("</div>");

        html.append("<h2>Itens selecionados</h2>");
        html.append("<table class=\"data\"><tr><th>Item</th><th class=\"right\">Quantidade</th>")
                .append("<th class=\"right\">Preço unitário</th><th class=\"right\">Total</th></tr>");
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (OrcamentoLineItem item : items) {
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(item.getQuantity());
            grandTotal = grandTotal.add(lineTotal);
            html.append("<tr><td>").append(escape(item.getName())).append("</td><td class=\"right\">")
                    .append(item.getQuantity()).append("</td><td class=\"right\">").append(money(unitPrice))
                    .append("</td><td class=\"right\">").append(money(lineTotal)).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p class=\"total-line\"><span class=\"label\">Total geral</span>").append(money(grandTotal)).append("</p>");
        html.append(footerNote());

        html.append("</body></html>");
        return html.toString();
    }

    private String buildSummaryHtml(
            PurchaseRequest purchaseRequest, List<PurchaseRequestItem> items, Map<UUID, OrcamentoLineItem> lineItemsById,
            Map<UUID, Orcamento> orcamentosById) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        html.append("<style>").append(PdfBrandingService.STYLE).append("</style></head><body>");

        html.append(brandingService.renderHeaderHtml(brandingService.resolve(purchaseRequest.getConstructionSiteId())));
        html.append("<h1>").append(escape(purchaseRequest.getName())).append("</h1>")
                .append("<span class=\"badge\">Resumo consolidado</span>");
        html.append(buildRequestMetaHtml(purchaseRequest));

        Map<UUID, BigDecimal> subtotalByOrcamentoId = new LinkedHashMap<>();
        html.append("<h2>Itens</h2>");
        html.append("<table class=\"data\"><tr><th>Item</th><th class=\"right\">Quantidade</th><th>Fornecedor</th>")
                .append("<th class=\"right\">Preço unitário</th><th class=\"right\">Total</th></tr>");
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (PurchaseRequestItem item : items) {
            OrcamentoLineItem lineItem = item.getSelectedOrcamentoLineItemId() != null
                    ? lineItemsById.get(item.getSelectedOrcamentoLineItemId())
                    : null;
            Orcamento orcamento = lineItem != null ? orcamentosById.get(lineItem.getOrcamentoId()) : null;

            html.append("<tr><td>").append(escape(item.getName())).append("</td><td class=\"right\">")
                    .append(item.getQuantity()).append("</td><td>");
            if (orcamento == null) {
                html.append("<span class=\"muted italic\">Não selecionado</span></td><td></td><td></td></tr>");
                continue;
            }
            BigDecimal unitPrice = lineItem.getUnitPrice() != null ? lineItem.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(item.getQuantity());
            grandTotal = grandTotal.add(lineTotal);
            subtotalByOrcamentoId.merge(orcamento.getId(), lineTotal, BigDecimal::add);
            html.append(escape(orcamento.getFornecedorNome())).append("</td><td class=\"right\">").append(money(unitPrice))
                    .append("</td><td class=\"right\">").append(money(lineTotal)).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Valor a pagar por fornecedor</h2>");
        html.append("<table class=\"data\"><tr><th>Fornecedor</th><th>Forma de pagamento</th><th>Chave Pix</th>")
                .append("<th class=\"right\">Subtotal</th></tr>");
        for (Map.Entry<UUID, BigDecimal> entry : subtotalByOrcamentoId.entrySet()) {
            Orcamento orcamento = orcamentosById.get(entry.getKey());
            boolean isPix = orcamento.getFornecedorFormaPagamento() == FornecedorPaymentMethod.PIX;
            html.append("<tr><td>").append(escape(orcamento.getFornecedorNome())).append("</td><td>")
                    .append(orcamento.getFornecedorFormaPagamento() != null
                            ? paymentMethodLabel(orcamento.getFornecedorFormaPagamento()) : "—")
                    .append("</td><td>").append(isPix ? escape(orcamento.getFornecedorPixKey()) : "—")
                    .append("</td><td class=\"right\">").append(money(entry.getValue())).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p class=\"total-line\"><span class=\"label\">Total geral</span>").append(money(grandTotal)).append("</p>");
        html.append(footerNote());

        html.append("</body></html>");
        return html.toString();
    }

    /** Requester + current-cycle approval decisions + open/close dates — see design.md decision 4. */
    private String buildRequestMetaHtml(PurchaseRequest purchaseRequest) {
        String requesterName = userRepository.findById(purchaseRequest.getCreatedBy())
                .map(this::displayNameOrEmail)
                .orElse("—");

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"meta-card\">");
        html.append("<div class=\"meta-row\"><span class=\"label\">Aberto por</span><span class=\"value\">")
                .append(escape(requesterName)).append("</span> <span class=\"muted\">em ")
                .append(DATE_FORMAT.format(purchaseRequest.getCreatedAt().atZone(ZoneOffset.UTC))).append("</span></div>");

        List<PurchaseRequestApproval> decidedSteps = approvalRepository
                .findByPurchaseRequestIdOrderByCycleNumberAscStepOrderAsc(purchaseRequest.getId())
                .stream()
                .filter(a -> a.getCycleNumber() == purchaseRequest.getCurrentApprovalCycle())
                .filter(a -> a.getStatus() != PurchaseRequestApprovalStatus.PENDING)
                .toList();
        for (PurchaseRequestApproval step : decidedSteps) {
            String approverName = step.getDecidedBySiteMembershipId() != null
                    ? resolveMembershipName(step.getDecidedBySiteMembershipId())
                    : "Equipe da empresa";
            String verb = step.getStatus() == PurchaseRequestApprovalStatus.APPROVED ? "Aprovado" : "Rejeitado";
            html.append("<div class=\"meta-row\"><span class=\"label\">")
                    .append(escape(functionLabel(step.getApproverFunction()))).append("</span><span class=\"value\">")
                    .append(escape(approverName)).append("</span> <span class=\"muted\">— ").append(verb).append(" em ")
                    .append(step.getDecidedAt() != null ? DATETIME_FORMAT.format(step.getDecidedAt().atZone(ZoneOffset.UTC)) : "—")
                    .append("</span></div>");
        }

        html.append("<div class=\"meta-row\"><span class=\"label\">Finalizado em</span><span class=\"value\">")
                .append(purchaseRequest.getCompletedAt() != null
                        ? DATE_FORMAT.format(purchaseRequest.getCompletedAt().atZone(ZoneOffset.UTC))
                        : "Em andamento")
                .append("</span></div>");
        html.append("</div>");
        return html.toString();
    }

    private String functionLabel(ConstructionFunction function) {
        return switch (function) {
            case ADMIN -> "Administrador";
            case CLIENT -> "Cliente";
            case ENGINEER -> "Engenheiro(a)";
            case ARCHITECT -> "Arquiteto(a)";
            case SITE_FOREMAN -> "Mestre de obras";
            case SERVICE_PROVIDER -> "Prestador de serviço";
            case OTHER -> "Outro";
        };
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private String footerNote() {
        return "<p class=\"footer-note\">Gerado em "
                + DATETIME_FORMAT.format(Instant.now().atZone(ZoneOffset.UTC)) + "</p>";
    }

    /** A {@code SiteMembership}'s displayable name: its own {@code displayName} when set (accountless members), otherwise its linked {@code AppUser}'s. */
    private String resolveMembershipName(UUID siteMembershipId) {
        SiteMembership membership = siteMembershipRepository.findById(siteMembershipId).orElse(null);
        if (membership == null) {
            return "—";
        }
        if (membership.getDisplayName() != null && !membership.getDisplayName().isBlank()) {
            return membership.getDisplayName();
        }
        if (membership.getUserId() != null) {
            return userRepository.findById(membership.getUserId()).map(this::displayNameOrEmail).orElse("—");
        }
        return "—";
    }

    private String displayNameOrEmail(AppUser user) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank() ? user.getDisplayName() : user.getEmail();
    }

    private PurchaseRequest requireVisiblePurchaseRequest(UUID purchaseRequestId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository
                .findById(purchaseRequestId)
                .orElseThrow(() -> new PurchaseRequestNotFoundException(purchaseRequestId));
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);
        return purchaseRequest;
    }

    private byte[] renderPdf(String html) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useSVGDrawer(new BatikSVGDrawer());
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        try {
            builder.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render purchase request PDF", e);
        }
        return outputStream.toByteArray();
    }

    private String paymentMethodLabel(FornecedorPaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CARTAO -> "Cartão";
            case BOLETO -> "Boleto";
            case PIX -> "Pix";
            case DINHEIRO -> "Dinheiro";
        };
    }

    private String escape(String value) {
        if (value == null) {
            return "—";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
