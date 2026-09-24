package com.pantheon.service.repository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.pantheon.service.entity.Equipment;
import com.pantheon.service.entity.EquipmentStatus;
public interface EquipmentRepository extends JpaRepository<Equipment, UUID>, JpaSpecificationExecutor<Equipment> {

    Page<Equipment> findByConstructionSiteId(UUID constructionSiteId, Pageable pageable);

    long countByConstructionSiteIdAndStatus(UUID constructionSiteId, EquipmentStatus status);
}
