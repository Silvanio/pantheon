package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
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
class DailyReportPdfServiceTest {

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private DailyReportWorkforceEntryRepository workforceEntryRepository;

    @Mock
    private DailyReportEquipmentUsageRepository equipmentUsageRepository;

    @Mock
    private DailyReportActivityRepository activityRepository;

    @Mock
    private DailyReportOccurrenceRepository occurrenceRepository;

    @Mock
    private DailyReportMaterialReceivedRepository materialReceivedRepository;

    @Mock
    private DailyReportMediaRepository mediaRepository;

    @Mock
    private DailyReportAttachmentRepository attachmentRepository;

    @Mock
    private DailyReportSignatureRepository signatureRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private PdfBrandingService brandingService;

    private DailyReportPdfService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new DailyReportPdfService(
                dailyReportRepository, workforceEntryRepository, equipmentUsageRepository, activityRepository,
                occurrenceRepository, materialReceivedRepository, mediaRepository, attachmentRepository,
                signatureRepository, siteRepository, siteAccessService, equipmentRepository, storageService,
                brandingService);
        siteId = UUID.randomUUID();
    }

    private String textOf(byte[] pdf) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void generateShowsBrandingHeader() throws IOException {
        DailyReport report = new DailyReport(UUID.randomUUID(), siteId, LocalDate.now(), 1, UUID.randomUUID(), Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        ConstructionSite site = new ConstructionSite(
                siteId, UUID.randomUUID(), "Obra Central", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
        when(brandingService.resolve(eq(siteId)))
                .thenReturn(new PdfBrandingService.Branding("Empresa Teste", "Obra Central", null));
        when(brandingService.renderHeaderHtml(any()))
                .thenReturn("<div class=\"pdf-header\">Empresa Teste — Obra Central</div>");

        byte[] pdf = service.generate(report.getId(), UUID.randomUUID());

        String text = textOf(pdf);
        assertThat(text).contains("Empresa Teste");
        assertThat(text).contains("Obra Central");
    }
}
