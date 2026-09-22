package com.pantheon.service.exception;

import java.util.UUID;

public class ScheduleStageNotFoundException extends RuntimeException {

    public ScheduleStageNotFoundException(UUID stageId) {
        super("Schedule stage not found: " + stageId);
    }
}
