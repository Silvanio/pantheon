package com.pantheon.service.exception;

import java.util.UUID;

public class SelfDependencyException extends RuntimeException {

    public SelfDependencyException(UUID taskId) {
        super("A schedule task cannot depend on itself: " + taskId);
    }
}
