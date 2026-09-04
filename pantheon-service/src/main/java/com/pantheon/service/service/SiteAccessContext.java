package com.pantheon.service.service;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SiteMembership;

/**
 * A resolved user's access to a construction site: either company staff (unrestricted,
 * {@code siteMembership} null) or a specific {@link SiteMembership} on that site.
 */
public record SiteAccessContext(boolean companyStaff, SiteMembership siteMembership) {

    public ConstructionFunction function() {
        return siteMembership != null ? siteMembership.getFunction() : null;
    }
}
