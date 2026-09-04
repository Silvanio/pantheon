package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.Equipment;
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    List<Equipment> findByConstructionSiteId(UUID constructionSiteId);
}
