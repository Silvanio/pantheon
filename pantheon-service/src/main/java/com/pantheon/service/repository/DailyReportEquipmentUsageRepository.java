package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReportEquipmentUsage;
public interface DailyReportEquipmentUsageRepository extends JpaRepository<DailyReportEquipmentUsage, UUID> {

    List<DailyReportEquipmentUsage> findByDailyReportId(UUID dailyReportId);
}
