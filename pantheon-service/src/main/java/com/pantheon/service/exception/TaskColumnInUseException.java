package com.pantheon.service.exception;

import java.util.UUID;

/** Thrown when deleting a {@code TaskColumn} that still has at least one card, in any obra, placed in it. */
public class TaskColumnInUseException extends RuntimeException {

    public TaskColumnInUseException(UUID taskColumnId) {
        super("Task column is in use and cannot be deleted: " + taskColumnId);
    }
}
