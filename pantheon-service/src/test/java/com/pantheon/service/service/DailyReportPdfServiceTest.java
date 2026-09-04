package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.ActivityStatus;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
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
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import com.pantheon.service.storage.StorageService;
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
    private ProjectMembershipRepository membershipRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private StorageService storageService;

    private DailyReportPdfService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new DailyReportPdfService(
                dailyReportRepository, workforceEntryRepository, equipmentUsageRepository, activityRepository,
                occurrenceRepository, materialReceivedRepository, mediaRepository, attachmentRepository,
                signatureRepository, siteRepository, membershipRepository, equipmentRepository, materialRepository,
                storageService);
    }

    @Test
    void generateProducesAValidPdfForAFullyPopulatedReport() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ConstructionSite site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Torre Norte", "Av. Central, 500", LocalDate.now(), null, userId,
                Instant.now());
        DailyReport report = new DailyReport(UUID.randomUUID(), site.getId(), LocalDate.now(), 1, userId, Instant.now());
        report.updateCore("Ensolarado", false, null, null, "Dia produtivo", Instant.now());
        report.submit(Instant.now());

        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(membership));

        when(workforceEntryRepository.findByDailyReportId(report.getId())).thenReturn(List.of(
                new DailyReportWorkforceEntry(UUID.randomUUID(), report.getId(), null, "Pedreiro", 4, Instant.now())));
        lenient().when(equipmentUsageRepository.findByDailyReportId(report.getId())).thenReturn(List.of());
        when(activityRepository.findByDailyReportId(report.getId())).thenReturn(List.of(new DailyReportActivity(
                UUID.randomUUID(), report.getId(), "Fundação", "60%", ActivityStatus.IN_PROGRESS, Instant.now())));
        when(occurrenceRepository.findByDailyReportId(report.getId()))
                .thenReturn(List.of(new DailyReportOccurrence(UUID.randomUUID(), report.getId(), "Chuva forte", Instant.now())));
        lenient().when(materialReceivedRepository.findByDailyReportId(report.getId())).thenReturn(List.of());

        DailyReportMedia photo = new DailyReportMedia(
                UUID.randomUUID(), report.getId(), MediaType.PHOTO, "some/key.jpg", "image/jpeg", "Fundação", userId,
                Instant.now());
        when(mediaRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId())).thenReturn(List.of(photo));
        when(storageService.getObject("some/key.jpg")).thenReturn(new byte[] {1, 2, 3});

        lenient().when(attachmentRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId())).thenReturn(List.of());
        when(signatureRepository.findByDailyReportIdOrderBySignedAtAsc(report.getId())).thenReturn(List.of(
                new DailyReportSignature(
                        UUID.randomUUID(), report.getId(), membership.getId(), null, Instant.now())));

        lenient().when(equipmentRepository.findAllById(List.of())).thenReturn(List.of());
        lenient().when(materialRepository.findAllById(List.of())).thenReturn(List.of());

        byte[] pdf = pdfService.generate(report.getId(), userId);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
