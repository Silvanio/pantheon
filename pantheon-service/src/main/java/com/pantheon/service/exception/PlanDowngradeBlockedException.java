package com.pantheon.service.exception;

/** Thrown when changing to a plan whose active-site limit is below the company's current active construction-site count. */
public class PlanDowngradeBlockedException extends RuntimeException {

    public PlanDowngradeBlockedException(String message) {
        super(message);
    }
}
