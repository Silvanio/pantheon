package com.pantheon.service.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteStatus;
public record ConstructionSiteResponse(
        UUID id,
        UUID projectId,
        String name,
        String address,
        SiteStatus status,
        LocalDate startDate,
        LocalDate expectedEndDate) {

    public static ConstructionSiteResponse from(ConstructionSite site) {
        return new ConstructionSiteResponse(
                site.getId(),
                site.getProjectId(),
                site.getName(),
                site.getAddress(),
                site.getStatus(),
                site.getStartDate(),
                site.getExpectedEndDate());
    }
}
