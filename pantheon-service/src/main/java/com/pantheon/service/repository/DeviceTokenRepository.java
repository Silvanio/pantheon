package com.pantheon.service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pantheon.service.entity.DeviceToken;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserId(UUID userId);

    Optional<DeviceToken> findByToken(String token);

    void deleteByUserIdAndToken(UUID userId, String token);
}
