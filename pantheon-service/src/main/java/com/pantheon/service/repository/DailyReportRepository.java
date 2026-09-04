package com.pantheon.service.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReport;
public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    List<DailyReport> findByConstructionSiteIdOrderByReportDateDesc(UUID constructionSiteId);

    Optional<DailyReport> findByConstructionSiteIdAndReportDate(UUID constructionSiteId, LocalDate reportDate);

    long countByConstructionSiteId(UUID constructionSiteId);
}
