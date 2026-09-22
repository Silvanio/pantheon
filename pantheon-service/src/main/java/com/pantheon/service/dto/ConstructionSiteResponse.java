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
        String photoObjectKey,
        Integer schedulePercentComplete) {

    /** {@code schedulePercentComplete} is {@code null} when the site has no schedule tasks yet — see
     * {@code ScheduleService#computeProgress} and the construction-schedule spec's progress requirement. */
    public static ConstructionSiteResponse from(ConstructionSite site, Integer schedulePercentComplete) {
        return new ConstructionSiteResponse(
                site.getId(),
                site.getCompanyId(),
                site.getName(),
                site.getAddress(),
                site.getStatus(),
                site.getStartDate(),
                site.getExpectedEndDate(),
                site.getPhotoObjectKey(),
                schedulePercentComplete);
    }
}
