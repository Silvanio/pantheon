package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskCard;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCardRepository extends JpaRepository<TaskCard, UUID> {

    List<TaskCard> findByConstructionSiteIdOrderBySortOrderAsc(UUID constructionSiteId);

    List<TaskCard> findByConstructionSiteIdInOrderBySortOrderAsc(List<UUID> constructionSiteIds);

    long countByConstructionSiteIdAndColumnId(UUID constructionSiteId, UUID columnId);

    boolean existsByColumnId(UUID columnId);
}
