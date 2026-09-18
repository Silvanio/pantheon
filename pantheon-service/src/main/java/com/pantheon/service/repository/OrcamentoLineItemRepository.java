package com.pantheon.service.repository;

import com.pantheon.service.entity.OrcamentoLineItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoLineItemRepository extends JpaRepository<OrcamentoLineItem, UUID> {

    List<OrcamentoLineItem> findByOrcamentoId(UUID orcamentoId);

    /**
     * Every line item, across a given set of Orcamentos, quoted against a given Pedido-de-Compra
     * item — at most one is expected per Orcamento. Backs the comparison table (one query per
     * row) and selection validation.
     */
    List<OrcamentoLineItem> findByOrcamentoIdInAndSourcePurchaseRequestItemId(
            List<UUID> orcamentoIds, UUID sourcePurchaseRequestItemId);
}
