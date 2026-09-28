package com.pantheon.service.repository;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.DailyReportApproval;
import com.pantheon.service.entity.DailyReportApprovalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyReportApprovalRepository extends JpaRepository<DailyReportApproval, UUID> {

    List<DailyReportApproval> findByDailyReportIdOrderByCycleNumberAscStepOrderAsc(UUID dailyReportId);

    List<DailyReportApproval> findByDailyReportIdAndCycleNumberOrderByStepOrderAsc(UUID dailyReportId, int cycleNumber);

    Optional<DailyReportApproval> findFirstByDailyReportIdAndCycleNumberAndStatusOrderByStepOrderAsc(
            UUID dailyReportId, int cycleNumber, DailyReportApprovalStatus status);

    /** Whether {@code function} already decided (approved or rejected) a step of this cycle — i.e. their turn already happened. */
    boolean existsByDailyReportIdAndCycleNumberAndApproverFunctionAndStatusNot(
            UUID dailyReportId, int cycleNumber, ConstructionFunction approverFunction, DailyReportApprovalStatus status);
}
