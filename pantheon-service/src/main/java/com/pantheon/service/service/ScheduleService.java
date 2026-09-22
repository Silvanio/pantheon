package com.pantheon.service.service;

import com.pantheon.service.dto.ScheduleDependencyRef;
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
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DuplicateDependencyException;
import com.pantheon.service.exception.ScheduleStageNotFoundException;
import com.pantheon.service.exception.ScheduleTaskNotFoundException;
import com.pantheon.service.exception.SelfDependencyException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.ScheduleStageRepository;
import com.pantheon.service.repository.ScheduleTaskDependencyRepository;
import com.pantheon.service.repository.ScheduleTaskRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleStageRepository stageRepository;
    private final ScheduleTaskRepository taskRepository;
    private final ScheduleTaskDependencyRepository dependencyRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;
    private final SitePermissionService permissionService;

    public ScheduleService(
            ScheduleStageRepository stageRepository,
            ScheduleTaskRepository taskRepository,
            ScheduleTaskDependencyRepository dependencyRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService) {
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
    }

    public List<ScheduleStageResponse> listStages(UUID siteId, UUID actingUserId) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.SCHEDULE);

        List<ScheduleStage> stages = stageRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId);
        List<UUID> stageIds = stages.stream().map(ScheduleStage::getId).toList();
        Map<UUID, List<ScheduleTask>> tasksByStage = taskRepository.findByStageIdIn(stageIds).stream()
                .collect(Collectors.groupingBy(ScheduleTask::getStageId));
        List<UUID> taskIds = tasksByStage.values().stream().flatMap(List::stream).map(ScheduleTask::getId).toList();
        Map<UUID, List<ScheduleDependencyRef>> dependsOnByTask = dependencyRepository
                .findByPredecessorTaskIdInOrSuccessorTaskIdIn(taskIds, taskIds).stream()
                .collect(Collectors.groupingBy(
                        ScheduleTaskDependency::getSuccessorTaskId,
                        Collectors.mapping(ScheduleDependencyRef::from, Collectors.toList())));

        return stages.stream()
                .map(stage -> {
                    List<ScheduleTaskResponse> taskResponses = tasksByStage
                            .getOrDefault(stage.getId(), List.of())
                            .stream()
                            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                            .map(task -> ScheduleTaskResponse.from(
                                    task, dependsOnByTask.getOrDefault(task.getId(), List.of())))
                            .toList();
                    return ScheduleStageResponse.from(stage, taskResponses);
                })
                .toList();
    }

    @Transactional
    public ScheduleStageResponse createStage(UUID siteId, UUID actingUserId, ScheduleStageCreationRequest request) {
        requireSite(siteId);
        requireManage(siteId, actingUserId);

        long nextSortOrder = stageRepository.countByConstructionSiteId(siteId);
        ScheduleStage stage = stageRepository.save(new ScheduleStage(
                UUID.randomUUID(), siteId, request.name(), request.color(), request.startDate(), request.endDate(),
                (int) nextSortOrder, Instant.now()));
        return ScheduleStageResponse.from(stage, List.of());
    }

    @Transactional
    public ScheduleStageResponse updateStage(UUID stageId, UUID actingUserId, ScheduleStageUpdateRequest request) {
        ScheduleStage stage = requireStage(stageId);
        requireManage(stage.getConstructionSiteId(), actingUserId);
        stage.update(request.name(), request.color(), request.startDate(), request.endDate(), request.sortOrder(), Instant.now());
        stageRepository.save(stage);
        return toStageResponse(stage);
    }

    @Transactional
    public void deleteStage(UUID stageId, UUID actingUserId) {
        ScheduleStage stage = requireStage(stageId);
        requireManage(stage.getConstructionSiteId(), actingUserId);

        List<ScheduleTask> tasks = taskRepository.findByStageIdOrderBySortOrderAsc(stageId);
        List<UUID> taskIds = tasks.stream().map(ScheduleTask::getId).toList();
        if (!taskIds.isEmpty()) {
            dependencyRepository.deleteAll(
                    dependencyRepository.findByPredecessorTaskIdInOrSuccessorTaskIdIn(taskIds, taskIds));
            taskRepository.deleteAll(tasks);
        }
        stageRepository.delete(stage);
    }

    @Transactional
    public ScheduleTaskResponse createTask(UUID stageId, UUID actingUserId, ScheduleTaskCreationRequest request) {
        ScheduleStage stage = requireStage(stageId);
        requireManage(stage.getConstructionSiteId(), actingUserId);

        long nextSortOrder = taskRepository.countByStageId(stageId);
        ScheduleTask task = taskRepository.save(new ScheduleTask(
                UUID.randomUUID(), stageId, request.title(), request.startDate(), request.endDate(),
                request.responsibleSiteMembershipId(), 0, (int) nextSortOrder, Instant.now()));
        return ScheduleTaskResponse.from(task, List.of());
    }

    @Transactional
    public ScheduleTaskResponse updateTask(UUID taskId, UUID actingUserId, ScheduleTaskUpdateRequest request) {
        ScheduleTask task = requireTask(taskId);
        ScheduleStage stage = requireStage(task.getStageId());
        requireManage(stage.getConstructionSiteId(), actingUserId);
        task.update(
                request.title(), request.startDate(), request.endDate(), request.responsibleSiteMembershipId(),
                request.percentComplete(), request.sortOrder(), request.clearResponsible(), Instant.now());
        taskRepository.save(task);
        return toTaskResponse(task);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID actingUserId) {
        ScheduleTask task = requireTask(taskId);
        ScheduleStage stage = requireStage(task.getStageId());
        requireManage(stage.getConstructionSiteId(), actingUserId);

        dependencyRepository.deleteAll(
                dependencyRepository.findByPredecessorTaskIdOrSuccessorTaskId(taskId, taskId));
        taskRepository.delete(task);
    }

    @Transactional
    public ScheduleTaskDependency linkDependency(UUID successorTaskId, UUID actingUserId, ScheduleDependencyRequest request) {
        ScheduleTask successor = requireTask(successorTaskId);
        ScheduleStage stage = requireStage(successor.getStageId());
        requireManage(stage.getConstructionSiteId(), actingUserId);

        UUID predecessorTaskId = request.predecessorTaskId();
        if (predecessorTaskId.equals(successorTaskId)) {
            throw new SelfDependencyException(successorTaskId);
        }
        requireTask(predecessorTaskId);
        if (dependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(predecessorTaskId, successorTaskId)) {
            throw new DuplicateDependencyException(predecessorTaskId, successorTaskId);
        }

        return dependencyRepository.save(
                new ScheduleTaskDependency(UUID.randomUUID(), predecessorTaskId, successorTaskId, Instant.now()));
    }

    @Transactional
    public void unlinkDependency(UUID dependencyId, UUID actingUserId) {
        ScheduleTaskDependency dependency = dependencyRepository
                .findById(dependencyId)
                .orElseThrow(() -> new ScheduleTaskNotFoundException(dependencyId));
        ScheduleTask successor = requireTask(dependency.getSuccessorTaskId());
        ScheduleStage stage = requireStage(successor.getStageId());
        requireManage(stage.getConstructionSiteId(), actingUserId);
        dependencyRepository.delete(dependency);
    }

    /** Average {@code percent_complete} across every task on the site, rounded; {@code null} when there are none. */
    public Integer computeProgress(UUID siteId) {
        List<UUID> stageIds =
                stageRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId).stream().map(ScheduleStage::getId).toList();
        if (stageIds.isEmpty()) {
            return null;
        }
        List<ScheduleTask> tasks = taskRepository.findByStageIdIn(stageIds);
        if (tasks.isEmpty()) {
            return null;
        }
        double average = tasks.stream().mapToInt(ScheduleTask::getPercentComplete).average().orElse(0);
        return (int) Math.round(average);
    }

    private ScheduleStageResponse toStageResponse(ScheduleStage stage) {
        List<ScheduleTask> tasks = taskRepository.findByStageIdOrderBySortOrderAsc(stage.getId());
        List<ScheduleTaskResponse> taskResponses = tasks.stream().map(this::toTaskResponse).toList();
        return ScheduleStageResponse.from(stage, taskResponses);
    }

    private ScheduleTaskResponse toTaskResponse(ScheduleTask task) {
        List<ScheduleDependencyRef> dependsOn = dependencyRepository
                .findByPredecessorTaskIdOrSuccessorTaskId(task.getId(), task.getId())
                .stream()
                .filter(dep -> dep.getSuccessorTaskId().equals(task.getId()))
                .map(ScheduleDependencyRef::from)
                .toList();
        return ScheduleTaskResponse.from(task, dependsOn);
    }

    private void requireManage(UUID siteId, UUID actingUserId) {
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireManage(siteId, access, PermissionCapability.SCHEDULE);
    }

    private ScheduleStage requireStage(UUID stageId) {
        return stageRepository.findById(stageId).orElseThrow(() -> new ScheduleStageNotFoundException(stageId));
    }

    private ScheduleTask requireTask(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new ScheduleTaskNotFoundException(taskId));
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }
}
