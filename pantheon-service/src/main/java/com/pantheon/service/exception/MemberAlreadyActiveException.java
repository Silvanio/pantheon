package com.pantheon.service.exception;

public class MemberAlreadyActiveException extends RuntimeException {

    public MemberAlreadyActiveException(String email) {
        super("Email is already an active member of this project: " + email);
    }
}
