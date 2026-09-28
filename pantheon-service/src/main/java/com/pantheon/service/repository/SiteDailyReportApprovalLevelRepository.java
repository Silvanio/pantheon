package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteDailyReportApprovalLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteDailyReportApprovalLevelRepository extends JpaRepository<SiteDailyReportApprovalLevel, UUID> {

    List<SiteDailyReportApprovalLevel> findByConstructionSiteIdAndActiveOrderByStepOrder(
            UUID constructionSiteId, boolean active);
}
