package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReportSignature;
public interface DailyReportSignatureRepository extends JpaRepository<DailyReportSignature, UUID> {

    List<DailyReportSignature> findByDailyReportIdOrderBySignedAtAsc(UUID dailyReportId);
}
