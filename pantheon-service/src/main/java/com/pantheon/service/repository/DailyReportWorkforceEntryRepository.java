package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReportWorkforceEntry;
public interface DailyReportWorkforceEntryRepository extends JpaRepository<DailyReportWorkforceEntry, UUID> {

    List<DailyReportWorkforceEntry> findByDailyReportId(UUID dailyReportId);
}
