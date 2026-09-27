package com.pantheon.service.repository;

import com.pantheon.service.entity.ScheduleStage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleStageRepository extends JpaRepository<ScheduleStage, UUID> {

    List<ScheduleStage> findByConstructionSiteIdOrderBySortOrderAsc(UUID constructionSiteId);

    List<ScheduleStage> findByConstructionSiteIdIn(Collection<UUID> constructionSiteIds);

    long countByConstructionSiteId(UUID constructionSiteId);
}
