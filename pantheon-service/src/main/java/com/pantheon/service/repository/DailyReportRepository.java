package com.pantheon.service.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReport;
public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    Page<DailyReport> findByConstructionSiteId(UUID constructionSiteId, Pageable pageable);

    Optional<DailyReport> findByConstructionSiteIdAndReportDate(UUID constructionSiteId, LocalDate reportDate);

    long countByConstructionSiteId(UUID constructionSiteId);
}
