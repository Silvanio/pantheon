package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PurchaseRequestRepository
        extends JpaRepository<PurchaseRequest, UUID>, JpaSpecificationExecutor<PurchaseRequest> {

    long countByConstructionSiteIdAndCreatedAtBetween(UUID constructionSiteId, Instant dayStart, Instant dayEnd);
}
