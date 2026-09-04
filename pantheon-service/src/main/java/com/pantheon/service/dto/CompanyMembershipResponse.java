package com.pantheon.service.dto;

import com.pantheon.service.entity.CompanyOnboardingStatus;
import com.pantheon.service.entity.CompanyRole;
import java.util.UUID;

/** One of the companies a user belongs to, for "list my companies". */
public record CompanyMembershipResponse(
        UUID companyId, String companyName, CompanyRole role, CompanyOnboardingStatus onboardingStatus) {
}
