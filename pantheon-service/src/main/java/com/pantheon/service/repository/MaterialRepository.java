package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.Material;
public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findByConstructionSiteId(UUID constructionSiteId);
}
