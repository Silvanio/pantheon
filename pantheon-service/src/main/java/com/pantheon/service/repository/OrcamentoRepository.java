package com.pantheon.service.repository;

import com.pantheon.service.entity.Orcamento;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {

    List<Orcamento> findByConstructionSiteIdOrderByCreatedAtDesc(UUID constructionSiteId);

    List<Orcamento> findByConstructionSiteIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID constructionSiteId, Instant dayStart, Instant dayEnd);

    List<Orcamento> findByConstructionSiteIdAndSourcePurchaseRequestIdOrderByCreatedAtDesc(
            UUID constructionSiteId, UUID sourcePurchaseRequestId);

    List<Orcamento> findByConstructionSiteIdAndCreatedAtBetweenAndSourcePurchaseRequestIdOrderByCreatedAtDesc(
            UUID constructionSiteId, Instant dayStart, Instant dayEnd, UUID sourcePurchaseRequestId);
}
