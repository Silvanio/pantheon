package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteDocumentProject;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteDocumentProjectRepository extends JpaRepository<SiteDocumentProject, UUID> {

    /** {@code parentId == null} means the construction site's root, not "any parent". */
    @Query("select p from SiteDocumentProject p where p.constructionSiteId = :siteId "
            + "and ((:parentId is null and p.parentId is null) or p.parentId = :parentId)")
    List<SiteDocumentProject> findChildren(@Param("siteId") UUID siteId, @Param("parentId") UUID parentId);

    List<SiteDocumentProject> findByParentId(UUID parentId);
}
