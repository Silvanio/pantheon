package com.pantheon.service.exception;

import java.util.UUID;

public class ScheduleTaskNotFoundException extends RuntimeException {

    public ScheduleTaskNotFoundException(UUID taskId) {
        super("Schedule task not found: " + taskId);
    }
}
