package com.pantheon.service.exception;

import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.Project;

public class ProjectLimitExceededException extends RuntimeException {

    public ProjectLimitExceededException(Plan plan) {
        super("Project limit reached for plan: " + plan);
    }
}
