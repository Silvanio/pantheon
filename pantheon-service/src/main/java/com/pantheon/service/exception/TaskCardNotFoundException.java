package com.pantheon.service.exception;

import java.util.UUID;

public class TaskCardNotFoundException extends RuntimeException {

    public TaskCardNotFoundException(UUID taskCardId) {
        super("Task card not found: " + taskCardId);
    }
}
