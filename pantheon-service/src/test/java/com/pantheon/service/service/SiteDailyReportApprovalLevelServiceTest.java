package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.DailyReportApprovalLevelEntry;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SiteDailyReportApprovalLevel;
import com.pantheon.service.repository.SiteDailyReportApprovalLevelRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Mirrors {@code SitePurchaseRequestApprovalLevelServiceTest}'s shape (see
 * {@code SitePurchaseRequestApprovalLevelService}'s "no rows -> single ENGINEER default"
 * behavior, which this new, parallel service reproduces exactly for Diário de Obra).
 */
@ExtendWith(MockitoExtension.class)
class SiteDailyReportApprovalLevelServiceTest {

    @Mock
    private SiteDailyReportApprovalLevelRepository repository;

    private SiteDailyReportApprovalLevelService service;

    private UUID siteId;

    @BeforeEach
    void setUp() {
        service = new SiteDailyReportApprovalLevelService(repository);
        siteId = UUID.randomUUID();
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getEffectiveLevelsReturnsConfiguredLevelsWhenPresent() {
        SiteDailyReportApprovalLevel level = new SiteDailyReportApprovalLevel(
                UUID.randomUUID(), siteId, 1, ConstructionFunction.SITE_FOREMAN, Instant.now());
        when(repository.findByConstructionSiteIdAndActiveOrderByStepOrder(siteId, true)).thenReturn(List.of(level));

        List<SiteDailyReportApprovalLevel> result = service.getEffectiveLevels(siteId);

        assertThat(result).containsExactly(level);
    }

    @Test
    void getEffectiveLevelsDefaultsToSingleEngineerLevelWhenUnconfigured() {
        when(repository.findByConstructionSiteIdAndActiveOrderByStepOrder(siteId, true)).thenReturn(List.of());

        List<SiteDailyReportApprovalLevel> result = service.getEffectiveLevels(siteId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApproverFunction()).isEqualTo(ConstructionFunction.ENGINEER);
        assertThat(result.get(0).getStepOrder()).isEqualTo(1);
        verify(repository, never()).save(any());
    }

    @Test
    void setLevelsDeactivatesExistingAndPersistsNewOnes() {
        SiteDailyReportApprovalLevel existing = new SiteDailyReportApprovalLevel(
                UUID.randomUUID(), siteId, 1, ConstructionFunction.ENGINEER, Instant.now());
        when(repository.findByConstructionSiteIdAndActiveOrderByStepOrder(siteId, true)).thenReturn(List.of(existing));

        List<SiteDailyReportApprovalLevel> result = service.setLevels(siteId, List.of(
                new DailyReportApprovalLevelEntry(1, ConstructionFunction.SITE_FOREMAN),
                new DailyReportApprovalLevelEntry(2, ConstructionFunction.ENGINEER)));

        assertThat(existing.isActive()).isFalse();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getApproverFunction()).isEqualTo(ConstructionFunction.SITE_FOREMAN);
        assertThat(result.get(1).getApproverFunction()).isEqualTo(ConstructionFunction.ENGINEER);
        verify(repository, times(3)).save(any());
    }
}
