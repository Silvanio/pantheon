package com.pantheon.service.exception;

public class InvitationNotForCurrentUserException extends RuntimeException {

    public InvitationNotForCurrentUserException() {
        super("This invitation can only be accepted by the invited account");
    }
}
