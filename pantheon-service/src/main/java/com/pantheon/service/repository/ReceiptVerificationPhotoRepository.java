package com.pantheon.service.repository;

import com.pantheon.service.entity.ReceiptVerificationPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptVerificationPhotoRepository extends JpaRepository<ReceiptVerificationPhoto, UUID> {

    List<ReceiptVerificationPhoto> findByReceiptVerificationId(UUID receiptVerificationId);
}
