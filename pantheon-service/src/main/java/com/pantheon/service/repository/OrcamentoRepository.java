package com.pantheon.service.repository;

import com.pantheon.service.entity.Orcamento;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {

    List<Orcamento> findByConstructionSiteIdOrderByCreatedAtDesc(UUID constructionSiteId);

    @Query("""
            SELECT o FROM Orcamento o
            WHERE o.constructionSiteId = :siteId
              AND (:dayStart IS NULL OR o.createdAt >= :dayStart)
              AND (:dayEnd IS NULL OR o.createdAt < :dayEnd)
              AND (:purchaseRequestId IS NULL OR o.sourcePurchaseRequestId = :purchaseRequestId)
            ORDER BY o.createdAt DESC
            """)
    List<Orcamento> findFiltered(
            @Param("siteId") UUID siteId,
            @Param("dayStart") Instant dayStart,
            @Param("dayEnd") Instant dayEnd,
            @Param("purchaseRequestId") UUID purchaseRequestId);
}
