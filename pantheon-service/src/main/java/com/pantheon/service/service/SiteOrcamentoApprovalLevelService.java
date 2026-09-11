package com.pantheon.service.service;

import com.pantheon.service.dto.OrcamentoApprovalLevelEntry;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SiteOrcamentoApprovalLevel;
import com.pantheon.service.repository.SiteOrcamentoApprovalLevelRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A construction site's configured, ordered Orcamento approval chain. See
 * {@code orcamento-approval-workflow}'s "Per-site Orçamento approval levels". Callers are
 * responsible for authorizing the acting user (company staff only) before calling
 * {@link #setLevels}.
 */
@Service
public class SiteOrcamentoApprovalLevelService {

    private final SiteOrcamentoApprovalLevelRepository repository;

    public SiteOrcamentoApprovalLevelService(SiteOrcamentoApprovalLevelRepository repository) {
        this.repository = repository;
    }

    /**
     * The site's configured levels, or a single implicit {@code ENGINEER} level (not persisted)
     * if none are configured, so a freshly created obra needs no configuration to behave like
     * the pre-multi-level-approval default.
     */
    public List<SiteOrcamentoApprovalLevel> getEffectiveLevels(UUID constructionSiteId) {
        List<SiteOrcamentoApprovalLevel> configured =
                repository.findByConstructionSiteIdAndActiveOrderByStepOrder(constructionSiteId, true);
        if (!configured.isEmpty()) {
            return configured;
        }
        return List.of(new SiteOrcamentoApprovalLevel(
                UUID.randomUUID(), constructionSiteId, 1, ConstructionFunction.ENGINEER, Instant.now()));
    }

    @Transactional
    public List<SiteOrcamentoApprovalLevel> setLevels(UUID constructionSiteId, List<OrcamentoApprovalLevelEntry> levels) {
        Instant now = Instant.now();
        repository.findByConstructionSiteIdAndActiveOrderByStepOrder(constructionSiteId, true).forEach(existing -> {
            existing.deactivate(now);
            repository.save(existing);
        });

        List<SiteOrcamentoApprovalLevel> created = new ArrayList<>();
        for (OrcamentoApprovalLevelEntry entry : levels) {
            created.add(repository.save(new SiteOrcamentoApprovalLevel(
                    UUID.randomUUID(), constructionSiteId, entry.stepOrder(), entry.approverFunction(), now)));
        }
        return created;
    }
}
