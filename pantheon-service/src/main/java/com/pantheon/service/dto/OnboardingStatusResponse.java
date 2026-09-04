package com.pantheon.service.dto;

import java.util.UUID;

/**
 * {@code expiredProjectId} is only populated when the caller administers an expired project
 * (i.e. can act on it via plan confirmation). A caller who is only a MEMBER of expired
 * projects gets {@code needsPlanSelection = true} with a null {@code expiredProjectId},
 * signalling the frontend to show a "contact your administrator" message instead of the
 * plan selection screen.
 */
public record OnboardingStatusResponse(
        boolean hasProject,
        ActiveProjectResponse activeProject,
        boolean needsPlanSelection,
        UUID expiredProjectId) {
}
