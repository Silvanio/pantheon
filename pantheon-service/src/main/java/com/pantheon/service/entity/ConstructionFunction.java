package com.pantheon.service.entity;

/**
 * A project member's function on site, independent of their platform permission
 * {@link ProjectRole} (ADMIN/MEMBER). Descriptive by default; individual capabilities may key
 * permission checks off it explicitly (see each capability's design.md).
 */
public enum ConstructionFunction {
    CLIENT,
    ENGINEER,
    ARCHITECT,
    SITE_FOREMAN,
    SERVICE_PROVIDER,
    OTHER
}
