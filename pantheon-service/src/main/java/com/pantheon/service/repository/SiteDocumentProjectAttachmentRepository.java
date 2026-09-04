package com.pantheon.service.repository;

import com.pantheon.service.entity.SiteDocumentProjectAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteDocumentProjectAttachmentRepository extends JpaRepository<SiteDocumentProjectAttachment, UUID> {

    List<SiteDocumentProjectAttachment> findBySiteDocumentProjectId(UUID siteDocumentProjectId);
}
