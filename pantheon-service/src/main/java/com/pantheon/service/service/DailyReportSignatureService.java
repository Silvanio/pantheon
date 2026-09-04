package com.pantheon.service.service;

import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.entity.DailyReportStatus;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.DailyReportNotFoundException;
import com.pantheon.service.exception.DailyReportNotSubmittedException;
import com.pantheon.service.exception.NotCompanyMemberException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportSignatureRepository;
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
    private final CompanyMembershipRepository companyMembershipRepository;
    private final SiteAccessService siteAccessService;

    public DailyReportSignatureService(
            DailyReportSignatureRepository signatureRepository,
            DailyReportRepository dailyReportRepository,
            ConstructionSiteRepository siteRepository,
            CompanyMembershipRepository companyMembershipRepository,
            SiteAccessService siteAccessService) {
        this.signatureRepository = signatureRepository;
        this.dailyReportRepository = dailyReportRepository;
        this.siteRepository = siteRepository;
        this.companyMembershipRepository = companyMembershipRepository;
        this.siteAccessService = siteAccessService;
    }

    @Transactional
    public DailyReportSignature sign(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        var access = siteAccessService.requireAccess(report.getConstructionSiteId(), actingUserId);

        if (report.getStatus() != DailyReportStatus.SUBMITTED) {
            throw new DailyReportNotSubmittedException(reportId);
        }

        UUID membershipId = access.siteMembership() != null
                ? access.siteMembership().getId()
                : companyMembershipId(report.getConstructionSiteId(), actingUserId);
        DailyReportSignature signature =
                new DailyReportSignature(UUID.randomUUID(), reportId, membershipId, access.function(), Instant.now());
        return signatureRepository.save(signature);
    }

    private UUID companyMembershipId(UUID constructionSiteId, UUID userId) {
        ConstructionSite site = siteRepository
                .findById(constructionSiteId)
                .orElseThrow(() -> new ConstructionSiteNotFoundException(constructionSiteId));
        return companyMembershipRepository
                .findByCompanyIdAndUserId(site.getCompanyId(), userId)
                .orElseThrow(() -> new NotCompanyMemberException(site.getCompanyId()))
                .getId();
    }

    public List<DailyReportSignature> list(UUID reportId, UUID actingUserId) {
        DailyReport report =
                dailyReportRepository.findById(reportId).orElseThrow(() -> new DailyReportNotFoundException(reportId));
        siteAccessService.requireAccess(report.getConstructionSiteId(), actingUserId);

        return signatureRepository.findByDailyReportIdOrderBySignedAtAsc(reportId);
    }
}
