package com.pantheon.service.exception;

import java.util.UUID;

public class ScheduleTaskAlreadyLinkedException extends RuntimeException {

    public ScheduleTaskAlreadyLinkedException(UUID scheduleTaskId) {
        super("Schedule task already has a linked task card: " + scheduleTaskId);
    }
}
