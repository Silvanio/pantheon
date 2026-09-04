package com.pantheon.service.entity;

/** Discriminates which membership table a {@link MembershipInvitation} targets. */
public enum MembershipType {
    COMPANY,
    SITE
}
