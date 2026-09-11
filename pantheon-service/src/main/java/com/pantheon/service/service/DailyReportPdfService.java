package com.pantheon.service.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMaterialReceived;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.MediaType;
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
import com.pantheon.service.repository.DailyReportSignatureRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.EquipmentRepository;
import com.pantheon.service.storage.StorageService;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Renders a daily report to PDF on demand (never cached) — see design.md "PDF generation".
 * Server-side HTML-to-PDF via openhtmltopdf, which requires well-formed XHTML input.
 */
@Service
public class DailyReportPdfService {

    private final DailyReportRepository dailyReportRepository;
    private final DailyReportWorkforceEntryRepository workforceEntryRepository;
    private final DailyReportEquipmentUsageRepository equipmentUsageRepository;
    private final DailyReportActivityRepository activityRepository;
    private final DailyReportOccurrenceRepository occurrenceRepository;
    private final DailyReportMaterialReceivedRepository materialReceivedRepository;
    private final DailyReportMediaRepository mediaRepository;
    private final DailyReportAttachmentRepository attachmentRepository;
    private final DailyReportSignatureRepository signatureRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final EquipmentRepository equipmentRepository;
    private final StorageService storageService;

    public DailyReportPdfService(
            DailyReportRepository dailyReportRepository,
            DailyReportWorkforceEntryRepository workforceEntryRepository,
            DailyReportEquipmentUsageRepository equipmentUsageRepository,
            DailyReportActivityRepository activityRepository,
            DailyReportOccurrenceRepository occurrenceRepository,
            DailyReportMaterialReceivedRepository materialReceivedRepository,
            DailyReportMediaRepository mediaRepository,
            DailyReportAttachmentRepository attachmentRepository,
            DailyReportSignatureRepository signatureRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            EquipmentRepository equipmentRepository,
            StorageService storageService) {
        this.dailyReportRepository = dailyReportRepository;
        this.workforceEntryRepository = workforceEntryRepository;
        this.equipmentUsageRepository = equipmentUsageRepository;
        this.activityRepository = activityRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.materialReceivedRepository = materialReceivedRepository;
        this.mediaRepository = mediaRepository;
        this.attachmentRepository = attachmentRepository;
        this.signatureRepository = signatureRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.equipmentRepository = equipmentRepository;
        this.storageService = storageService;
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
        List<DailyReportSignature> signatures =
                signatureRepository.findByDailyReportIdOrderBySignedAtAsc(report.getId());

        Map<UUID, Equipment> equipmentById = equipmentRepository.findAllById(
                        equipmentUsage.stream().map(DailyReportEquipmentUsage::getEquipmentId).toList())
                .stream()
                .collect(Collectors.toMap(Equipment::getId, e -> e));
        StringBuilder html = new StringBuilder();
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/>");
        html.append("<style>");
        html.append("body{font-family:sans-serif;font-size:11px;color:#1a1a1a;}");
        html.append("h1{font-size:18px;margin-bottom:4px;} h2{font-size:13px;margin-top:16px;border-bottom:1px solid #ccc;}");
        html.append("table{width:100%;border-collapse:collapse;margin-top:4px;}");
        html.append("td,th{border:1px solid #ddd;padding:4px 6px;text-align:left;font-size:10px;}");
        html.append(".meta{color:#555;margin-bottom:8px;} .photo{max-width:220px;max-height:160px;margin:4px;border:1px solid #ccc;}");
        html.append("</style></head><body>");

        html.append("<h1>Relatório Diário de Obra #").append(report.getSequenceNo()).append("</h1>");
        html.append("<p class=\"meta\">Obra: ").append(escape(site.getName())).append(" — Data: ")
                .append(report.getReportDate()).append(" — Status: ").append(report.getStatus()).append("</p>");

        html.append("<h2>Clima</h2><p>")
                .append(escape(report.getWeatherCondition())).append(
                        Boolean.TRUE.equals(report.getWeatherBlockedTasks()) ? " (bloqueou tarefas planejadas)" : "")
                .append("</p>");

        html.append("<h2>Horário de trabalho</h2><p>")
                .append(report.getWorkHoursStart() != null ? report.getWorkHoursStart() : "—").append(" - ")
                .append(report.getWorkHoursEnd() != null ? report.getWorkHoursEnd() : "—").append("</p>");

        html.append("<h2>Mão de obra</h2><table><tr><th>Função</th><th>Quantidade</th></tr>");
        for (DailyReportWorkforceEntry entry : workforce) {
            html.append("<tr><td>").append(escape(entry.getRoleDescription())).append("</td><td>")
                    .append(entry.getHeadcount()).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Equipamentos utilizados</h2><table><tr><th>Equipamento</th><th>Observação</th></tr>");
        for (DailyReportEquipmentUsage usage : equipmentUsage) {
            Equipment eq = equipmentById.get(usage.getEquipmentId());
            html.append("<tr><td>").append(escape(eq != null ? eq.getName() : usage.getEquipmentId().toString()))
                    .append("</td><td>").append(escape(usage.getStatusNote())).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Atividades</h2><table><tr><th>Descrição</th><th>Progresso</th><th>Status</th></tr>");
        for (DailyReportActivity activity : activities) {
            html.append("<tr><td>").append(escape(activity.getDescription())).append("</td><td>")
                    .append(escape(activity.getProgressNote())).append("</td><td>")
                    .append(activity.getStatus()).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Ocorrências</h2>");
        for (DailyReportOccurrence occurrence : occurrences) {
            html.append("<p>- ").append(escape(occurrence.getDescription())).append("</p>");
        }

        html.append("<h2>Materiais recebidos</h2><table><tr><th>Material</th><th>Quantidade</th></tr>");
        for (DailyReportMaterialReceived received : materialsReceived) {
            html.append("<tr><td>").append(escape(received.getMaterialName()))
                    .append("</td><td>").append(received.getQuantity())
                    .append(received.getUnit() != null ? " " + escape(received.getUnit()) : "").append("</td></tr>");
        }
        html.append("</table>");

        html.append("<h2>Comentários</h2><p>").append(escape(report.getComments())).append("</p>");

        html.append("<h2>Mídia</h2>");
        for (DailyReportMedia item : media) {
            if (item.getType() == MediaType.PHOTO) {
                byte[] bytes = storageService.getObject(item.getStorageKey());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                html.append("<img class=\"photo\" src=\"data:")
                        .append(item.getContentType() != null ? item.getContentType() : "image/jpeg")
                        .append(";base64,").append(base64).append("\" />");
            } else {
                html.append("<p>[Vídeo] ").append(escape(item.getCaption())).append("</p>");
            }
        }

        html.append("<h2>Anexos</h2>");
        for (DailyReportAttachment attachment : attachments) {
            html.append("<p>- ").append(escape(attachment.getOriginalName())).append("</p>");
        }

        html.append("<h2>Assinaturas</h2><table><tr><th>Função</th><th>Data/hora</th></tr>");
        for (DailyReportSignature signature : signatures) {
            html.append("<tr><td>").append(signature.getFunction() != null ? signature.getFunction() : "—")
                    .append("</td><td>").append(signature.getSignedAt()).append("</td></tr>");
        }
        html.append("</table>");

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
