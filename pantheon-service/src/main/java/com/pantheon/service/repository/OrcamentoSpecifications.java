package com.pantheon.service.repository;

import com.pantheon.service.entity.Orcamento;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds the Orçamento listing filter as a {@link Specification} — see
 * {@link PurchaseRequestSpecifications} for why a single {@code @Query} with
 * {@code (:param is null or ...)} clauses fails against Postgres for a parameter used only in an
 * {@code IS NULL} check.
 */
public final class OrcamentoSpecifications {

    private OrcamentoSpecifications() {}

    public static Specification<Orcamento> siteId(UUID siteId) {
        return (root, query, cb) -> cb.equal(root.get("constructionSiteId"), siteId);
    }

    public static Specification<Orcamento> sourcePurchaseRequestId(UUID sourcePurchaseRequestId) {
        if (sourcePurchaseRequestId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("sourcePurchaseRequestId"), sourcePurchaseRequestId);
    }

    public static Specification<Orcamento> supplierContains(String supplier) {
        if (!StringUtils.hasText(supplier)) {
            return null;
        }
        String pattern = "%" + supplier.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("fornecedorNome")), pattern);
    }

    public static Specification<Orcamento> createdOn(Instant dayStart, Instant dayEnd) {
        if (dayStart == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), dayStart), cb.lessThan(root.get("createdAt"), dayEnd));
    }
}
