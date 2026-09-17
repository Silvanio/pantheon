package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiteDocumentProjectAttachmentRepository extends JpaRepository<SiteDocumentProjectAttachment, UUID> {

    /** {@code folderId == null} means the construction site's root, not "any folder". */
    @Query("select a from SiteDocumentProjectAttachment a where a.constructionSiteId = :siteId "
            + "and ((:folderId is null and a.siteDocumentProjectId is null) or a.siteDocumentProjectId = :folderId)")
    List<SiteDocumentProjectAttachment> findChildren(@Param("siteId") UUID siteId, @Param("folderId") UUID folderId);

    List<SiteDocumentProjectAttachment> findBySiteDocumentProjectIdIn(List<UUID> folderIds);

    List<SiteDocumentProjectAttachment> findByTaskCardId(UUID taskCardId);

    @Query("select a.taskCardId as cardId, count(a) as attachmentCount from SiteDocumentProjectAttachment a "
            + "where a.taskCardId in :cardIds group by a.taskCardId")
    List<CardAttachmentCount> countByTaskCardIdIn(@Param("cardIds") List<UUID> cardIds);

    interface CardAttachmentCount {
        UUID getCardId();

        long getAttachmentCount();
    }
}
