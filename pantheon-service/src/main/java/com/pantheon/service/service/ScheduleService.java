package com.pantheon.service.service;

import com.pantheon.service.dto.ScheduleDependencyRef;
import com.pantheon.service.dto.ScheduleDependencyRequest;
import com.pantheon.service.dto.ScheduleStageCreationRequest;
import com.pantheon.service.dto.ScheduleStageResponse;
import com.pantheon.service.dto.ScheduleStageUpdateRequest;
import com.pantheon.service.dto.ScheduleTaskCreationRequest;
import com.pantheon.service.dto.ScheduleTaskResponse;
import com.pantheon.service.dto.ScheduleTaskUpdateRequest;
import com.pantheon.service.dto.TaskCardCreationRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.ScheduleStage;
import com.pantheon.service.entity.ScheduleTask;
import com.pantheon.service.entity.ScheduleTaskDependency;
import com.pantheon.service.entity.TaskCard;
import com.pantheon.service.entity.TaskColumn;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DuplicateDependencyException;
import com.pantheon.service.exception.NoTaskColumnsAvailableException;
import com.pantheon.service.exception.ScheduleStageNotFoundException;
import com.pantheon.service.exception.ScheduleTaskAlreadyLinkedException;
import com.pantheon.service.exception.ScheduleTaskNotFoundException;
import com.pantheon.service.exception.SelfDependencyException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.ScheduleStageRepository;
import com.pantheon.service.repository.ScheduleTaskDependencyRepository;
import com.pantheon.service.repository.ScheduleTaskRepository;
import com.pantheon.service.repository.TaskCardRepository;
import com.pantheon.service.repository.TaskColumnRepository;
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
    private final TaskCardRepository taskCardRepository;
    private final TaskColumnRepository taskColumnRepository;
    private final TaskCardService taskCardService;

    public ScheduleService(
            ScheduleStageRepository stageRepository,
            ScheduleTaskRepository taskRepository,
            ScheduleTaskDependencyRepository dependencyRepository,
            ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService,
            SitePermissionService permissionService,
            TaskCardRepository taskCardRepository,
            TaskColumnRepository taskColumnRepository,
            TaskCardService taskCardService) {
        this.stageRepository = stageRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
        this.permissionService = permissionService;
        this.taskCardRepository = taskCardRepository;
        this.taskColumnRepository = taskColumnRepository;
        this.taskCardService = taskCardService;
    }

    public List<ScheduleStageResponse> listStages(UUID siteId, UUID actingUserId) {
        requireSite(siteId);
        var access = siteAccessService.requireAccess(siteId, actingUserId);
        permissionService.requireVisible(siteId, access, PermissionCapability.SCHEDULE);

        List<ScheduleStage> stages = stageRepository.findByConstructionSiteIdOrderBySortOrderAsc(siteId);
        List<UUID> stageIds = stages.stream().map(ScheduleStage::getId).toList();
        Map<UUID, List<ScheduleTask>> tasksByStage = taskRepository.findByStageIdIn(stageIds).stream()
                .collect(Collectors.groupingBy(ScheduleTask::getStageId));
        List<ScheduleTask> allTasks = tasksByStage.values().stream().flatMap(List::stream).toList();
        List<UUID> taskIds = allTasks.stream().map(ScheduleTask::getId).toList();
        Map<UUID, List<ScheduleDependencyRef>> dependsOnByTask = dependencyRepository
                .findByPredecessorTaskIdInOrSuccessorTaskIdIn(taskIds, taskIds).stream()
                .collect(Collectors.groupingBy(
                        ScheduleTaskDependency::getSuccessorTaskId,
                        Collectors.mapping(ScheduleDependencyRef::from, Collectors.toList())));
        Map<UUID, String> taskCardTitles = taskCardTitles(allTasks);

        return stages.stream()
                .map(stage -> {
                    List<ScheduleTaskResponse> taskResponses = tasksByStage
                            .getOrDefault(stage.getId(), List.of())
                            .stream()
                            .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                            .map(task -> ScheduleTaskResponse.from(
                                    task, dependsOnByTask.getOrDefault(task.getId(), List.of()),
                                    taskCardTitles.get(task.getTaskCardId())))
                            .toList();
                    return ScheduleStageResponse.from(stage, taskResponses);
                })
                .toList();
    }

    /** Batch-resolves linked Tasks-board card titles for a set of schedule tasks — a task with no
     * link, or whose linked card was deleted independently, simply gets no entry (map lookup miss). */
    private Map<UUID, String> taskCardTitles(List<ScheduleTask> tasks) {
        // A plain (mutable) map, never `Map.of()` — this gets looked up with `task.getTaskCardId()`,
        // which is null for the (common) unlinked case, and Map.of()'s null-hostile `get` would throw.
        List<UUID> taskCardIds = tasks.stream().map(ScheduleTask::getTaskCardId).filter(id -> id != null).toList();
        return taskCardRepository.findAllById(taskCardIds).stream()
                .collect(Collectors.toMap(TaskCard::getId, TaskCard::getTitle));
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
        return ScheduleTaskResponse.from(task, List.of(), null);
    }

    @Transactional
    public ScheduleTaskResponse updateTask(UUID taskId, UUID actingUserId, ScheduleTaskUpdateRequest request) {
        ScheduleTask task = requireTask(taskId);
        ScheduleStage stage = requireStage(task.getStageId());
        requireManage(stage.getConstructionSiteId(), actingUserId);
        task.update(
                request.title(), request.startDate(), request.endDate(), request.responsibleSiteMembershipId(),
                request.percentComplete(), request.sortOrder(), Boolean.TRUE.equals(request.clearResponsible()),
                Instant.now());
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

    /** Creates a new Tasks-board card from this schedule task (title + due date carried over,
     * placed in the site's company's first task column), assigns the schedule task's responsible
     * site membership to the new card if one is set, and links the two — see design.md's
     * "reuse TaskCardService.createCard" decision. Requires MANAGE on both SCHEDULE (checked here)
     * and TASKS (checked inside TaskCardService.createCard/assign themselves). */
    @Transactional
    public ScheduleTaskResponse createLinkedTask(UUID scheduleTaskId, UUID actingUserId) {
        ScheduleTask task = requireTask(scheduleTaskId);
        ScheduleStage stage = requireStage(task.getStageId());
        UUID siteId = stage.getConstructionSiteId();
        requireManage(siteId, actingUserId);

        if (task.getTaskCardId() != null) {
            throw new ScheduleTaskAlreadyLinkedException(scheduleTaskId);
        }

        ConstructionSite site = requireSite(siteId);
        List<TaskColumn> columns = taskColumnRepository.findByCompanyIdOrderBySortOrderAsc(site.getCompanyId());
        if (columns.isEmpty()) {
            throw new NoTaskColumnsAvailableException(site.getCompanyId());
        }

        TaskCard card = taskCardService.createCard(
                siteId, actingUserId,
                new TaskCardCreationRequest(columns.get(0).getId(), task.getTitle(), null, task.getEndDate()));

        if (task.getResponsibleSiteMembershipId() != null) {
            taskCardService.assign(card.getId(), actingUserId, task.getResponsibleSiteMembershipId());
        }

        task.linkTask(card.getId(), Instant.now());
        taskRepository.save(task);
        return toTaskResponse(task, card.getTitle());
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
        Map<UUID, String> taskCardTitles = taskCardTitles(tasks);
        List<ScheduleTaskResponse> taskResponses =
                tasks.stream().map(task -> toTaskResponse(task, taskCardTitles.get(task.getTaskCardId()))).toList();
        return ScheduleStageResponse.from(stage, taskResponses);
    }

    private ScheduleTaskResponse toTaskResponse(ScheduleTask task) {
        String taskCardTitle = task.getTaskCardId() == null
                ? null
                : taskCardRepository.findById(task.getTaskCardId()).map(TaskCard::getTitle).orElse(null);
        return toTaskResponse(task, taskCardTitle);
    }

    private ScheduleTaskResponse toTaskResponse(ScheduleTask task, String taskCardTitle) {
        List<ScheduleDependencyRef> dependsOn = dependencyRepository
                .findByPredecessorTaskIdOrSuccessorTaskId(task.getId(), task.getId())
                .stream()
                .filter(dep -> dep.getSuccessorTaskId().equals(task.getId()))
                .map(ScheduleDependencyRef::from)
                .toList();
        return ScheduleTaskResponse.from(task, dependsOn, taskCardTitle);
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
