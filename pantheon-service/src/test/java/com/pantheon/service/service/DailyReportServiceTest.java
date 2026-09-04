package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.ActivityRequest;
import com.pantheon.service.dto.DailyReportCoreUpdateRequest;
import com.pantheon.service.dto.EquipmentUsageRequest;
import com.pantheon.service.dto.MaterialReceivedRequest;
import com.pantheon.service.dto.OccurrenceRequest;
import com.pantheon.service.dto.WorkforceEntryRequest;
import com.pantheon.service.entity.ActivityStatus;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportActivity;
import com.pantheon.service.entity.DailyReportEquipmentUsage;
import com.pantheon.service.entity.DailyReportMaterialReceived;
import com.pantheon.service.entity.DailyReportOccurrence;
import com.pantheon.service.entity.DailyReportStatus;
import com.pantheon.service.entity.DailyReportWorkforceEntry;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.DailyReportNotEditableException;
import com.pantheon.service.exception.DuplicateDailyReportException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportActivityRepository;
import com.pantheon.service.repository.DailyReportEquipmentUsageRepository;
import com.pantheon.service.repository.DailyReportMaterialReceivedRepository;
import com.pantheon.service.repository.DailyReportOccurrenceRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportWorkforceEntryRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private DailyReportService dailyReportService;

    private UUID projectId;
    private UUID userId;
    private ConstructionSite site;

    @BeforeEach
    void setUp() {
        dailyReportService = new DailyReportService(
                dailyReportRepository, workforceEntryRepository, equipmentUsageRepository, activityRepository,
                occurrenceRepository, materialReceivedRepository, siteRepository, membershipRepository);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Torre Norte", "Av. Central, 500", LocalDate.now(), null,
                userId, Instant.now());

        lenient().when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        lenient().when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(membership));

        lenient().when(dailyReportRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(workforceEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(equipmentUsageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(occurrenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(materialReceivedRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private DailyReport draftReport() {
        return new DailyReport(UUID.randomUUID(), site.getId(), LocalDate.now(), 1, userId, Instant.now());
    }

    @Test
    void createPersistsReportInDraftWithNextSequenceNumber() {
        when(dailyReportRepository.findByConstructionSiteIdAndReportDate(site.getId(), LocalDate.of(2026, 9, 1)))
                .thenReturn(Optional.empty());
        when(dailyReportRepository.countByConstructionSiteId(site.getId())).thenReturn(3L);

        DailyReport report = dailyReportService.create(site.getId(), userId, LocalDate.of(2026, 9, 1));

        assertThat(report.getStatus()).isEqualTo(DailyReportStatus.DRAFT);
        assertThat(report.getSequenceNo()).isEqualTo(4);
    }

    @Test
    void createRejectedForDuplicateDate() {
        LocalDate date = LocalDate.of(2026, 9, 1);
        when(dailyReportRepository.findByConstructionSiteIdAndReportDate(site.getId(), date))
                .thenReturn(Optional.of(draftReport()));

        assertThatThrownBy(() -> dailyReportService.create(site.getId(), userId, date))
                .isInstanceOf(DuplicateDailyReportException.class);
    }

    @Test
    void createRejectedForNonMember() {
        UUID outsiderId = UUID.randomUUID();
        when(membershipRepository.findByProjectIdAndUserId(projectId, outsiderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dailyReportService.create(site.getId(), outsiderId, LocalDate.now()))
                .isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    void updateCoreAppliesOnlyProvidedFields() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReport updated = dailyReportService.updateCore(
                report.getId(), userId, new DailyReportCoreUpdateRequest("Ensolarado", false, null, null, null));

        assertThat(updated.getWeatherCondition()).isEqualTo("Ensolarado");
        assertThat(updated.getWeatherBlockedTasks()).isFalse();
        assertThat(updated.getWorkHoursStart()).isNull();
        assertThat(updated.getComments()).isNull();
    }

    @Test
    void updateCoreRejectedWhenSubmitted() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.updateCore(
                        report.getId(), userId, new DailyReportCoreUpdateRequest("Chuvoso", true, null, null, null)))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void submitTransitionsToSubmittedStatus() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReport submitted = dailyReportService.submit(report.getId(), userId);

        assertThat(submitted.getStatus()).isEqualTo(DailyReportStatus.SUBMITTED);
        assertThat(submitted.getSubmittedAt()).isNotNull();
    }

    @Test
    void submitRejectedWhenAlreadySubmitted() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.submit(report.getId(), userId))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void addWorkforceEntryWithExplicitDescription() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportWorkforceEntry entry = dailyReportService.addWorkforceEntry(
                report.getId(), userId, new WorkforceEntryRequest(null, "Pedreiro", 3));

        assertThat(entry.getRoleDescription()).isEqualTo("Pedreiro");
        assertThat(entry.getHeadcount()).isEqualTo(3);
    }

    @Test
    void addWorkforceEntryPreFillsFromMembershipSpecialty() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        UUID membershipId = UUID.randomUUID();
        ProjectMembership serviceProvider = new ProjectMembership(
                membershipId, projectId, UUID.randomUUID(), ProjectRole.MEMBER, ConstructionFunction.SERVICE_PROVIDER,
                "Pintor", Instant.now());
        when(membershipRepository.findById(membershipId)).thenReturn(Optional.of(serviceProvider));

        DailyReportWorkforceEntry entry =
                dailyReportService.addWorkforceEntry(report.getId(), userId, new WorkforceEntryRequest(membershipId, null, 2));

        assertThat(entry.getRoleDescription()).isEqualTo("Pintor");
        assertThat(entry.getMembershipId()).isEqualTo(membershipId);
    }

    @Test
    void addWorkforceEntryRejectedOnSubmittedReport() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.addWorkforceEntry(
                        report.getId(), userId, new WorkforceEntryRequest(null, "Pedreiro", 1)))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void addAndListEquipmentUsage() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        UUID equipmentId = UUID.randomUUID();

        DailyReportEquipmentUsage usage = dailyReportService.addEquipmentUsage(
                report.getId(), userId, new EquipmentUsageRequest(equipmentId, "Operando normalmente"));

        assertThat(usage.getEquipmentId()).isEqualTo(equipmentId);

        when(equipmentUsageRepository.findByDailyReportId(report.getId())).thenReturn(java.util.List.of(usage));
        assertThat(dailyReportService.listEquipmentUsage(report.getId(), userId)).containsExactly(usage);
    }

    @Test
    void addEquipmentUsageRejectedOnSubmittedReport() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.addEquipmentUsage(
                        report.getId(), userId, new EquipmentUsageRequest(UUID.randomUUID(), null)))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void addAndListActivity() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportActivity activity = dailyReportService.addActivity(
                report.getId(), userId, new ActivityRequest("Fundação", "60%", ActivityStatus.IN_PROGRESS));

        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.IN_PROGRESS);

        when(activityRepository.findByDailyReportId(report.getId())).thenReturn(java.util.List.of(activity));
        assertThat(dailyReportService.listActivities(report.getId(), userId)).containsExactly(activity);
    }

    @Test
    void addActivityRejectedOnSubmittedReport() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.addActivity(
                        report.getId(), userId, new ActivityRequest("Fundação", "60%", ActivityStatus.IN_PROGRESS)))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void addAndListOccurrence() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReportOccurrence occurrence =
                dailyReportService.addOccurrence(report.getId(), userId, new OccurrenceRequest("Chuva forte à tarde"));

        assertThat(occurrence.getDescription()).isEqualTo("Chuva forte à tarde");

        when(occurrenceRepository.findByDailyReportId(report.getId())).thenReturn(java.util.List.of(occurrence));
        assertThat(dailyReportService.listOccurrences(report.getId(), userId)).containsExactly(occurrence);
    }

    @Test
    void addOccurrenceRejectedOnSubmittedReport() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.addOccurrence(report.getId(), userId, new OccurrenceRequest("X")))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void addAndListMaterialReceived() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        UUID materialId = UUID.randomUUID();

        DailyReportMaterialReceived received = dailyReportService.addMaterialReceived(
                report.getId(), userId, new MaterialReceivedRequest(materialId, new BigDecimal("10.5")));

        assertThat(received.getQuantity()).isEqualByComparingTo("10.5");

        when(materialReceivedRepository.findByDailyReportId(report.getId())).thenReturn(java.util.List.of(received));
        assertThat(dailyReportService.listMaterialsReceived(report.getId(), userId)).containsExactly(received);
    }

    @Test
    void addMaterialReceivedRejectedOnSubmittedReport() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> dailyReportService.addMaterialReceived(
                        report.getId(), userId, new MaterialReceivedRequest(UUID.randomUUID(), BigDecimal.ONE)))
                .isInstanceOf(DailyReportNotEditableException.class);
    }

    @Test
    void listReportsOrdersImplicitlyByRepositoryQuery() {
        when(dailyReportRepository.findByConstructionSiteIdOrderByReportDateDesc(site.getId()))
                .thenReturn(java.util.List.of(draftReport()));

        assertThat(dailyReportService.list(site.getId(), userId)).hasSize(1);
    }

    @Test
    void workHoursOnlyUpdateLeavesWeatherUntouched() {
        DailyReport report = draftReport();
        report.updateCore("Ensolarado", true, null, null, null, Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));

        DailyReport updated = dailyReportService.updateCore(
                report.getId(), userId,
                new DailyReportCoreUpdateRequest(null, null, LocalTime.of(7, 0), LocalTime.of(17, 0), null));

        assertThat(updated.getWeatherCondition()).isEqualTo("Ensolarado");
        assertThat(updated.getWorkHoursStart()).isEqualTo(LocalTime.of(7, 0));
    }
}
