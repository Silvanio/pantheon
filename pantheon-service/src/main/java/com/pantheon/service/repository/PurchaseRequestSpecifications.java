package com.pantheon.service.repository;

import com.pantheon.service.entity.PurchaseRequest;
import com.pantheon.service.entity.PurchaseRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the Pedido de Compra listing filter as a {@link Specification} instead of a single
 * {@code @Query} with {@code (:param is null or ...)} clauses: with Postgres' JDBC driver, a bind
 * parameter used only inside an {@code IS NULL} check (as every optional filter's null branch is)
 * has no other context to infer its type from, and the driver rejects the query with
 * "could not determine data type of parameter $N". Building predicates conditionally in Java never
 * binds a parameter for a filter that isn't actually applied, so the ambiguity never arises.
 */
public final class PurchaseRequestSpecifications {

    private PurchaseRequestSpecifications() {}

    public static Specification<PurchaseRequest> siteId(UUID siteId) {
        return (root, query, cb) -> cb.equal(root.get("constructionSiteId"), siteId);
    }

    public static Specification<PurchaseRequest> status(PurchaseRequestStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseRequest> createdOn(Instant dayStart, Instant dayEnd) {
        if (dayStart == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.and(cb.greaterThanOrEqualTo(root.get("createdAt"), dayStart), cb.lessThan(root.get("createdAt"), dayEnd));
    }
}
