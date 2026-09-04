package com.pantheon.service.repository;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.PermissionCapability;
import com.pantheon.service.entity.SitePermissionOverride;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SitePermissionOverrideRepository extends JpaRepository<SitePermissionOverride, UUID> {

    List<SitePermissionOverride> findByConstructionSiteId(UUID constructionSiteId);

    Optional<SitePermissionOverride> findBySiteMembershipIdAndCapability(
            UUID siteMembershipId, PermissionCapability capability);

    Optional<SitePermissionOverride> findByConstructionSiteIdAndFunctionAndCapability(
            UUID constructionSiteId, ConstructionFunction function, PermissionCapability capability);
}
