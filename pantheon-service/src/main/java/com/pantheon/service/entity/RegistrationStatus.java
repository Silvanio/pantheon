package com.pantheon.service.entity;

/**
 * Whether an {@link AppUser} has completed registration. A {@link #PENDING_REGISTRATION}
 * account is a placeholder created for an invited email that had no account: it has no
 * usable credentials and cannot authenticate until registration is completed, at which
 * point it becomes {@link #ACTIVE}.
 */
public enum RegistrationStatus {
    ACTIVE,
    PENDING_REGISTRATION
}
