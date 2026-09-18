package com.pantheon.service.repository;

import com.pantheon.service.entity.SitePurchaseRequestApprovalLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SitePurchaseRequestApprovalLevelRepository extends JpaRepository<SitePurchaseRequestApprovalLevel, UUID> {

    List<SitePurchaseRequestApprovalLevel> findByConstructionSiteIdAndActiveOrderByStepOrder(
            UUID constructionSiteId, boolean active);
}
