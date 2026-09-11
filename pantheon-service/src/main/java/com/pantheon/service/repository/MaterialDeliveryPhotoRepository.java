package com.pantheon.service.repository;

import com.pantheon.service.entity.MaterialDeliveryPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialDeliveryPhotoRepository extends JpaRepository<MaterialDeliveryPhoto, UUID> {

    List<MaterialDeliveryPhoto> findByMaterialId(UUID materialId);
}
