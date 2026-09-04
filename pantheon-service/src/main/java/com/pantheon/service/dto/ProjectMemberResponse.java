package com.pantheon.service.dto;

import java.util.UUID;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipStatus;
import com.pantheon.service.entity.ProjectRole;
/**
 * {@code membershipId} is the {@code ProjectMembership} row's own id — distinct from
 * {@code userId} — needed wherever another record links to this specific membership (e.g. a
 * daily report's workforce entry). {@code invited} is true while the membership is still an
 * unaccepted invitation ({@code status == INVITED}); the UI shows a "Convite" badge for it.
 */
public record ProjectMemberResponse(
        UUID membershipId,
        UUID userId,
        String email,
        String displayName,
        ProjectRole role,
        ConstructionFunction function,
        String specialty,
        MembershipStatus status,
        boolean invited) {
}
