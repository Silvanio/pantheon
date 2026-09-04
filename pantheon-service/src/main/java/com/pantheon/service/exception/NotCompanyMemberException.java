package com.pantheon.service.exception;

import java.util.UUID;

public class NotCompanyMemberException extends RuntimeException {

    public NotCompanyMemberException(UUID companyId) {
        super("User is not a member of company: " + companyId);
    }
}
