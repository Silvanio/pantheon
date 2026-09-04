package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;

import com.pantheon.service.entity.SiteStatus;
public record ConstructionSiteStatusUpdateRequest(@NotNull SiteStatus status) {
}
