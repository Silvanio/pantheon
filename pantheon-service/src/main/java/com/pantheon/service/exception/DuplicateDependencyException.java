package com.pantheon.service.exception;

import java.util.UUID;

public class DuplicateDependencyException extends RuntimeException {

    public DuplicateDependencyException(UUID predecessorTaskId, UUID successorTaskId) {
        super("Schedule task " + successorTaskId + " already depends on " + predecessorTaskId);
    }
}
