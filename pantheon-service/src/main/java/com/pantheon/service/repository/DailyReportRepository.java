package com.pantheon.service.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportStatus;
public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    Page<DailyReport> findByConstructionSiteId(UUID constructionSiteId, Pageable pageable);

    /** Used to list only {@code APPROVED} reports for a member whose resolved access is plain {@code VIEW} — see {@code DailyReportService#list}. */
    Page<DailyReport> findByConstructionSiteIdAndStatus(UUID constructionSiteId, DailyReportStatus status, Pageable pageable);

    Optional<DailyReport> findByConstructionSiteIdAndReportDate(UUID constructionSiteId, LocalDate reportDate);

    long countByConstructionSiteId(UUID constructionSiteId);
}
