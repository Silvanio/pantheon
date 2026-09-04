package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteMembershipRepository extends JpaRepository<SiteMembership, UUID> {

    List<SiteMembership> findByConstructionSiteId(UUID constructionSiteId);

    List<SiteMembership> findByUserId(UUID userId);

    Optional<SiteMembership> findByConstructionSiteIdAndUserId(UUID constructionSiteId, UUID userId);
}
