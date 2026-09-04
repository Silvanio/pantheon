package com.pantheon.service.exception;

import java.util.UUID;

import com.pantheon.service.entity.Project;
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(UUID projectId) {
        super("Project not found: " + projectId);
    }
}
