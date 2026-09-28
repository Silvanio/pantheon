package com.pantheon.service.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.pantheon.service.dto.DailyReportApprovalResponse;
import com.pantheon.service.dto.DailyReportImportedInvoiceResponse;
import com.pantheon.service.entity.ActivityStatus;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMaterialReceived;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportStatus;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.MaterialDeliveryStatus;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestInvoice;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportEquipmentUsageRepository;
import com.pantheon.service.repository.DailyReportMaterialReceivedRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportOccurrenceRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.EquipmentRepository;
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.PurchaseRequestInvoiceRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.storage.StorageService;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Renders a daily report to PDF on demand (never cached) — see design.md "PDF generation". Server-
 * side HTML-to-PDF via openhtmltopdf, which requires well-formed XHTML input. Section layout,
 * order, and copy follow {@code DailyReportPDF.dc.html} (the redesign's visual source of truth),
 * translated into table-based markup that openhtmltopdf renders reliably (its CSS Grid/Flexbox
 * support is unreliable, unlike the mockup's own browser-rendered CSS) and bound to real data —
 * see this class's own doc comments below for the handful of places the mockup showed a detail
 * (file size, workforce member photos) this backend has no data for, so it was intentionally
 * dropped rather than fabricated. Keeps {@link PdfBrandingService#STYLE}'s Helvetica/Arial
 * convention — only section content and supplementary spacing/color CSS are added here.
 */
@Service
public class DailyReportPdfService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter REPORT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));

    /** Supplementary CSS for elements {@link PdfBrandingService#STYLE} doesn't already cover (status pills, meta grid, comments box, photo grid) — appended alongside the shared style, not replacing it. */
    private static final String EXTRA_STYLE = ""
            + ".status-pill{display:inline-block;border-radius:999px;padding:4px 12px;font-size:8.5px;font-weight:700;white-space:nowrap;}"
            + ".status-approved{background:#d1fae5;color:#047857;} .status-pending{background:#fef3c7;color:#92400e;}"
            + ".status-draft{background:#eef0f6;color:#4e546e;} .status-rejected{background:#fde8e8;color:#b42318;}"
            + ".title-table{width:100%;margin:2px 0 14px;} .title-table td{border:none;padding:0;vertical-align:middle;}"
            + ".report-title{font-size:14px;font-weight:700;color:#12172e;}"
            + ".meta-grid{width:100%;border:1px solid #e1e4ee;border-radius:4px;margin:0 0 14px;}"
            + ".meta-grid td{background:#f9fafc;border-right:1px solid #eef0f6;padding:8px 10px;vertical-align:top;width:25%;}"
            + ".meta-grid td:last-child{border-right:none;}"
            + ".meta-label{display:block;font-size:7.5px;font-weight:700;color:#9298b3;text-transform:uppercase;letter-spacing:0.03em;margin-bottom:3px;}"
            + ".meta-value{font-size:10px;font-weight:700;color:#12172e;}"
            + ".comments-box{background:#f6f7fb;border-radius:4px;padding:9px 11px;margin:0 0 14px;font-size:9px;color:#4e546e;line-height:1.5;}"
            + ".tag-note{font-size:7.5px;color:#9298b3;font-weight:700;} .tag-link{font-size:7.5px;color:#2f53f0;font-weight:700;}"
            + ".photo-grid{width:100%;margin-top:2px;} .photo-grid td{border:none;padding:4px;vertical-align:top;width:25%;}"
            + ".photo{max-width:100%;max-height:120px;border-radius:4px;border:1px solid #eef0f6;}"
            + ".video-placeholder{width:100%;height:80px;background:#f4f6ff;border-radius:4px;border:1px solid #eef0f6;"
            + "display:table-cell;text-align:center;vertical-align:middle;font-size:8px;color:#6b7290;font-weight:700;}"
            + ".photo-caption{font-size:7.5px;color:#9298b3;margin-top:3px;}";

    private final DailyReportRepository dailyReportRepository;
    private final DailyReportWorkforceEntryRepository workforceEntryRepository;
    private final DailyReportEquipmentUsageRepository equipmentUsageRepository;
    private final DailyReportActivityRepository activityRepository;
    private final DailyReportOccurrenceRepository occurrenceRepository;
    private final DailyReportMaterialReceivedRepository materialReceivedRepository;
    private final DailyReportMediaRepository mediaRepository;
    private final DailyReportAttachmentRepository attachmentRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final EquipmentRepository equipmentRepository;
    private final MaterialRepository materialRepository;
    private final MaterialService materialService;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestInvoiceRepository purchaseRequestInvoiceRepository;
    private final DailyReportService dailyReportService;
    private final StorageService storageService;
    private final PdfBrandingService brandingService;

    public DailyReportPdfService(
            DailyReportRepository dailyReportRepository,
            DailyReportWorkforceEntryRepository workforceEntryRepository,
            DailyReportEquipmentUsageRepository equipmentUsageRepository,
            DailyReportActivityRepository activityRepository,
            DailyReportOccurrenceRepository occurrenceRepository,
            DailyReportMaterialReceivedRepository materialReceivedRepository,
            DailyReportMediaRepository mediaRepository,
            DailyReportAttachmentRepository attachmentRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            EquipmentRepository equipmentRepository,
            MaterialRepository materialRepository,
            MaterialService materialService,
            PurchaseRequestRepository purchaseRequestRepository,
            PurchaseRequestInvoiceRepository purchaseRequestInvoiceRepository,
            DailyReportService dailyReportService,
            StorageService storageService,
            PdfBrandingService brandingService) {
        this.dailyReportRepository = dailyReportRepository;
        this.workforceEntryRepository = workforceEntryRepository;
        this.equipmentUsageRepository = equipmentUsageRepository;
        this.activityRepository = activityRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.materialReceivedRepository = materialReceivedRepository;
        this.mediaRepository = mediaRepository;
        this.attachmentRepository = attachmentRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.equipmentRepository = equipmentRepository;
        this.materialRepository = materialRepository;
        this.materialService = materialService;
        this.purchaseRequestRepository = purchaseRequestRepository;
        this.purchaseRequestInvoiceRepository = purchaseRequestInvoiceRepository;
        this.dailyReportService = dailyReportService;
        this.storageService = storageService;
        this.brandingService = brandingService;
    }

    public byte[] generate(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        ConstructionSite site = siteRepository
                .findById(report.getConstructionSiteId())
                .orElseThrow(() -> new ConstructionSiteNotFoundException(report.getConstructionSiteId()));
        siteAccessService.requireAccess(site.getId(), actingUserId);

        String html = buildHtml(report, site);
        return renderPdf(html);
    }

    private String buildHtml(DailyReport report, ConstructionSite site) {
        List<DailyReportWorkforceEntry> workforce = workforceEntryRepository.findByDailyReportId(report.getId());
        List<DailyReportEquipmentUsage> equipmentUsage = equipmentUsageRepository.findByDailyReportId(report.getId());
        List<DailyReportActivity> activities = activityRepository.findByDailyReportId(report.getId());
        List<DailyReportOccurrence> occurrences = occurrenceRepository.findByDailyReportId(report.getId());
        List<DailyReportMaterialReceived> materialsReceived =
                materialReceivedRepository.findByDailyReportId(report.getId());
        List<DailyReportMedia> media = mediaRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId());
        List<DailyReportAttachment> attachments =
                attachmentRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId());
        List<Material> deliveredMaterials = findDeliveredMaterials(report);
        Map<UUID, MaterialService.SourcePurchaseRequestRef> deliveredMaterialSources =
                materialService.resolveSourcePurchaseRequests(deliveredMaterials);
        List<DailyReportImportedInvoiceResponse> importedInvoices = findImportedInvoices(report);
        List<DailyReportApprovalResponse> approvals = dailyReportService.listApprovalResponses(report.getId());

        Map<UUID, Equipment> equipmentById = equipmentRepository.findAllById(
                        equipmentUsage.stream().map(DailyReportEquipmentUsage::getEquipmentId)
                                .filter(Objects::nonNull).toList())
                .stream()
                .collect(Collectors.toMap(Equipment::getId, e -> e));

        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        html.append("<style>").append(PdfBrandingService.STYLE).append(EXTRA_STYLE).append("</style></head><body>");

        html.append(brandingService.renderHeaderHtml(brandingService.resolve(site.getId())));

        html.append("<table class=\"title-table\"><tr>")
                .append("<td><span class=\"report-title\">Diário de Obra — ")
                .append(REPORT_DATE_FORMAT.format(report.getReportDate())).append("</span></td>")
                .append("<td style=\"text-align:right\">").append(statusPill(report.getStatus())).append("</td>")
                .append("</tr></table>");

        html.append("<table class=\"meta-grid\"><tr>");
        appendMetaCell(html, "Clima manhã", report.getWeatherConditionMorning());
        appendMetaCell(html, "Clima tarde", report.getWeatherConditionAfternoon());
        appendMetaCell(html, "Expediente", workHoursLabel(report));
        appendMetaCell(html, "Tarefas bloqueadas", Boolean.TRUE.equals(report.getWeatherBlockedTasks()) ? "Sim" : "Não");
        html.append("</tr></table>");

        if (report.getComments() != null && !report.getComments().isBlank()) {
            html.append("<p class=\"comments-box\">").append(escape(report.getComments())).append("</p>");
        }

        html.append("<h2>Mão de obra</h2><table class=\"data\"><tr><th>Função</th><th class=\"right\">Qtd.</th></tr>");
        for (DailyReportWorkforceEntry entry : workforce) {
            html.append("<tr><td>").append(escape(entry.getRoleDescription())).append("</td><td class=\"right\">")
                    .append(entry.getHeadcount()).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Equipamentos utilizados</h2><table class=\"data\"><tr><th>Equipamento</th><th>Observação</th></tr>");
        for (DailyReportEquipmentUsage usage : equipmentUsage) {
            String equipmentLabel;
            if (usage.getEquipmentId() != null) {
                Equipment eq = equipmentById.get(usage.getEquipmentId());
                equipmentLabel = escape(eq != null ? eq.getName() : usage.getEquipmentId().toString());
            } else {
                equipmentLabel = escape(usage.getCustomName())
                        + " <span class=\"tag-note\">(não cadastrado)</span>";
            }
            html.append("<tr><td>").append(equipmentLabel).append("</td><td>")
                    .append(escape(usage.getStatusNote())).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Atividades</h2><table class=\"data\"><tr><th>Descrição</th><th class=\"right\">Status</th></tr>");
        for (DailyReportActivity activity : activities) {
            html.append("<tr><td>").append(escape(activity.getDescription())).append("</td><td class=\"right\">")
                    .append(activityStatusLabel(activity.getStatus())).append("</td></tr>");
        }
        html.append("</table>");

        if (!occurrences.isEmpty()) {
            html.append("<h2>Ocorrências</h2>");
            for (DailyReportOccurrence occurrence : occurrences) {
                html.append("<p>- ").append(escape(occurrence.getDescription())).append("</p>");
            }
        }

        html.append("<h2>Materiais recebidos</h2><table class=\"data\"><tr><th>Material</th><th>Origem</th><th class=\"right\">Status</th></tr>");
        for (DailyReportMaterialReceived received : materialsReceived) {
            html.append("<tr><td>").append(escape(received.getMaterialName())).append(" (")
                    .append(received.getQuantity())
                    .append(received.getUnit() != null ? " " + escape(received.getUnit()) : "")
                    .append(")</td><td class=\"muted\">Registro manual</td><td class=\"right muted\">—</td></tr>");
        }
        for (Material material : deliveredMaterials) {
            var ref = deliveredMaterialSources.get(material.getId());
            html.append("<tr><td>").append(escape(material.getName())).append(" (").append(material.getQuantity())
                    .append(material.getType() != null ? " " + escape(material.getType()) : "").append(")</td><td>")
                    .append(ref != null && ref.name() != null
                            ? "<span class=\"tag-link\">" + escape(ref.name()) + "</span>" : "—")
                    .append("</td><td class=\"right\">").append(materialDeliveryStatusLabel(material.getStatus()))
                    .append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Fotos</h2>");
        appendMediaGrid(html, media);

        html.append("<h2>Anexos</h2><table class=\"data\"><tr><th>Arquivo</th><th class=\"right\">Enviado em</th></tr>");
        for (DailyReportAttachment attachment : attachments) {
            html.append("<tr><td>").append(escape(attachment.getOriginalName())).append("</td><td class=\"right muted\">")
                    .append(DATETIME_FORMAT.format(attachment.getCreatedAt().atZone(ZoneOffset.UTC))).append("</td></tr>");
        }
        for (DailyReportImportedInvoiceResponse invoice : importedInvoices) {
            html.append("<tr><td>").append(escape(invoice.originalName()))
                    .append(invoice.purchaseRequestName() != null
                            ? " <span class=\"tag-link\">· Importado do " + escape(invoice.purchaseRequestName()) + "</span>"
                            : " <span class=\"tag-note\">(Importado do Pedido de Compra)</span>")
                    .append("</td><td class=\"right muted\">")
                    .append(DATETIME_FORMAT.format(invoice.uploadedAt().atZone(ZoneOffset.UTC))).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Aprovações</h2><table class=\"data\"><tr><th>Etapa</th><th>Responsável</th><th class=\"right\">Decisão</th></tr>");
        for (DailyReportApprovalResponse approval : approvals) {
            html.append("<tr><td>").append(functionLabel(approval.approverFunction())).append("</td><td>")
                    .append(approval.decidedByName() != null ? escape(approval.decidedByName()) : "—")
                    .append("</td><td class=\"right\">").append(approvalStatusLabel(approval)).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<p class=\"footer-note\">Gerado em ")
                .append(DATETIME_FORMAT.format(Instant.now().atZone(ZoneOffset.UTC))).append("</p>");

        html.append("</body></html>");
        return html.toString();
    }

    /** Mirrors {@code DailyReportService#listDeliveredMaterials}'s query directly (rather than calling that permission-gated method) so PDF generation keeps its existing, coarser access check unchanged. */
    private List<Material> findDeliveredMaterials(DailyReport report) {
        ZoneId zone = ZoneId.systemDefault();
        Instant startInclusive = report.getReportDate().atStartOfDay(zone).toInstant();
        Instant endExclusive = report.getReportDate().plusDays(1).atStartOfDay(zone).toInstant();
        return materialRepository.findByConstructionSiteIdAndDeliveredAtBetween(
                report.getConstructionSiteId(), startInclusive, endExclusive);
    }

    /** Mirrors {@code DailyReportService#listImportedInvoices}'s query directly — same rationale as {@link #findDeliveredMaterials}. */
    private List<DailyReportImportedInvoiceResponse> findImportedInvoices(DailyReport report) {
        List<UUID> purchaseRequestIds = purchaseRequestRepository
                .findByConstructionSiteId(report.getConstructionSiteId())
                .stream()
                .map(PurchaseRequest::getId)
                .toList();
        if (purchaseRequestIds.isEmpty()) {
            return List.of();
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant startInclusive = report.getReportDate().atStartOfDay(zone).toInstant();
        Instant endExclusive = report.getReportDate().plusDays(1).atStartOfDay(zone).toInstant();
        List<PurchaseRequestInvoice> invoices = purchaseRequestInvoiceRepository
                .findByPurchaseRequestIdInAndCreatedAtBetween(purchaseRequestIds, startInclusive, endExclusive);
        Map<UUID, String> namesById = purchaseRequestRepository
                .findAllById(invoices.stream().map(PurchaseRequestInvoice::getPurchaseRequestId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(PurchaseRequest::getId, PurchaseRequest::getName));
        return invoices.stream()
                .map(invoice -> DailyReportImportedInvoiceResponse.from(invoice, namesById.get(invoice.getPurchaseRequestId())))
                .toList();
    }

    private void appendMetaCell(StringBuilder html, String label, String value) {
        html.append("<td><span class=\"meta-label\">").append(escape(label).toUpperCase(Locale.ROOT))
                .append("</span><span class=\"meta-value\">").append(escape(value)).append("</span></td>");
    }

    private String workHoursLabel(DailyReport report) {
        if (report.getWorkHoursStart() == null && report.getWorkHoursEnd() == null) {
            return null;
        }
        return (report.getWorkHoursStart() != null ? report.getWorkHoursStart() : "—") + " – "
                + (report.getWorkHoursEnd() != null ? report.getWorkHoursEnd() : "—");
    }

    private void appendMediaGrid(StringBuilder html, List<DailyReportMedia> media) {
        if (media.isEmpty()) {
            return;
        }
        html.append("<table class=\"photo-grid\">");
        for (int i = 0; i < media.size(); i++) {
            if (i % 4 == 0) {
                if (i > 0) {
                    html.append("</tr>");
                }
                html.append("<tr>");
            }
            DailyReportMedia item = media.get(i);
            html.append("<td>");
            if (item.getType() == MediaType.PHOTO) {
                byte[] bytes = storageService.getObject(item.getStorageKey());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                html.append("<img class=\"photo\" src=\"data:")
                        .append(item.getContentType() != null ? item.getContentType() : "image/jpeg")
                        .append(";base64,").append(base64).append("\" />");
            } else {
                html.append("<div class=\"video-placeholder\">[Vídeo]</div>");
            }
            if (item.getCaption() != null && !item.getCaption().isBlank()) {
                html.append("<div class=\"photo-caption\">").append(escape(item.getCaption())).append("</div>");
            }
            html.append("</td>");
        }
        html.append("</tr></table>");
    }

    private String statusPill(DailyReportStatus status) {
        String css = switch (status) {
            case APPROVED -> "status-approved";
            case PENDING_APPROVAL -> "status-pending";
            case DRAFT -> "status-draft";
        };
        String label = switch (status) {
            case APPROVED -> "Aprovado";
            case PENDING_APPROVAL -> "Pendente de aprovação";
            case DRAFT -> "Rascunho";
        };
        return "<span class=\"status-pill " + css + "\">" + label + "</span>";
    }

    private String activityStatusLabel(ActivityStatus status) {
        return status == ActivityStatus.COMPLETED
                ? "<span style=\"color:#047857;font-weight:700;\">Concluída</span>"
                : "<span style=\"color:#4e546e;font-weight:700;\">Em andamento</span>";
    }

    private String materialDeliveryStatusLabel(MaterialDeliveryStatus status) {
        return switch (status) {
            case DELIVERED_AND_CHECKED -> "<span style=\"color:#047857;font-weight:700;\">Conferido</span>";
            case DELIVERED -> "<span style=\"color:#92400e;font-weight:700;\">Entregue</span>";
            case AWAITING_DELIVERY -> "<span class=\"muted\">Aguardando entrega</span>";
        };
    }

    private String approvalStatusLabel(DailyReportApprovalResponse approval) {
        return switch (approval.status()) {
            case APPROVED -> "<span style=\"color:#047857;font-weight:700;\">Aprovado"
                    + timestampSuffix(approval) + "</span>";
            case REJECTED -> "<span style=\"color:#b42318;font-weight:700;\">Rejeitado" + timestampSuffix(approval) + "</span>";
            case PENDING -> "<span class=\"muted\">Pendente</span>";
        };
    }

    private String timestampSuffix(DailyReportApprovalResponse approval) {
        if (approval.decidedAt() == null) {
            return "";
        }
        return " — " + DATETIME_FORMAT.format(approval.decidedAt().atZone(ZoneOffset.UTC));
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
            throw new IllegalStateException("Failed to render daily report PDF", e);
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
