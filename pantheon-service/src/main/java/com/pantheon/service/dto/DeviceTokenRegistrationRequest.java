package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.pantheon.service.entity.DevicePlatform;

public record DeviceTokenRegistrationRequest(@NotBlank String token, @NotNull DevicePlatform platform) {
}
