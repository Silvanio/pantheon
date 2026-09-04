package com.pantheon.service.repository;

import com.pantheon.service.entity.OrcamentoLineItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoLineItemRepository extends JpaRepository<OrcamentoLineItem, UUID> {

    List<OrcamentoLineItem> findByOrcamentoId(UUID orcamentoId);
}
