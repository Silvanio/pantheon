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

import com.pantheon.service.dto.ScheduleDependencyRequest;
import com.pantheon.service.dto.ScheduleStageCreationRequest;
import com.pantheon.service.dto.ScheduleStageResponse;
import com.pantheon.service.dto.ScheduleStageUpdateRequest;
import com.pantheon.service.dto.ScheduleTaskCreationRequest;
import com.pantheon.service.dto.ScheduleTaskResponse;
import com.pantheon.service.dto.ScheduleTaskUpdateRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.ScheduleStage;
import com.pantheon.service.entity.ScheduleTask;
import com.pantheon.service.entity.ScheduleTaskDependency;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.DuplicateDependencyException;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.NoTaskColumnsAvailableException;
import com.pantheon.service.exception.ScheduleTaskAlreadyLinkedException;
import com.pantheon.service.exception.SelfDependencyException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.ScheduleStageRepository;
import com.pantheon.service.repository.ScheduleTaskDependencyRepository;
import com.pantheon.service.repository.ScheduleTaskRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
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
class ScheduleServiceTest {

    @Mock
    private ScheduleStageRepository stageRepository;

    @Mock
    private ScheduleTaskRepository taskRepository;

    @Mock
    private ScheduleTaskDependencyRepository dependencyRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    @Mock
    private SitePermissionService permissionService;

    @Mock
    private TaskCardRepository taskCardRepository;

    @Mock
    private TaskColumnRepository taskColumnRepository;

    @Mock
    private TaskCardService taskCardService;

    private ScheduleService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(
                stageRepository, taskRepository, dependencyRepository, siteRepository, siteAccessService,
                permissionService, taskCardRepository, taskColumnRepository, taskCardService);
        siteId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(site(siteId)));
        lenient().when(siteAccessService.requireAccess(eq(siteId), any())).thenReturn(new SiteAccessContext(true, null));
        lenient().when(stageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(dependencyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ConstructionSite site(UUID id) {
        return new ConstructionSite(
                id, UUID.randomUUID(), "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now());
    }

    private ScheduleStage stage() {
        return new ScheduleStage(
                UUID.randomUUID(), siteId, "Fundação", "blueprint", LocalDate.now(), LocalDate.now().plusDays(10), 0,
                Instant.now());
    }

    private ScheduleTask task(UUID stageId, int percentComplete) {
        return new ScheduleTask(
                UUID.randomUUID(), stageId, "Escavação", LocalDate.now(), LocalDate.now().plusDays(3), null,
                percentComplete, 0, Instant.now());
    }

    @Test
    void createStageRejectsMemberWithoutManageAccess() {
        doThrow(new ForbiddenCapabilityException(siteId, PermissionCapability.SCHEDULE))
                .when(permissionService)
                .requireManage(eq(siteId), any(), eq(PermissionCapability.SCHEDULE));

        assertThatThrownBy(() -> service.createStage(
                        siteId, UUID.randomUUID(),
                        new ScheduleStageCreationRequest("Fundação", "blueprint", LocalDate.now(), LocalDate.now().plusDays(5))))
                .isInstanceOf(ForbiddenCapabilityException.class);
        verify(stageRepository, never()).save(any());
    }

    @Test
    void createStageAssignsNextSortOrder() {
        when(stageRepository.countByConstructionSiteId(siteId)).thenReturn(2L);

        ScheduleStageResponse response = service.createStage(
                siteId, UUID.randomUUID(),
                new ScheduleStageCreationRequest("Fundação", "blueprint", LocalDate.now(), LocalDate.now().plusDays(5)));

        assertThat(response.sortOrder()).isEqualTo(2);
        assertThat(response.percentComplete()).isEqualTo(0);
    }

    @Test
    void stagePercentCompleteIsAverageOfItsTasks() {
        ScheduleStage stage = stage();
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(taskRepository.findByStageIdOrderBySortOrderAsc(stage.getId()))
                .thenReturn(List.of(task(stage.getId(), 40), task(stage.getId(), 60)));
        when(dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(any(), any())).thenReturn(List.of());

        ScheduleStageResponse response = service.updateStage(stage.getId(), UUID.randomUUID(), new ScheduleStageUpdateRequest(null, null, null, null, null));

        assertThat(response.percentComplete()).isEqualTo(50);
    }

    @Test
    void emptyStageHasZeroPercentComplete() {
        ScheduleStage stage = stage();
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(taskRepository.findByStageIdOrderBySortOrderAsc(stage.getId())).thenReturn(List.of());

        ScheduleStageResponse response = service.updateStage(stage.getId(), UUID.randomUUID(), new ScheduleStageUpdateRequest(null, null, null, null, null));

        assertThat(response.percentComplete()).isEqualTo(0);
    }

    @Test
    void deleteStageCascadesToItsTasksAndDependencies() {
        ScheduleStage stage = stage();
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        ScheduleTask taskA = task(stage.getId(), 0);
        ScheduleTask taskB = task(stage.getId(), 0);
        List<ScheduleTask> tasks = List.of(taskA, taskB);
        when(taskRepository.findByStageIdOrderBySortOrderAsc(stage.getId())).thenReturn(tasks);
        List<UUID> taskIds = List.of(taskA.getId(), taskB.getId());
        ScheduleTaskDependency dependency =
                new ScheduleTaskDependency(UUID.randomUUID(), taskA.getId(), taskB.getId(), Instant.now());
        when(dependencyRepository.findByPredecessorTaskIdInOrSuccessorTaskIdIn(taskIds, taskIds))
                .thenReturn(List.of(dependency));

        service.deleteStage(stage.getId(), UUID.randomUUID());

        verify(dependencyRepository).deleteAll(List.of(dependency));
        verify(taskRepository).deleteAll(tasks);
        verify(stageRepository).delete(stage);
    }

    @Test
    void createTaskAssignsNextSortOrderAndZeroPercent() {
        ScheduleStage stage = stage();
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(taskRepository.countByStageId(stage.getId())).thenReturn(1L);

        ScheduleTaskResponse response = service.createTask(
                stage.getId(), UUID.randomUUID(),
                new ScheduleTaskCreationRequest("Escavação", LocalDate.now(), LocalDate.now().plusDays(3), null));

        assertThat(response.sortOrder()).isEqualTo(1);
        assertThat(response.percentComplete()).isEqualTo(0);
    }

    /** Regression test: {@code ScheduleTaskUpdateRequest.clearResponsible} is a boxed {@code Boolean}
     * (not a primitive) specifically so a request that omits it — like the Gantt checkbox's
     * percent-only PATCH — deserializes without error instead of failing on a missing primitive. */
    @Test
    void updateTaskWithOmittedClearResponsibleDoesNotClearResponsible() {
        ScheduleStage stage = stage();
        UUID responsibleId = UUID.randomUUID();
        ScheduleTask task = new ScheduleTask(
                UUID.randomUUID(), stage.getId(), "Escavação", LocalDate.now(), LocalDate.now().plusDays(3),
                responsibleId, 0, 0, Instant.now());
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(any(), any())).thenReturn(List.of());

        ScheduleTaskResponse response = service.updateTask(
                task.getId(), UUID.randomUUID(),
                new ScheduleTaskUpdateRequest(null, null, null, null, null, 100, null));

        assertThat(response.percentComplete()).isEqualTo(100);
        assertThat(response.responsibleSiteMembershipId()).isEqualTo(responsibleId);
    }

    @Test
    void updateTaskWithClearResponsibleTrueClearsResponsible() {
        ScheduleStage stage = stage();
        ScheduleTask task = new ScheduleTask(
                UUID.randomUUID(), stage.getId(), "Escavação", LocalDate.now(), LocalDate.now().plusDays(3),
                UUID.randomUUID(), 0, 0, Instant.now());
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(any(), any())).thenReturn(List.of());

        ScheduleTaskResponse response = service.updateTask(
                task.getId(), UUID.randomUUID(),
                new ScheduleTaskUpdateRequest(null, null, null, null, true, null, null));

        assertThat(response.responsibleSiteMembershipId()).isNull();
    }

    @Test
    void deleteTaskAlsoDeletesItsDependencies() {
        ScheduleStage stage = stage();
        ScheduleTask task = task(stage.getId(), 0);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        ScheduleTaskDependency dependency =
                new ScheduleTaskDependency(UUID.randomUUID(), task.getId(), UUID.randomUUID(), Instant.now());
        when(dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(task.getId(), task.getId()))
                .thenReturn(List.of(dependency));

        service.deleteTask(task.getId(), UUID.randomUUID());

        verify(dependencyRepository).deleteAll(List.of(dependency));
        verify(taskRepository).delete(task);
    }

    @Test
    void createLinkedTaskCreatesCardInFirstCompanyColumnAndLinksIt() {
        ScheduleStage stage = stage();
        ScheduleTask task = task(stage.getId(), 0);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        ConstructionSite site = site(stage.getConstructionSiteId());
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        TaskColumn firstColumn = new TaskColumn(UUID.randomUUID(), site.getCompanyId(), "A fazer", 0, Instant.now());
        TaskColumn secondColumn = new TaskColumn(UUID.randomUUID(), site.getCompanyId(), "Feito", 1, Instant.now());
        when(taskColumnRepository.findByCompanyIdOrderBySortOrderAsc(site.getCompanyId()))
                .thenReturn(List.of(firstColumn, secondColumn));
        TaskCard card = new TaskCard(
                UUID.randomUUID(), site.getId(), firstColumn.getId(), task.getTitle(), null, task.getEndDate(), 0,
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(taskCardService.createCard(eq(site.getId()), any(), any())).thenReturn(card);
        when(dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(any(), any())).thenReturn(List.of());

        ScheduleTaskResponse response = service.createLinkedTask(task.getId(), UUID.randomUUID());

        assertThat(response.taskCardId()).isEqualTo(card.getId());
        assertThat(response.taskCardTitle()).isEqualTo(task.getTitle());
        verify(taskCardService).createCard(eq(site.getId()), any(), argThat(
                request -> request.columnId().equals(firstColumn.getId()) && request.title().equals(task.getTitle())
                        && request.dueDate().equals(task.getEndDate())));
        verify(taskCardService, never()).assign(any(), any(), any());
    }

    @Test
    void createLinkedTaskAssignsCardToTaskResponsibleWhenSet() {
        ScheduleStage stage = stage();
        UUID responsibleSiteMembershipId = UUID.randomUUID();
        ScheduleTask task = new ScheduleTask(
                UUID.randomUUID(), stage.getId(), "Escavação", LocalDate.now(), LocalDate.now().plusDays(3),
                responsibleSiteMembershipId, 0, 0, Instant.now());
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        ConstructionSite site = site(stage.getConstructionSiteId());
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        TaskColumn firstColumn = new TaskColumn(UUID.randomUUID(), site.getCompanyId(), "A fazer", 0, Instant.now());
        when(taskColumnRepository.findByCompanyIdOrderBySortOrderAsc(site.getCompanyId()))
                .thenReturn(List.of(firstColumn));
        TaskCard card = new TaskCard(
                UUID.randomUUID(), site.getId(), firstColumn.getId(), task.getTitle(), null, task.getEndDate(), 0,
                UUID.randomUUID(), Instant.now(), Instant.now());
        when(taskCardService.createCard(eq(site.getId()), any(), any())).thenReturn(card);
        when(dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(any(), any())).thenReturn(List.of());
        UUID actingUserId = UUID.randomUUID();

        service.createLinkedTask(task.getId(), actingUserId);

        verify(taskCardService).assign(card.getId(), actingUserId, responsibleSiteMembershipId);
    }

    @Test
    void createLinkedTaskRejectsWhenAlreadyLinked() {
        ScheduleStage stage = stage();
        ScheduleTask task = task(stage.getId(), 0);
        task.linkTask(UUID.randomUUID(), Instant.now());
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));

        assertThatThrownBy(() -> service.createLinkedTask(task.getId(), UUID.randomUUID()))
                .isInstanceOf(ScheduleTaskAlreadyLinkedException.class);
        verify(taskCardService, never()).createCard(any(), any(), any());
    }

    @Test
    void createLinkedTaskRejectsWhenCompanyHasNoTaskColumns() {
        ScheduleStage stage = stage();
        ScheduleTask task = task(stage.getId(), 0);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        ConstructionSite site = site(stage.getConstructionSiteId());
        when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        when(taskColumnRepository.findByCompanyIdOrderBySortOrderAsc(site.getCompanyId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createLinkedTask(task.getId(), UUID.randomUUID()))
                .isInstanceOf(NoTaskColumnsAvailableException.class);
        verify(taskCardService, never()).createCard(any(), any(), any());
    }

    @Test
    void linkDependencyRejectsSelfLink() {
        ScheduleStage stage = stage();
        ScheduleTask task = task(stage.getId(), 0);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));

        assertThatThrownBy(() -> service.linkDependency(task.getId(), UUID.randomUUID(), new ScheduleDependencyRequest(task.getId())))
                .isInstanceOf(SelfDependencyException.class);
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    void linkDependencyRejectsDuplicateLink() {
        ScheduleStage stage = stage();
        ScheduleTask predecessor = task(stage.getId(), 0);
        ScheduleTask successor = task(stage.getId(), 0);
        when(taskRepository.findById(successor.getId())).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessor.getId())).thenReturn(Optional.of(predecessor));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(dependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessor.getId(), successor.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.linkDependency(
                        successor.getId(), UUID.randomUUID(), new ScheduleDependencyRequest(predecessor.getId())))
                .isInstanceOf(DuplicateDependencyException.class);
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    void linkDependencySucceedsForTwoDistinctTasks() {
        ScheduleStage stage = stage();
        ScheduleTask predecessor = task(stage.getId(), 0);
        ScheduleTask successor = task(stage.getId(), 0);
        when(taskRepository.findById(successor.getId())).thenReturn(Optional.of(successor));
        when(taskRepository.findById(predecessor.getId())).thenReturn(Optional.of(predecessor));
        when(stageRepository.findById(stage.getId())).thenReturn(Optional.of(stage));
        when(dependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessor.getId(), successor.getId()))
                .thenReturn(false);

        ScheduleTaskDependency result = service.linkDependency(
                successor.getId(), UUID.randomUUID(), new ScheduleDependencyRequest(predecessor.getId()));

        assertThat(result.getPredecessorTaskId()).isEqualTo(predecessor.getId());
        assertThat(result.getSuccessorTaskId()).isEqualTo(successor.getId());
    }

    @Test
    void computeProgressReturnsNullWhenSiteHasNoStages() {
        when(stageRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId)).thenReturn(List.of());

        assertThat(service.computeProgress(siteId)).isNull();
    }

    @Test
    void computeProgressReturnsNullWhenStagesHaveNoTasks() {
        ScheduleStage stage = stage();
        when(stageRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId)).thenReturn(List.of(stage));
        when(taskRepository.findByStageIdIn(List.of(stage.getId()))).thenReturn(List.of());

        assertThat(service.computeProgress(siteId)).isNull();
    }

    @Test
    void computeProgressAveragesAllTasksOnTheSite() {
        ScheduleStage stage = stage();
        when(stageRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId)).thenReturn(List.of(stage));
        when(taskRepository.findByStageIdIn(List.of(stage.getId())))
                .thenReturn(List.of(task(stage.getId(), 20), task(stage.getId(), 50)));

        assertThat(service.computeProgress(siteId)).isEqualTo(35);
    }
}
