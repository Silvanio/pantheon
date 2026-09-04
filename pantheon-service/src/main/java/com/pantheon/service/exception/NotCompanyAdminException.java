package com.pantheon.service.exception;

import java.util.UUID;

public class NotCompanyAdminException extends RuntimeException {

    public NotCompanyAdminException(UUID companyId) {
        super("User is not an administrator of company: " + companyId);
    }
}
