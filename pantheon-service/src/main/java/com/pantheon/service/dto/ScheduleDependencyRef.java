package com.pantheon.service.dto;

import com.pantheon.service.entity.ScheduleTaskDependency;
import java.util.UUID;

/** A dependency as seen from its successor task: the dependency row's own id (needed to unlink
 * it via {@code DELETE /api/schedule-tasks/{id}/dependencies/{depId}}) plus which task it depends on. */
public record ScheduleDependencyRef(UUID id, UUID predecessorTaskId) {

    public static ScheduleDependencyRef from(ScheduleTaskDependency dependency) {
        return new ScheduleDependencyRef(dependency.getId(), dependency.getPredecessorTaskId());
    }
}
