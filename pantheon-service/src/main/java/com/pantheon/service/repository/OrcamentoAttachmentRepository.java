package com.pantheon.service.repository;

import com.pantheon.service.entity.OrcamentoAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoAttachmentRepository extends JpaRepository<OrcamentoAttachment, UUID> {

    List<OrcamentoAttachment> findByOrcamentoId(UUID orcamentoId);
}
