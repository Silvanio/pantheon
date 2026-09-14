package com.pantheon.service.exception;

public class PersonBelongsToAnotherCompanyException extends RuntimeException {

    public PersonBelongsToAnotherCompanyException(String email) {
        super("Person already belongs to another company: " + email);
    }
}
