package com.pantheon.service.exception;

public class EmailRequiredException extends RuntimeException {

    public EmailRequiredException() {
        super("Email is required for this function");
    }
}
