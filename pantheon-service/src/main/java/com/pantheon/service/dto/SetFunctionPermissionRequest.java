package com.pantheon.service.dto;

import com.pantheon.service.entity.AccessLevel;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.PermissionCapability;
import jakarta.validation.constraints.NotNull;

public record SetFunctionPermissionRequest(
        @NotNull ConstructionFunction function, @NotNull PermissionCapability capability, @NotNull AccessLevel accessLevel) {
}
