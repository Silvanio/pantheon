package com.pantheon.service.entity;

/**
 * Lifecycle of a {@link CompanyMembership} or {@link SiteMembership}. A membership created by
 * an admin adding someone starts as {@link #INVITED} and confers no access; it becomes
 * {@link #ACTIVE} only when the invited person accepts. Admin/creator memberships are created
 * {@link #ACTIVE} directly. {@link #NONE} is only used by an accountless
 * {@code SiteMembership} (service provider with no login) — there is no invitation to accept.
 */
public enum MembershipStatus {
    INVITED,
    ACTIVE,
    NONE
}
