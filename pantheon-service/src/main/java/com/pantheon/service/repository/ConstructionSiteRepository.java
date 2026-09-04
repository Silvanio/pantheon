package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.ConstructionSite;
public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, UUID> {

    List<ConstructionSite> findByProjectId(UUID projectId);
}
