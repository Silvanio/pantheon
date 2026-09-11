package com.pantheon.service.repository;

import com.pantheon.service.entity.TaskColumn;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskColumnRepository extends JpaRepository<TaskColumn, UUID> {

    List<TaskColumn> findByCompanyIdOrderBySortOrderAsc(UUID companyId);
}
