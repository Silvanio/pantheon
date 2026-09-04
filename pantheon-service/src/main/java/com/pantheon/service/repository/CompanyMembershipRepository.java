package com.pantheon.service.repository;

import com.pantheon.service.entity.CompanyMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyMembershipRepository extends JpaRepository<CompanyMembership, UUID> {

    List<CompanyMembership> findByUserId(UUID userId);

    List<CompanyMembership> findByCompanyId(UUID companyId);

    Optional<CompanyMembership> findByCompanyIdAndUserId(UUID companyId, UUID userId);
}
