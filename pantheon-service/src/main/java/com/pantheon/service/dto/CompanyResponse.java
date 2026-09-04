package com.pantheon.service.dto;

import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.CompanyOnboardingStatus;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        UUID planId,
        String legalName,
        String tradeName,
        String cnpj,
        String address,
        String logoObjectKey,
        CompanyOnboardingStatus onboardingStatus) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getPlanId(),
                company.getLegalName(),
                company.getTradeName(),
                company.getCnpj(),
                company.getAddress(),
                company.getLogoObjectKey(),
                company.getOnboardingStatus());
    }
}
