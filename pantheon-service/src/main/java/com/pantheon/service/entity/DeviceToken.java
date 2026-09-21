package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A mobile device's push-notification token, registered by the authenticated user it belongs to. See push-notifications capability. */
@Entity
@Table(name = "device_token")
public class DeviceToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevicePlatform platform;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeviceToken() {
        // JPA
    }

    public DeviceToken(UUID id, UUID userId, DevicePlatform platform, String token, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.token = token;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public String getToken() {
        return token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
