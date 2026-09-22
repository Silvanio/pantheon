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
import com.pantheon.service.exception.DuplicateDependencyException;
import com.pantheon.service.exception.ForbiddenCapabilityException;
import com.pantheon.service.exception.SelfDependencyException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.ScheduleStageRepository;
import com.pantheon.service.repository.ScheduleTaskDependencyRepository;
import com.pantheon.service.repository.ScheduleTaskRepository;
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

    private ScheduleService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new ScheduleService(
                stageRepository, taskRepository, dependencyRepository, siteRepository, siteAccessService,
                permissionService);
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
