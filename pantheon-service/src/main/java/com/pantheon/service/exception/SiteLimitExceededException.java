package com.pantheon.service.exception;

/** Thrown when creating a construction site would exceed the company's plan's active-site limit (or no plan is selected). */
public class SiteLimitExceededException extends RuntimeException {

    public SiteLimitExceededException(String message) {
        super(message);
    }
}
