package com.pantheon.service.exception;

import java.util.UUID;

public class TaskColumnNotFoundException extends RuntimeException {

    public TaskColumnNotFoundException(UUID taskColumnId) {
        super("Task column not found: " + taskColumnId);
    }
}
