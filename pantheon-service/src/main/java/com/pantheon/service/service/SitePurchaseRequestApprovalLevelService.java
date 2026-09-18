package com.pantheon.service.service;

import com.pantheon.service.dto.PurchaseRequestApprovalLevelEntry;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.SitePurchaseRequestApprovalLevel;
import com.pantheon.service.repository.SitePurchaseRequestApprovalLevelRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A construction site's configured, ordered Pedido de Compra approval chain. See
 * {@code purchase-request-approval-workflow}'s "Per-site Pedido de Compra approval levels".
 * Callers are responsible for authorizing the acting user (company staff only) before calling
 * {@link #setLevels}.
 */
@Service
public class SitePurchaseRequestApprovalLevelService {

    private final SitePurchaseRequestApprovalLevelRepository repository;

    public SitePurchaseRequestApprovalLevelService(SitePurchaseRequestApprovalLevelRepository repository) {
        this.repository = repository;
    }

    /**
     * The site's configured levels, or a single implicit {@code ENGINEER} level (not persisted)
     * if none are configured, so a freshly created obra needs no configuration to behave like
     * the pre-multi-level-approval default.
     */
    public List<SitePurchaseRequestApprovalLevel> getEffectiveLevels(UUID constructionSiteId) {
        List<SitePurchaseRequestApprovalLevel> configured =
                repository.findByConstructionSiteIdAndActiveOrderByStepOrder(constructionSiteId, true);
        if (!configured.isEmpty()) {
            return configured;
        }
        return List.of(new SitePurchaseRequestApprovalLevel(
                UUID.randomUUID(), constructionSiteId, 1, ConstructionFunction.ENGINEER, Instant.now()));
    }

    @Transactional
    public List<SitePurchaseRequestApprovalLevel> setLevels(
            UUID constructionSiteId, List<PurchaseRequestApprovalLevelEntry> levels) {
        Instant now = Instant.now();
        repository.findByConstructionSiteIdAndActiveOrderByStepOrder(constructionSiteId, true).forEach(existing -> {
            existing.deactivate(now);
            repository.save(existing);
        });

        List<SitePurchaseRequestApprovalLevel> created = new ArrayList<>();
        for (PurchaseRequestApprovalLevelEntry entry : levels) {
            created.add(repository.save(new SitePurchaseRequestApprovalLevel(
                    UUID.randomUUID(), constructionSiteId, entry.stepOrder(), entry.approverFunction(), now)));
        }
        return created;
    }
}
