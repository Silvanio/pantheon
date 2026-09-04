package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipStatus;
import java.util.UUID;

public record SiteMemberResponse(
        UUID membershipId,
        UUID userId,
        String email,
        String displayName,
        ConstructionFunction function,
        String trade,
        String cpf,
        MembershipStatus status,
        boolean invited) {
}
