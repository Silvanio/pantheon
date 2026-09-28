package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.DailyReportNotApprovedException;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportSignatureRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the sign-off status gate's SUBMITTED -> APPROVED rename (see
 * {@code daily-report-media-and-signoff}'s "Report sign-off" requirement, modified by
 * add-daily-report-approval-workflow).
 */
@ExtendWith(MockitoExtension.class)
class DailyReportSignatureServiceTest {

    @Mock
    private DailyReportSignatureRepository signatureRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private CompanyMembershipRepository companyMembershipRepository;

    @Mock
    private SiteAccessService siteAccessService;

    private DailyReportSignatureService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new DailyReportSignatureService(
                signatureRepository, dailyReportRepository, siteRepository, companyMembershipRepository,
                siteAccessService);
        siteId = UUID.randomUUID();
        lenient().when(signatureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private DailyReport draftReport() {
        return new DailyReport(UUID.randomUUID(), siteId, LocalDate.now(), 1, UUID.randomUUID(), Instant.now());
    }

    private SiteMembership member(UUID userId) {
        SiteMembership membership = SiteMembership.invited(
                UUID.randomUUID(), siteId, userId, ConstructionFunction.ENGINEER, null, null, Instant.now());
        membership.accept();
        return membership;
    }

    @Test
    void signRejectsWhenReportIsDraft() {
        DailyReport report = draftReport();
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        UUID userId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, userId)).thenReturn(new SiteAccessContext(false, member(userId)));

        assertThatThrownBy(() -> service.sign(report.getId(), userId))
                .isInstanceOf(DailyReportNotApprovedException.class);
    }

    @Test
    void signRejectsWhenReportIsPendingApproval() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        UUID userId = UUID.randomUUID();
        when(siteAccessService.requireAccess(siteId, userId)).thenReturn(new SiteAccessContext(false, member(userId)));

        assertThatThrownBy(() -> service.sign(report.getId(), userId))
                .isInstanceOf(DailyReportNotApprovedException.class);
    }

    @Test
    void signSucceedsWhenReportIsApproved() {
        DailyReport report = draftReport();
        report.submit(Instant.now());
        report.approve(Instant.now());
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        UUID userId = UUID.randomUUID();
        SiteMembership membership = member(userId);
        when(siteAccessService.requireAccess(siteId, userId)).thenReturn(new SiteAccessContext(false, membership));

        DailyReportSignature signature = service.sign(report.getId(), userId);

        assertThat(signature.getDailyReportId()).isEqualTo(report.getId());
        assertThat(signature.getMembershipId()).isEqualTo(membership.getId());
        assertThat(signature.getFunction()).isEqualTo(ConstructionFunction.ENGINEER);
    }
}
