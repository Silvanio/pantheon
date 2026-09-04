package com.pantheon.service.entity;

/** A capability that can be granted/restricted per construction site (see {@link SitePermissionOverride}). */
public enum PermissionCapability {
    DOCUMENT_PROJECTS,
    DAILY_REPORT,
    EQUIPMENT_MATERIAL,
    MATERIAL_REQUEST,
    MATERIAL_APPROVAL
}
