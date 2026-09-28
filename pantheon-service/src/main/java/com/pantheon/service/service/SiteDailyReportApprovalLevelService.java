package com.pantheon.service.service;

import com.pantheon.service.dto.DailyReportApprovalLevelEntry;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SiteDailyReportApprovalLevel;
import com.pantheon.service.repository.SiteDailyReportApprovalLevelRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A construction site's configured, ordered Diário de Obra approval chain. See
 * {@code daily-report-approval-workflow}'s "Per-site Diário de Obra approval levels". Callers are
 * responsible for authorizing the acting user (company staff only) before calling
 * {@link #setLevels}. Mirrors {@code SitePurchaseRequestApprovalLevelService} — a new, parallel
 * service rather than a shared one (see the change's design.md Decision 1).
 */
@Service
public class SiteDailyReportApprovalLevelService {

    private final SiteDailyReportApprovalLevelRepository repository;

    public SiteDailyReportApprovalLevelService(SiteDailyReportApprovalLevelRepository repository) {
        this.repository = repository;
    }

    /**
     * The site's configured levels, or a single implicit {@code ENGINEER} level (not persisted)
     * if none are configured, so a freshly created obra needs no configuration to behave like
     * the pre-multi-level-approval default.
     */
    public List<SiteDailyReportApprovalLevel> getEffectiveLevels(UUID constructionSiteId) {
        List<SiteDailyReportApprovalLevel> configured =
                repository.findByConstructionSiteIdAndActiveOrderByStepOrder(constructionSiteId, true);
        if (!configured.isEmpty()) {
            return configured;
        }
        return List.of(new SiteDailyReportApprovalLevel(
                UUID.randomUUID(), constructionSiteId, 1, ConstructionFunction.ENGINEER, Instant.now()));
    }

    @Transactional
    public List<SiteDailyReportApprovalLevel> setLevels(
            UUID constructionSiteId, List<DailyReportApprovalLevelEntry> levels) {
        Instant now = Instant.now();
        repository.findByConstructionSiteIdAndActiveOrderByStepOrder(constructionSiteId, true).forEach(existing -> {
            existing.deactivate(now);
            repository.save(existing);
        });

        List<SiteDailyReportApprovalLevel> created = new ArrayList<>();
        for (DailyReportApprovalLevelEntry entry : levels) {
            created.add(repository.save(new SiteDailyReportApprovalLevel(
                    UUID.randomUUID(), constructionSiteId, entry.stepOrder(), entry.approverFunction(), now)));
        }
        return created;
    }
}
