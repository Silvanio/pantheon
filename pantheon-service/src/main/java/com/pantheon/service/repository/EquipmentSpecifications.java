package com.pantheon.service.repository;

import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the Equipment listing filter as a {@link Specification} instead of a single
 * {@code @Query} with {@code (:param is null or ...)} clauses — see
 * {@code PurchaseRequestSpecifications}'s doc comment for why (Postgres' JDBC driver can't infer
 * a bind parameter's type when it's only ever used inside an {@code IS NULL} check).
 */
public final class EquipmentSpecifications {

    private EquipmentSpecifications() {}

    public static Specification<Equipment> siteId(UUID siteId) {
        return (root, query, cb) -> cb.equal(root.get("constructionSiteId"), siteId);
    }

    public static Specification<Equipment> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Equipment> typeContains(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("type")), "%" + type.toLowerCase() + "%");
    }

    public static Specification<Equipment> status(EquipmentStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
