package com.pantheon.service.dto;

import java.util.List;
import java.util.UUID;

/**
 * Reports, for every company the current user belongs to, its onboarding status
 * ({@code PLAN_PENDING}/{@code PROFILE_PENDING}/{@code COMPLETE}). {@code hasCompany} is false
 * when the user belongs to no company at all. {@code siteIds} lists the construction sites the
 * user has an active {@code SiteMembership} on (e.g. a client or an outside architect/engineer
 * who has no company of their own) — used so such a user isn't forced into "create a company"
 * just because {@code hasCompany} is false.
 */
public record OnboardingStatusResponse(boolean hasCompany, List<CompanyMembershipResponse> companies, List<UUID> siteIds) {
}
