package com.pantheon.service.repository;

import com.pantheon.service.entity.ConstructionSite;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, UUID> {

    List<ConstructionSite> findByCompanyId(UUID companyId);
}
