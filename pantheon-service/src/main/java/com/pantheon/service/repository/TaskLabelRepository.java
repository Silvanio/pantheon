package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskLabel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, UUID> {

    List<TaskLabel> findByConstructionSiteIdOrderByNameAsc(UUID constructionSiteId);
}
