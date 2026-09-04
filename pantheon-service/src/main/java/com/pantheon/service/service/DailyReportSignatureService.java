package com.pantheon.service.service;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.entity.DailyReportStatus;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.DailyReportNotSubmittedException;
import com.pantheon.service.exception.NotProjectMemberException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportSignatureRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyReportSignatureService {

    private final DailyReportSignatureRepository signatureRepository;
    private final DailyReportRepository dailyReportRepository;
    private final ConstructionSiteRepository siteRepository;
    private final ProjectMembershipRepository membershipRepository;

    public DailyReportSignatureService(
            DailyReportSignatureRepository signatureRepository,
            DailyReportRepository dailyReportRepository,
            ConstructionSiteRepository siteRepository,
            ProjectMembershipRepository membershipRepository) {
        this.signatureRepository = signatureRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.siteRepository = siteRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public DailyReportSignature sign(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        ConstructionSite site = siteRepository
                .findById(report.getConstructionSiteId())
                .orElseThrow(() -> new ConstructionSiteNotFoundException(report.getConstructionSiteId()));
        ProjectMembership membership = membershipRepository
                .findByProjectIdAndUserId(site.getProjectId(), actingUserId)
                .orElseThrow(() -> new NotProjectMemberException(site.getProjectId()));

        if (report.getStatus() != DailyReportStatus.SUBMITTED) {
            throw new DailyReportNotSubmittedException(reportId);
        }

        DailyReportSignature signature = new DailyReportSignature(
                UUID.randomUUID(), reportId, membership.getId(), membership.getFunction(), Instant.now());
        return signatureRepository.save(signature);
    }

    public List<DailyReportSignature> list(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        ConstructionSite site = siteRepository
                .findById(report.getConstructionSiteId())
                .orElseThrow(() -> new ConstructionSiteNotFoundException(report.getConstructionSiteId()));
        membershipRepository
                .findByProjectIdAndUserId(site.getProjectId(), actingUserId)
                .orElseThrow(() -> new NotProjectMemberException(site.getProjectId()));

        return signatureRepository.findByDailyReportIdOrderBySignedAtAsc(reportId);
    }
}
