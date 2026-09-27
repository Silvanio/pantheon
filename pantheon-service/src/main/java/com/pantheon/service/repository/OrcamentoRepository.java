package com.pantheon.service.repository;

import com.pantheon.service.entity.Orcamento;
import com.pantheon.service.entity.OrcamentoStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID>, JpaSpecificationExecutor<Orcamento> {

    /** Every Orcamento converted from a given Pedido de Compra — used to lock/unlock them together. */
    List<Orcamento> findBySourcePurchaseRequestId(UUID sourcePurchaseRequestId);

    /** Batch form of {@link #findBySourcePurchaseRequestId(UUID)} across a page of headers — avoids one query per row. */
    List<Orcamento> findBySourcePurchaseRequestIdIn(List<UUID> sourcePurchaseRequestIds);

    long countByConstructionSiteIdAndStatus(UUID constructionSiteId, OrcamentoStatus status);
}
