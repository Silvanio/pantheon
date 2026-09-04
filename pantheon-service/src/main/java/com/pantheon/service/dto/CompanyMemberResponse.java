package com.pantheon.service.dto;

import com.pantheon.service.entity.CompanyRole;
import com.pantheon.service.entity.MembershipStatus;
import java.util.UUID;

/** A row in a company's staff list. {@code invited} drives the "Convite" badge in the UI. */
public record CompanyMemberResponse(
        UUID membershipId,
        UUID userId,
        String email,
        String displayName,
        CompanyRole role,
        MembershipStatus status,
        boolean invited) {
}
