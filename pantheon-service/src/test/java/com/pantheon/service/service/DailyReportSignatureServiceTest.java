package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.DailyReport;
import com.pantheon.service.entity.DailyReportSignature;
import com.pantheon.service.entity.ProjectMembership;
import com.pantheon.service.entity.ProjectRole;
import com.pantheon.service.exception.DailyReportNotSubmittedException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.DailyReportRepository;
import com.pantheon.service.repository.DailyReportSignatureRepository;
import com.pantheon.service.repository.ProjectMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyReportSignatureServiceTest {

    @Mock
    private DailyReportSignatureRepository signatureRepository;

    @Mock
    private DailyReportRepository dailyReportRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private ProjectMembershipRepository membershipRepository;

    private DailyReportSignatureService signatureService;

    private UUID projectId;
    private UUID userId;
    private ConstructionSite site;

    @BeforeEach
    void setUp() {
        signatureService =
                new DailyReportSignatureService(signatureRepository, dailyReportRepository, siteRepository, membershipRepository);

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        site = new ConstructionSite(
                UUID.randomUUID(), projectId, "Torre Norte", "Av. Central, 500", LocalDate.now(), null, userId,
                Instant.now());

        lenient().when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
        lenient().when(signatureRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private DailyReport report(boolean submitted) {
        DailyReport r = new DailyReport(UUID.randomUUID(), site.getId(), LocalDate.now(), 1, userId, Instant.now());
        if (submitted) {
            r.submit(Instant.now());
        }
        return r;
    }

    @Test
    void signSucceedsOnSubmittedReport() {
        DailyReport report = report(true);
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        ProjectMembership membership = new ProjectMembership(
                UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, ConstructionFunction.ENGINEER, null,
                Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(membership));

        DailyReportSignature signature = signatureService.sign(report.getId(), userId);

        assertThat(signature.getMembershipId()).isEqualTo(membership.getId());
        assertThat(signature.getFunction()).isEqualTo(ConstructionFunction.ENGINEER);
    }

    @Test
    void signRejectedOnDraftReport() {
        DailyReport report = report(false);
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> signatureService.sign(report.getId(), userId))
                .isInstanceOf(DailyReportNotSubmittedException.class);
    }

    @Test
    void listReturnsSignaturesInOrder() {
        DailyReport report = report(true);
        when(dailyReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        ProjectMembership membership =
                new ProjectMembership(UUID.randomUUID(), projectId, userId, ProjectRole.MEMBER, Instant.now());
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.of(membership));
        DailyReportSignature signature = new DailyReportSignature(
                UUID.randomUUID(), report.getId(), membership.getId(), ConstructionFunction.OTHER, Instant.now());
        when(signatureRepository.findByDailyReportIdOrderBySignedAtAsc(report.getId())).thenReturn(List.of(signature));

        assertThat(signatureService.list(report.getId(), userId)).containsExactly(signature);
    }
}
