package com.pantheon.service.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.MaterialRequestItem;
public interface MaterialRequestItemRepository extends JpaRepository<MaterialRequestItem, UUID> {

    List<MaterialRequestItem> findByMaterialRequestId(UUID materialRequestId);
}
