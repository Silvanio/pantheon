package com.pantheon.service.entity;

/**
 * Lifecycle of a {@link ProjectMembership}. A membership created by an admin adding someone
 * to a project starts as {@link #INVITED} and confers no project access; it becomes
 * {@link #ACTIVE} only when the invited person accepts the invitation. Admin memberships
 * (the project creator) are created {@link #ACTIVE} directly.
 */
public enum MembershipStatus {
    INVITED,
    ACTIVE
}
