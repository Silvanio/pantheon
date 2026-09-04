package com.pantheon.service.entity;

/**
 * A construction site member's function, independent of company staff's platform role
 * ({@link CompanyRole}, ADMIN/MEMBER) — see {@link SiteMembership}. Individual capabilities may
 * key permission checks off it explicitly (see {@code SitePermissionService}).
 */
public enum ConstructionFunction {
    CLIENT,
    ENGINEER,
    ARCHITECT,
    SITE_FOREMAN,
    SERVICE_PROVIDER,
    OTHER
}
