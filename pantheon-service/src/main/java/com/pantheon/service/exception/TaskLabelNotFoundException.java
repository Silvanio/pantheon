package com.pantheon.service.exception;

import java.util.UUID;

public class TaskLabelNotFoundException extends RuntimeException {

    public TaskLabelNotFoundException(UUID taskLabelId) {
        super("Task label not found: " + taskLabelId);
    }
}
