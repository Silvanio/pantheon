package com.pantheon.service.entity;

/**
 * Resolved or configured access for a {@link PermissionCapability}. {@code MATERIAL_REQUEST}
 * and {@code MATERIAL_APPROVAL} only ever use {@code MANAGE} (presence = granted); the other
 * capabilities distinguish {@code VIEW} from {@code MANAGE}.
 */
public enum AccessLevel {
    VIEW,
    MANAGE
}
