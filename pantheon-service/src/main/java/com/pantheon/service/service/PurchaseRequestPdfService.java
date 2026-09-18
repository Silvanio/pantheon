package com.pantheon.service.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoLineItem;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestItem;
import com.pantheon.service.exception.OrcamentoNotFoundException;
import com.pantheon.service.exception.OrcamentoNotLinkedToPurchaseRequestException;
import com.pantheon.service.exception.PurchaseRequestNotFoundException;
import com.pantheon.service.repository.OrcamentoLineItemRepository;
import com.pantheon.service.repository.OrcamentoRepository;
import com.pantheon.service.repository.PurchaseRequestItemRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository itemRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoLineItemRepository lineItemRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public PurchaseRequestPdfService(
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestItemRepository itemRepository,
            OrcamentoRepository orcamentoRepository,
            OrcamentoLineItemRepository lineItemRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.itemRepository = itemRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.lineItemRepository = lineItemRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public byte[] generate(UUID purchaseRequestId, UUID orcamentoId, UUID actingUserId) {
        PurchaseRequest purchaseRequest = purchaseRequestRepository
                .findById(purchaseRequestId)
                .orElseThrow(() -> new PurchaseRequestNotFoundException(purchaseRequestId));
        var access = siteAccessService.requireAccess(purchaseRequest.getConstructionSiteId(), actingUserId);
        permissionService.requireVisible(purchaseRequest.getConstructionSiteId(), access, PermissionCapability.PURCHASE_REQUEST);

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

    private String buildHtml(PurchaseRequest purchaseRequest, Orcamento orcamento, List<OrcamentoLineItem> items) {
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        html.append("<style>");
        html.append("body{font-family:sans-serif;font-size:11px;color:#1a1a1a;}");
        html.append("h1{font-size:18px;margin-bottom:4px;} h2{font-size:13px;margin-top:16px;border-bottom:1px solid #ccc;}");
        html.append("table{width:100%;border-collapse:collapse;margin-top:4px;}");
        html.append("td,th{border:1px solid #ddd;padding:4px 6px;text-align:left;font-size:10px;}");
        html.append(".meta{color:#555;margin-bottom:8px;} .total{font-weight:bold;}");
        html.append("</style></head><body>");

        html.append("<h1>").append(escape(purchaseRequest.getName())).append("</h1>");
        html.append("<p class=\"meta\">Data: ")
                .append(DATE_FORMAT.format(purchaseRequest.getCreatedAt().atZone(ZoneOffset.UTC))).append("</p>");
        html.append("<p class=\"meta\">Fornecedor: ").append(escape(orcamento.getFornecedorNome()))
                .append(" — CNPJ: ").append(escape(orcamento.getFornecedorCnpj())).append("</p>");
        if (orcamento.getFornecedorEndereco() != null) {
            html.append("<p class=\"meta\">Endereço: ").append(escape(orcamento.getFornecedorEndereco())).append("</p>");
        }
        if (orcamento.getFornecedorContatoNome() != null || orcamento.getFornecedorContatoTelefone() != null) {
            html.append("<p class=\"meta\">Contato: ").append(escape(orcamento.getFornecedorContatoNome()))
                    .append(orcamento.getFornecedorContatoTelefone() != null
                            ? " — " + escape(orcamento.getFornecedorContatoTelefone()) : "")
                    .append("</p>");
        }

        html.append("<h2>Itens selecionados</h2>");
        html.append("<table><tr><th>Item</th><th>Quantidade</th><th>Preço unitário</th><th>Total</th></tr>");
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (OrcamentoLineItem item : items) {
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(item.getQuantity());
            grandTotal = grandTotal.add(lineTotal);
            html.append("<tr><td>").append(escape(item.getName())).append("</td><td>").append(item.getQuantity())
                    .append("</td><td>").append(unitPrice).append("</td><td>").append(lineTotal).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p class=\"total\">Total geral: ").append(grandTotal).append("</p>");

        html.append("</body></html>");
        return html.toString();
    }

    private byte[] renderPdf(String html) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        try {
            builder.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render purchase request PDF", e);
        }
        return outputStream.toByteArray();
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
