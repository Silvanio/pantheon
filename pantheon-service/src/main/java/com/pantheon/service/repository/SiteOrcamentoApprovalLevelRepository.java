package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteOrcamentoApprovalLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteOrcamentoApprovalLevelRepository extends JpaRepository<SiteOrcamentoApprovalLevel, UUID> {

    List<SiteOrcamentoApprovalLevel> findByConstructionSiteIdAndActiveOrderByStepOrder(
            UUID constructionSiteId, boolean active);
}
