package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskLabel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskLabelRepository extends JpaRepository<TaskLabel, UUID> {

    List<TaskLabel> findByCompanyIdOrderByNameAsc(UUID companyId);

    List<TaskLabel> findByCompanyIdIn(List<UUID> companyIds);

    List<TaskLabel> findByCardId(UUID cardId);

    void deleteByCardId(UUID cardId);
}
