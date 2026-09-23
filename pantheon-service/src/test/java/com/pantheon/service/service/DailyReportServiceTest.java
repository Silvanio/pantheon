package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.exception.DailyReportNotDeletableException;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportEquipmentUsageRepository;
import com.pantheon.service.repository.DailyReportMaterialReceivedRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportOccurrenceRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.storage.StorageService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {

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
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private StorageService storageService;

    private DailyReportService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new DailyReportService(
                dailyReportRepository, workforceEntryRepository, equipmentUsageRepository, activityRepository,
                occurrenceRepository, materialReceivedRepository, mediaRepository, attachmentRepository, siteRepository,
                siteMembershipRepository, siteAccessService, permissionService, storageService);

        siteId = UUID.randomUUID();
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
    }

    private com.pantheon.service.entity.ConstructionSite site(UUID id) {
        return new com.pantheon.service.entity.ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private DailyReport draftReport() {
        return new DailyReport(UUID.randomUUID(), siteId, LocalDate.now(), 1, UUID.randomUUID(), Instant.now());
    }

    @Test
    void deleteRemovesDraftReportAndItsChildRows() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        service.delete(report.getId(), UUID.randomUUID());

        verify(dailyReportRepository).delete(report);
        verify(permissionService).requireManage(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT));
    }

    @Test
    void deleteRejectsWhenNotDraft() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.delete(report.getId(), UUID.randomUUID()))
                .isInstanceOf(DailyReportNotDeletableException.class);
        verify(dailyReportRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsMemberWithoutManageAccess() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.DAILY_REPORT))
                .when(permissionService)
                .requireManage(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT));

        assertThatThrownBy(() -> service.delete(report.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(dailyReportRepository, never()).delete(any());
    }

    @Test
    void deleteAlsoDeletesAttachedMediaAndAttachmentStorageObjectsAndRows() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), report.getId(), MediaType.PHOTO, "media/key.jpg", "image/jpeg", null,
                UUID.randomUUID(), Instant.now());
        DailyReportAttachment attachment = new DailyReportAttachment(
                UUID.randomUUID(), report.getId(), "attachment/key.pdf", "application/pdf", "arquivo.pdf",
                UUID.randomUUID(), Instant.now());
        when(mediaRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId())).thenReturn(List.of(media));
        when(attachmentRepository.findByDailyReportIdOrderByCreatedAtAsc(report.getId())).thenReturn(List.of(attachment));

        service.delete(report.getId(), UUID.randomUUID());

        verify(storageService).deleteObject("media/key.jpg");
        verify(storageService).deleteObject("attachment/key.pdf");
        verify(mediaRepository).deleteAll(List.of(media));
        verify(attachmentRepository).deleteAll(List.of(attachment));
    }

    @Test
    void listReturnsPagedResultsSortedByCreatedAtDescending() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findByConstructionSiteId(eq(siteId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 1));

        Page<DailyReport> result = service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(report);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(dailyReportRepository).findByConstructionSiteId(eq(siteId), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void listRejectsMemberWithHiddenAccess() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.DAILY_REPORT))
                .when(permissionService)
                .requireVisible(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT));

        assertThatThrownBy(() -> service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenCapabilityException.class);
    }
}
