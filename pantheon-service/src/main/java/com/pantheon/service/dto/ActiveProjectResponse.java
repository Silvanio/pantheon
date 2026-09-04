package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.ProjectRole;
public record ActiveProjectResponse(UUID id, ProjectRole role) {
}
