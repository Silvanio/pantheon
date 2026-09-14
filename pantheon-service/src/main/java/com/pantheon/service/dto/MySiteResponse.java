package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.SiteStatus;
import java.time.LocalDate;
import java.util.UUID;

/** A construction site a site-only member has access to, with its owning company's name inlined. */
public record MySiteResponse(
        UUID id, UUID companyId, String companyName, String name, String address, SiteStatus status,
        LocalDate startDate, LocalDate expectedEndDate, String photoObjectKey) {

    public static MySiteResponse from(ConstructionSite site, String companyName) {
        return new MySiteResponse(
                site.getId(), site.getCompanyId(), companyName, site.getName(), site.getAddress(), site.getStatus(),
                site.getStartDate(), site.getExpectedEndDate(), site.getPhotoObjectKey());
    }
}
