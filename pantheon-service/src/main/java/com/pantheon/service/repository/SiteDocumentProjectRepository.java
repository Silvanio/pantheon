package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteDocumentProject;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteDocumentProjectRepository extends JpaRepository<SiteDocumentProject, UUID> {

    List<SiteDocumentProject> findByConstructionSiteId(UUID constructionSiteId);
}
