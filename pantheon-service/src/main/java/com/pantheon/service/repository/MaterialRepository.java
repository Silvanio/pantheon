package com.pantheon.service.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.Material;
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findByConstructionSiteId(UUID constructionSiteId);

    List<Material> findByOrcamentoLineItemIdIn(List<UUID> orcamentoLineItemIds);

    /**
     * Materials for a site delivered within {@code [startInclusive, endExclusive)} — used for a
     * daily report's "materials delivered on this date" read-only view (see {@code
     * daily-construction-report}'s "Materials delivered on this report's date"). A plain derived
     * {@code Between} would be inclusive on both ends; an explicit query keeps the upper bound
     * exclusive so a material delivered exactly at the next day's start isn't double-counted.
     */
    @Query("SELECT m FROM Material m WHERE m.constructionSiteId = :siteId "
            + "AND m.deliveredAt >= :startInclusive AND m.deliveredAt < :endExclusive")
    List<Material> findByConstructionSiteIdAndDeliveredAtBetween(
            @Param("siteId") UUID constructionSiteId,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive);
}
