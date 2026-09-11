package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {

    List<PurchaseRequest> findByConstructionSiteIdOrderByCreatedAtDesc(UUID constructionSiteId);

    List<PurchaseRequest> findByConstructionSiteIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID constructionSiteId, Instant dayStart, Instant dayEnd);

    long countByConstructionSiteIdAndCreatedAtBetween(UUID constructionSiteId, Instant dayStart, Instant dayEnd);
}
