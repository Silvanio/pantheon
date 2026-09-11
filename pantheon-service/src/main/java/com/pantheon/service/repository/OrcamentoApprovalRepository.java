package com.pantheon.service.repository;

import com.pantheon.service.entity.OrcamentoApproval;
import com.pantheon.service.entity.OrcamentoApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoApprovalRepository extends JpaRepository<OrcamentoApproval, UUID> {

    List<OrcamentoApproval> findByOrcamentoIdOrderByCycleNumberAscStepOrderAsc(UUID orcamentoId);

    List<OrcamentoApproval> findByOrcamentoIdAndCycleNumberOrderByStepOrderAsc(UUID orcamentoId, int cycleNumber);

    Optional<OrcamentoApproval> findFirstByOrcamentoIdAndCycleNumberAndStatusOrderByStepOrderAsc(
            UUID orcamentoId, int cycleNumber, OrcamentoApprovalStatus status);
}
