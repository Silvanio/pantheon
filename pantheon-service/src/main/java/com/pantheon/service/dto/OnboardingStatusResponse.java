package com.pantheon.service.dto;

import java.util.List;

/**
 * Reports, for every company the current user belongs to, its onboarding status
 * ({@code PLAN_PENDING}/{@code PROFILE_PENDING}/{@code COMPLETE}). {@code hasCompany} is false
 * only when the user belongs to no company at all, driving the frontend to the
 * company-creation step instead of any per-company onboarding step.
 */
public record OnboardingStatusResponse(boolean hasCompany, List<CompanyMembershipResponse> companies) {
}
