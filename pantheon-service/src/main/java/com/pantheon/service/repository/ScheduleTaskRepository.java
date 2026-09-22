package com.pantheon.service.repository;

import com.pantheon.service.entity.ScheduleTask;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTaskRepository extends JpaRepository<ScheduleTask, UUID> {

    List<ScheduleTask> findByStageIdOrderBySortOrderAsc(UUID stageId);

    List<ScheduleTask> findByStageIdIn(Collection<UUID> stageIds);

    long countByStageId(UUID stageId);
}
