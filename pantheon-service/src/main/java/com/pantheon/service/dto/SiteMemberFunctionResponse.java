package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;

/** The acting user's own construction function on a site — {@code null} for company staff with no {@code SiteMembership} there. */
public record SiteMemberFunctionResponse(ConstructionFunction function) {
}
