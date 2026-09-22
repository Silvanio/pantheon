package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A purely visual predecessor -> successor link between two {@link ScheduleTask}s — drawn as a
 * connecting line on the Gantt chart. Linking never changes either task's dates; there is no
 * automatic cascade rescheduling (see the construction-schedule spec's dependency requirement). */
@Entity
@Table(name = "schedule_task_dependency")
public class ScheduleTaskDependency {

    @Id
    private UUID id;

    @Column(name = "predecessor_task_id", nullable = false)
    private UUID predecessorTaskId;

    @Column(name = "successor_task_id", nullable = false)
    private UUID successorTaskId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScheduleTaskDependency() {
        // JPA
    }

    public ScheduleTaskDependency(UUID id, UUID predecessorTaskId, UUID successorTaskId, Instant createdAt) {
        this.id = id;
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPredecessorTaskId() {
        return predecessorTaskId;
    }

    public UUID getSuccessorTaskId() {
        return successorTaskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
