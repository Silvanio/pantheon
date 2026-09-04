package com.pantheon.service.repository;

import com.pantheon.service.entity.Plan;
import com.pantheon.service.entity.PlanCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findAllByOrderBySortOrderAsc();

    Optional<Plan> findByCode(PlanCode code);
}
