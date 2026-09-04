package com.pantheon.service.exception;

public class RegistrationNotApplicableException extends RuntimeException {

    public RegistrationNotApplicableException() {
        super("This invitation does not require completing a registration");
    }
}
