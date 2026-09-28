package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.DailyReportCoreUpdateRequest;
import com.pantheon.service.dto.EquipmentUsageRequest;
import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.ActivityStatus;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportApproval;
import com.pantheon.service.entity.DailyReportApprovalStatus;
import com.pantheon.service.entity.DailyReportAttachment;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMedia;
import com.pantheon.service.entity.DailyReportStatus;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.Material;
import com.pantheon.service.entity.MediaType;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestInvoice;
import com.pantheon.service.entity.SiteDailyReportApprovalLevel;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.DailyReportCoreFieldsRequiredException;
import com.pantheon.service.exception.DailyReportNotDeletableException;
import com.pantheon.service.exception.DailyReportNotEditableException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.EquipmentReferenceRequiredException;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.NotCurrentDailyReportApprovalStepException;
import com.pantheon.service.repository.AppUserRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportApprovalRepository;
import com.pantheon.service.repository.DailyReportAttachmentRepository;
import com.pantheon.service.repository.DailyReportEquipmentUsageRepository;
import com.pantheon.service.repository.DailyReportMaterialReceivedRepository;
import com.pantheon.service.repository.DailyReportMediaRepository;
import com.pantheon.service.repository.DailyReportOccurrenceRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.MaterialRepository;
import com.pantheon.service.repository.PurchaseRequestInvoiceRepository;
import com.pantheon.service.repository.PurchaseRequestRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import com.pantheon.service.storage.StorageService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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
    private DailyReportApprovalRepository approvalRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private MaterialService materialService;

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private PurchaseRequestInvoiceRepository purchaseRequestInvoiceRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private SiteDailyReportApprovalLevelService approvalLevelService;

    @Mock
    private StorageService storageService;

    private DailyReportService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new DailyReportService(
                dailyReportRepository, workforceEntryRepository, equipmentUsageRepository, activityRepository,
                occurrenceRepository, materialReceivedRepository, mediaRepository, attachmentRepository,
                approvalRepository, siteRepository, siteMembershipRepository, userRepository, materialRepository,
                materialService, purchaseRequestRepository, purchaseRequestInvoiceRepository, siteAccessService,
                permissionService, approvalLevelService, storageService);

        siteId = UUID.randomUUID();
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(dailyReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(approvalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private SiteMembership activeMember(UUID userId, ConstructionFunction function) {
        SiteMembership member =
                SiteMembership.invited(UUID.randomUUID(), siteId, userId, function, null, null, Instant.now());
        member.accept();
        return member;
    }

    private com.pantheon.service.entity.ConstructionSite site(UUID id) {
        return new com.pantheon.service.entity.ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private DailyReport draftReport() {
        return new DailyReport(UUID.randomUUID(), siteId, LocalDate.now(), 1, UUID.randomUUID(), Instant.now());
    }

    @Test
    void updateCoreSavesWhenWeatherAndHoursArePresent() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        lenient().when(dailyReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DailyReport result = service.updateCore(report.getId(), UUID.randomUUID(), new DailyReportCoreUpdateRequest(
                "SUNNY", "CLOUDY", true, LocalTime.of(8, 0), LocalTime.of(17, 0), null));

        assertThat(result.getWeatherConditionMorning()).isEqualTo("SUNNY");
        assertThat(result.getWeatherConditionAfternoon()).isEqualTo("CLOUDY");
        verify(dailyReportRepository).save(report);
    }

    @Test
    void updateCoreRejectsWhenMorningWeatherMissing() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateCore(report.getId(), UUID.randomUUID(), new DailyReportCoreUpdateRequest(
                null, "SUNNY", true, LocalTime.of(8, 0), LocalTime.of(17, 0), null)))
                .isInstanceOf(DailyReportCoreFieldsRequiredException.class);
        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void updateCoreRejectsWhenAfternoonWeatherMissing() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateCore(report.getId(), UUID.randomUUID(), new DailyReportCoreUpdateRequest(
                "SUNNY", null, true, LocalTime.of(8, 0), LocalTime.of(17, 0), null)))
                .isInstanceOf(DailyReportCoreFieldsRequiredException.class);
        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void updateCoreRejectsWhenWorkHoursMissing() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateCore(report.getId(), UUID.randomUUID(), new DailyReportCoreUpdateRequest(
                "SUNNY", "CLOUDY", true, null, null, null)))
                .isInstanceOf(DailyReportCoreFieldsRequiredException.class);
        verify(dailyReportRepository, never()).save(any());
    }

    @Test
    void updateCoreRejectsSavingCommentsAloneOnAFreshReport() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.updateCore(report.getId(), UUID.randomUUID(), new DailyReportCoreUpdateRequest(
                null, null, null, null, null, "Sem intercorrências")))
                .isInstanceOf(DailyReportCoreFieldsRequiredException.class);
        verify(dailyReportRepository, never()).save(any());
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

    @Test
    void listFiltersToApprovedOnlyForViewOnlyMember() {
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(AccessLevel.VIEW);
        DailyReport approved = draftReport();
        when(dailyReportRepository.findByConstructionSiteIdAndStatus(
                eq(siteId), eq(DailyReportStatus.APPROVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(approved), PageRequest.of(0, 20), 1));

        Page<DailyReport> result = service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(approved);
        verify(dailyReportRepository, never()).findByConstructionSiteId(any(), any());
    }

    @Test
    void listReturnsEveryStatusForViewAndApproveMember() {
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);
        DailyReport report = draftReport();
        when(dailyReportRepository.findByConstructionSiteId(eq(siteId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 1));

        Page<DailyReport> result = service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(report);
        verify(dailyReportRepository, never()).findByConstructionSiteIdAndStatus(any(), any(), any());
    }

    @Test
    void listReturnsEveryStatusForManageMember() {
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(AccessLevel.MANAGE);
        DailyReport report = draftReport();
        when(dailyReportRepository.findByConstructionSiteId(eq(siteId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 1));

        Page<DailyReport> result = service.list(siteId, UUID.randomUUID(), PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(report);
        verify(dailyReportRepository, never()).findByConstructionSiteIdAndStatus(any(), any(), any());
    }

    @Test
    void viewOnlyMemberCannotAccessNonApprovedReportDirectly() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(AccessLevel.VIEW);

        assertThatThrownBy(() -> service.listWorkforceEntries(report.getId(), UUID.randomUUID()))
                .isInstanceOf(DailyReportNotFoundException.class);
    }

    @Test
    void viewOnlyMemberCanAccessApprovedReportDirectly() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        report.approve(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(AccessLevel.VIEW);
        when(workforceEntryRepository.findByDailyReportId(report.getId())).thenReturn(List.of());

        List<DailyReportWorkforceEntry> result = service.listWorkforceEntries(report.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void manageOrViewAndApproveMemberCanAccessNonApprovedReportDirectly() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(permissionService.resolve(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT)))
                .thenReturn(AccessLevel.VIEW_AND_APPROVE);
        when(workforceEntryRepository.findByDailyReportId(report.getId())).thenReturn(List.of());

        List<DailyReportWorkforceEntry> result = service.listWorkforceEntries(report.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void submitTransitionsToPendingApprovalAndCreatesStepsFromDefaultLevel() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(approvalLevelService.getEffectiveLevels(siteId)).thenReturn(List.of(new SiteDailyReportApprovalLevel(
                UUID.randomUUID(), siteId, 1, ConstructionFunction.ENGINEER, Instant.now())));

        DailyReport result = service.submit(report.getId(), UUID.randomUUID());

        assertThat(result.getStatus()).isEqualTo(DailyReportStatus.PENDING_APPROVAL);
        assertThat(result.getCurrentApprovalCycle()).isEqualTo(1);
        verify(approvalRepository).save(
                argThat(a -> a.getStepOrder() == 1 && a.getApproverFunction() == ConstructionFunction.ENGINEER
                        && a.getCycleNumber() == 1));
    }

    @Test
    void approveStepAdvancesToNextStepWithoutFinalizing() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step1 = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        DailyReportApproval step2 = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 2, ConstructionFunction.CLIENT, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING))
                .thenReturn(Optional.of(step1), Optional.of(step2));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(true);

        DailyReport result = service.approveStep(report.getId(), engineerUserId, null);

        assertThat(result.getStatus()).isEqualTo(DailyReportStatus.PENDING_APPROVAL);
        assertThat(step1.getStatus()).isEqualTo(DailyReportApprovalStatus.APPROVED);
    }

    @Test
    void approveStepFinalizesReportWhenLastStep() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval onlyStep = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING))
                .thenReturn(Optional.of(onlyStep), Optional.empty());

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(true);

        DailyReport result = service.approveStep(report.getId(), engineerUserId, null);

        assertThat(result.getStatus()).isEqualTo(DailyReportStatus.APPROVED);
    }

    @Test
    void approveStepBlocksNonMatchingFunction() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID architectUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, architectUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(architectUserId, ConstructionFunction.ARCHITECT)));

        assertThatThrownBy(() -> service.approveStep(report.getId(), architectUserId, null))
                .isInstanceOf(NotCurrentDailyReportApprovalStepException.class);
    }

    @Test
    void companyStaffWithNoSiteMembershipCanAlwaysActOnApprovalStep() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING))
                .thenReturn(Optional.of(step), Optional.empty());

        UUID staffUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, staffUserId)).thenReturn(new SiteAccessContext(true, null));
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, staffUserId)).thenReturn(Optional.empty());

        DailyReport result = service.approveStep(report.getId(), staffUserId, "ok");

        assertThat(result.getStatus()).isEqualTo(DailyReportStatus.APPROVED);
        assertThat(step.getDecidedBySiteMembershipId()).isNull();
    }

    @Test
    void companyStaffWithNonMatchingSiteMembershipIsBlockedDespiteBeingStaff() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID staffClientUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, staffClientUserId)).thenReturn(new SiteAccessContext(true, null));
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, staffClientUserId))
                .thenReturn(Optional.of(activeMember(staffClientUserId, ConstructionFunction.CLIENT)));

        assertThatThrownBy(() -> service.approveStep(report.getId(), staffClientUserId, "ok"))
                .isInstanceOf(NotCurrentDailyReportApprovalStepException.class);
    }

    @Test
    void companyStaffWithMatchingSiteMembershipApprovesAsThatMembership() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.CLIENT, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING))
                .thenReturn(Optional.of(step), Optional.empty());

        UUID staffClientUserId = UUID.randomUUID();
        SiteMembership clientMembership = activeMember(staffClientUserId, ConstructionFunction.CLIENT);
        when(siteAccessService.requireAccess(siteId, staffClientUserId)).thenReturn(new SiteAccessContext(true, null));
        when(siteMembershipRepository.findByConstructionSiteIdAndUserId(siteId, staffClientUserId))
                .thenReturn(Optional.of(clientMembership));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(true);

        DailyReport result = service.approveStep(report.getId(), staffClientUserId, "ok");

        assertThat(result.getStatus()).isEqualTo(DailyReportStatus.APPROVED);
        assertThat(step.getDecidedBySiteMembershipId()).isEqualTo(clientMembership.getId());
    }

    @Test
    void viewOnlyAccessBlocksApprovalEvenOnFunctionMatch() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(false);

        assertThatThrownBy(() -> service.approveStep(report.getId(), engineerUserId, null))
                .isInstanceOf(NotCurrentDailyReportApprovalStepException.class);
    }

    @Test
    void rejectStepRequiresAReason() {
        assertThatThrownBy(() -> service.rejectStep(UUID.randomUUID(), UUID.randomUUID(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectStepReturnsReportToDraftAndRecordsReasonOnStep() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportApproval step = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
                report.getId(), 1, DailyReportApprovalStatus.PENDING)).thenReturn(Optional.of(step));

        UUID engineerUserId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, engineerUserId))
                .thenReturn(new SiteAccessContext(false, activeMember(engineerUserId, ConstructionFunction.ENGINEER)));
        when(permissionService.canApprove(eq(siteId), any(), eq(PermissionCapability.DAILY_REPORT))).thenReturn(true);

        DailyReport result = service.rejectStep(report.getId(), engineerUserId, "Faltou assinatura do responsável");

        assertThat(result.getStatus()).isEqualTo(DailyReportStatus.DRAFT);
        assertThat(step.getStatus()).isEqualTo(DailyReportApprovalStatus.REJECTED);
        assertThat(step.getComment()).isEqualTo("Faltou assinatura do responsável");
    }

    @Test
    void addEquipmentUsageRejectsWhenNeitherEquipmentIdNorCustomNameGiven() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.addEquipmentUsage(
                report.getId(), UUID.randomUUID(), new EquipmentUsageRequest(null, "  ", "nota")))
                .isInstanceOf(EquipmentReferenceRequiredException.class);
        verify(equipmentUsageRepository, never()).save(any());
    }

    @Test
    void addEquipmentUsageAcceptsCustomNameWithoutEquipmentId() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(equipmentUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DailyReportEquipmentUsage usage = service.addEquipmentUsage(
                report.getId(), UUID.randomUUID(), new EquipmentUsageRequest(null, "Gerador alugado 15kVA", null));

        assertThat(usage.getEquipmentId()).isNull();
        assertThat(usage.getCustomName()).isEqualTo("Gerador alugado 15kVA");
    }

    @Test
    void addEquipmentUsageAcceptsRegisteredEquipmentIdWithoutCustomName() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(equipmentUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID equipmentId = UUID.randomUUID();

        DailyReportEquipmentUsage usage = service.addEquipmentUsage(
                report.getId(), UUID.randomUUID(), new EquipmentUsageRequest(equipmentId, null, null));

        assertThat(usage.getEquipmentId()).isEqualTo(equipmentId);
        assertThat(usage.getCustomName()).isNull();
    }

    @Test
    void deleteWorkforceEntryRemovesRowWhenReportIsDraft() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportWorkforceEntry entry = new DailyReportWorkforceEntry(
                UUID.randomUUID(), report.getId(), null, "Pedreiro", 2, Instant.now());
        when(workforceEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        service.deleteWorkforceEntry(report.getId(), UUID.randomUUID(), entry.getId());

        verify(workforceEntryRepository).delete(entry);
    }

    @Test
    void deleteWorkforceEntryRejectsWhenReportNotDraft() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.deleteWorkforceEntry(report.getId(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(DailyReportNotEditableException.class);
        verify(workforceEntryRepository, never()).delete(any());
    }

    @Test
    void deleteWorkforceEntryRejectsWhenEntryBelongsToAnotherReport() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportWorkforceEntry entry = new DailyReportWorkforceEntry(
                UUID.randomUUID(), UUID.randomUUID(), null, "Pedreiro", 2, Instant.now());
        when(workforceEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.deleteWorkforceEntry(report.getId(), UUID.randomUUID(), entry.getId()))
                .isInstanceOf(DailyReportNotFoundException.class);
        verify(workforceEntryRepository, never()).delete(any());
    }

    @Test
    void deleteEquipmentUsageRemovesRowWhenReportIsDraft() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportEquipmentUsage usage = new DailyReportEquipmentUsage(
                UUID.randomUUID(), report.getId(), UUID.randomUUID(), null, null, Instant.now());
        when(equipmentUsageRepository.findById(usage.getId())).thenReturn(Optional.of(usage));

        service.deleteEquipmentUsage(report.getId(), UUID.randomUUID(), usage.getId());

        verify(equipmentUsageRepository).delete(usage);
    }

    @Test
    void deleteActivityRemovesRowWhenReportIsDraft() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportActivity activity = new DailyReportActivity(
                UUID.randomUUID(), report.getId(), "Alvenaria", "50%", ActivityStatus.IN_PROGRESS, Instant.now());
        when(activityRepository.findById(activity.getId())).thenReturn(Optional.of(activity));

        service.deleteActivity(report.getId(), UUID.randomUUID(), activity.getId());

        verify(activityRepository).delete(activity);
    }

    @Test
    void deleteMediaAlsoDeletesStorageObject() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), report.getId(), MediaType.PHOTO, "media/key.jpg", "image/jpeg", null,
                UUID.randomUUID(), Instant.now());
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));

        service.deleteMedia(report.getId(), UUID.randomUUID(), media.getId());

        verify(storageService).deleteObject("media/key.jpg");
        verify(mediaRepository).delete(media);
    }

    @Test
    void updateMediaCaptionSavesNewCaption() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportMedia media = new DailyReportMedia(
                UUID.randomUUID(), report.getId(), MediaType.PHOTO, "media/key.jpg", "image/jpeg", "old",
                UUID.randomUUID(), Instant.now());
        when(mediaRepository.findById(media.getId())).thenReturn(Optional.of(media));
        when(mediaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DailyReportMedia result = service.updateMediaCaption(report.getId(), UUID.randomUUID(), media.getId(), "novo");

        assertThat(result.getCaption()).isEqualTo("novo");
    }

    @Test
    void deleteAttachmentAlsoDeletesStorageObject() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        DailyReportAttachment attachment = new DailyReportAttachment(
                UUID.randomUUID(), report.getId(), "attachment/key.pdf", "application/pdf", "arquivo.pdf",
                UUID.randomUUID(), Instant.now());
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));

        service.deleteAttachment(report.getId(), UUID.randomUUID(), attachment.getId());

        verify(storageService).deleteObject("attachment/key.pdf");
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void listDeliveredMaterialsQueriesDayRangeAndResolvesSource() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        Material material = new Material(
                UUID.randomUUID(), siteId, UUID.randomUUID(), "Cimento CP-II 50kg", "Saco", new BigDecimal("120"),
                Instant.now());
        ZoneId zone = ZoneId.systemDefault();
        Instant start = report.getReportDate().atStartOfDay(zone).toInstant();
        Instant end = report.getReportDate().plusDays(1).atStartOfDay(zone).toInstant();
        when(materialRepository.findByConstructionSiteIdAndDeliveredAtBetween(siteId, start, end))
                .thenReturn(List.of(material));
        UUID purchaseRequestId = UUID.randomUUID();
        when(materialService.resolveSourcePurchaseRequests(List.of(material)))
                .thenReturn(Map.of(material.getId(), new MaterialService.SourcePurchaseRequestRef(purchaseRequestId, "Pedido 15/03 #231")));

        var result = service.listDeliveredMaterials(report.getId(), UUID.randomUUID());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sourcePurchaseRequestId()).isEqualTo(purchaseRequestId);
        assertThat(result.get(0).sourcePurchaseRequestName()).isEqualTo("Pedido 15/03 #231");
    }

    @Test
    void listImportedInvoicesFiltersToSiteAndDayRange() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        PurchaseRequest purchaseRequest =
                new PurchaseRequest(UUID.randomUUID(), siteId, "Pedido 15/03 #231", UUID.randomUUID(), Instant.now());
        when(purchaseRequestRepository.findByConstructionSiteId(siteId)).thenReturn(List.of(purchaseRequest));
        ZoneId zone = ZoneId.systemDefault();
        Instant start = report.getReportDate().atStartOfDay(zone).toInstant();
        Instant end = report.getReportDate().plusDays(1).atStartOfDay(zone).toInstant();
        PurchaseRequestInvoice invoice = new PurchaseRequestInvoice(
                UUID.randomUUID(), purchaseRequest.getId(), "invoices/key.pdf", "application/pdf",
                "NF-e 000.231.pdf", UUID.randomUUID(), Instant.now());
        when(purchaseRequestInvoiceRepository.findByPurchaseRequestIdInAndCreatedAtBetween(
                        List.of(purchaseRequest.getId()), start, end))
                .thenReturn(List.of(invoice));
        when(purchaseRequestRepository.findAllById(List.of(purchaseRequest.getId()))).thenReturn(List.of(purchaseRequest));

        var result = service.listImportedInvoices(report.getId(), UUID.randomUUID());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).purchaseRequestId()).isEqualTo(purchaseRequest.getId());
        assertThat(result.get(0).purchaseRequestName()).isEqualTo("Pedido 15/03 #231");
        assertThat(result.get(0).originalName()).isEqualTo("NF-e 000.231.pdf");
    }

    @Test
    void listImportedInvoicesReturnsEmptyWhenSiteHasNoPurchaseRequests() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(purchaseRequestRepository.findByConstructionSiteId(siteId)).thenReturn(List.of());

        var result = service.listImportedInvoices(report.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
        verify(purchaseRequestInvoiceRepository, never())
                .findByPurchaseRequestIdInAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    void listApprovalResponsesResolvesAccountlessMembershipDisplayName() {
        DailyReport report = draftReport();
        SiteMembership accountless = SiteMembership.accountless(
                UUID.randomUUID(), siteId, "Eletricista", "Rafael Martins", "rafael@example.com", null, null,
                Instant.now());
        DailyReportApproval approval = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.SITE_FOREMAN, Instant.now());
        approval.approve(accountless.getId(), "ok", Instant.now());
        when(approvalRepository.findByDailyReportIdOrderByCycleNumberAscStepOrderAsc(report.getId()))
                .thenReturn(List.of(approval));
        when(siteMembershipRepository.findAllById(List.of(accountless.getId()))).thenReturn(List.of(accountless));

        var result = service.listApprovalResponses(report.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).decidedByName()).isEqualTo("Rafael Martins");
    }

    @Test
    void listApprovalResponsesResolvesAccountLinkedMembershipViaAppUser() {
        DailyReport report = draftReport();
        UUID userId = UUID.randomUUID();
        SiteMembership membership = activeMember(userId, ConstructionFunction.ENGINEER);
        AppUser user = new AppUser(userId, "carla@example.com", "Carla Souza", null, null, Instant.now(), Instant.now());
        DailyReportApproval approval = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        approval.approve(membership.getId(), "ok", Instant.now());
        when(approvalRepository.findByDailyReportIdOrderByCycleNumberAscStepOrderAsc(report.getId()))
                .thenReturn(List.of(approval));
        when(siteMembershipRepository.findAllById(List.of(membership.getId()))).thenReturn(List.of(membership));
        when(userRepository.findAllById(List.of(userId))).thenReturn(List.of(user));

        var result = service.listApprovalResponses(report.getId());

        assertThat(result.get(0).decidedByName()).isEqualTo("Carla Souza");
    }

    @Test
    void listApprovalResponsesLeavesNameNullForPendingStep() {
        DailyReport report = draftReport();
        DailyReportApproval pending = new DailyReportApproval(
                UUID.randomUUID(), report.getId(), 1, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(approvalRepository.findByDailyReportIdOrderByCycleNumberAscStepOrderAsc(report.getId()))
                .thenReturn(List.of(pending));

        var result = service.listApprovalResponses(report.getId());

        assertThat(result.get(0).decidedByName()).isNull();
        assertThat(result.get(0).status()).isEqualTo(DailyReportApprovalStatus.PENDING);
    }
}
