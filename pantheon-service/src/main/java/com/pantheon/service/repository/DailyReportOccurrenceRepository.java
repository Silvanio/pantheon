package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReportOccurrence;
public interface DailyReportOccurrenceRepository extends JpaRepository<DailyReportOccurrence, UUID> {

    List<DailyReportOccurrence> findByDailyReportId(UUID dailyReportId);
}
