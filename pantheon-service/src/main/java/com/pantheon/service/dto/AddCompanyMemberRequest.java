package com.pantheon.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddCompanyMemberRequest(@Email @NotBlank String email) {
}
