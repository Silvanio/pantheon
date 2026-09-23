package com.pantheon.service.exception;

import java.util.UUID;

public class NoTaskColumnsAvailableException extends RuntimeException {

    public NoTaskColumnsAvailableException(UUID companyId) {
        super("Company has no task columns configured yet, cannot create a task card: " + companyId);
    }
}
