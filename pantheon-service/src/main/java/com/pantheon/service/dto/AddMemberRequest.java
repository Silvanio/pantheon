package com.pantheon.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import com.pantheon.service.entity.ConstructionFunction;
/**
 * {@code function} and {@code specialty} are optional: {@code function} defaults to OTHER when
 * omitted; {@code specialty} is only persisted when {@code function} is SERVICE_PROVIDER.
 */
public record AddMemberRequest(
        @Email @NotBlank String email, ConstructionFunction function, String specialty) {
}
