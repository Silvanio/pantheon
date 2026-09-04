package com.pantheon.service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.ReceiptVerification;
public interface ReceiptVerificationRepository extends JpaRepository<ReceiptVerification, UUID> {

    List<ReceiptVerification> findByMaterialRequestItemIdIn(List<UUID> materialRequestItemIds);

    Optional<ReceiptVerification> findByMaterialRequestItemId(UUID materialRequestItemId);
}
