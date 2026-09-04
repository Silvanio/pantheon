package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReportMedia;
public interface DailyReportMediaRepository extends JpaRepository<DailyReportMedia, UUID> {

    List<DailyReportMedia> findByDailyReportIdOrderByCreatedAtAsc(UUID dailyReportId);
}
