package com.pantheon.service.entity;

/**
 * Resolved or configured access for a {@link PermissionCapability}. {@code PURCHASE_REQUEST}
 * and {@code ORCAMENTO_MANAGE} only ever use {@code MANAGE} (presence = granted); the other
 * capabilities distinguish {@code VIEW} from {@code MANAGE}. Orcamento approval-step authority
 * is a separate, structural concern (see {@code SiteOrcamentoApprovalLevel}) and is not gated
 * by a {@link PermissionCapability} at all.
 */
public enum AccessLevel {
    VIEW,
    MANAGE
}
