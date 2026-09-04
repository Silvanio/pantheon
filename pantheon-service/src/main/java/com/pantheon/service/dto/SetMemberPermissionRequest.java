package com.pantheon.service.dto;

import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.PermissionCapability;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetMemberPermissionRequest(
        @NotNull UUID siteMembershipId, @NotNull PermissionCapability capability, @NotNull AccessLevel accessLevel) {
}
