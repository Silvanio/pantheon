package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** {@code predecessorTaskId} is the task that must come first; the path's task id is the successor. */
public record ScheduleDependencyRequest(@NotNull UUID predecessorTaskId) {
}
