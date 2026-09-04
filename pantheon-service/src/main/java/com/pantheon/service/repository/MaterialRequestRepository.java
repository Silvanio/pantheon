package com.pantheon.service.repository;

import com.pantheon.service.entity.MaterialRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.MaterialRequest;
public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, UUID> {

    List<MaterialRequest> findByConstructionSiteIdOrderByCreatedAtDesc(UUID constructionSiteId);

    List<MaterialRequest> findByConstructionSiteIdAndStatusOrderByCreatedAtDesc(
            UUID constructionSiteId, MaterialRequestStatus status);
}
