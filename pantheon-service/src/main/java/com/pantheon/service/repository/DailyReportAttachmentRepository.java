package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DailyReportAttachment;
public interface DailyReportAttachmentRepository extends JpaRepository<DailyReportAttachment, UUID> {

    List<DailyReportAttachment> findByDailyReportIdOrderByCreatedAtAsc(UUID dailyReportId);
}
