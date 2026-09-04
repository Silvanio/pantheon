package com.pantheon.service.exception;

public class InvitationNotFoundException extends RuntimeException {

    public InvitationNotFoundException() {
        super("No valid invitation found for the given token");
    }
}
