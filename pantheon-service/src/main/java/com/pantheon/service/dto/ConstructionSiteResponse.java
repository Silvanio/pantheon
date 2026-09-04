package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ConstructionSiteResponse(
        UUID id,
        UUID companyId,
        String name,
        String address,
        SiteStatus status,
        LocalDate startDate,
        LocalDate expectedEndDate,
        String photoObjectKey) {

    public static ConstructionSiteResponse from(ConstructionSite site) {
        return new ConstructionSiteResponse(
                site.getId(),
                site.getCompanyId(),
                site.getName(),
                site.getAddress(),
                site.getStatus(),
                site.getStartDate(),
                site.getExpectedEndDate(),
                site.getPhotoObjectKey());
    }
}
