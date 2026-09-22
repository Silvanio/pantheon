package com.pantheon.service.repository;

import com.pantheon.service.entity.ScheduleTaskDependency;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTaskDependencyRepository extends JpaRepository<ScheduleTaskDependency, UUID> {

    List<ScheduleTaskDependency> findByPredecessorTaskIdOrSuccessorTaskId(UUID predecessorTaskId, UUID successorTaskId);

    List<ScheduleTaskDependency> findByPredecessorTaskIdInOrSuccessorTaskIdIn(
            Collection<UUID> predecessorTaskIds, Collection<UUID> successorTaskIds);

    boolean existsByPredecessorTaskIdAndSuccessorTaskId(UUID predecessorTaskId, UUID successorTaskId);
}
