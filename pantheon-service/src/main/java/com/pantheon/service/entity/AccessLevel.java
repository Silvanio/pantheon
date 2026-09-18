package com.pantheon.service.entity;

/**
 * Resolved or configured access for a {@link PermissionCapability}. {@code PURCHASE_REQUEST}
 * and {@code ORCAMENTO_MANAGE} only ever use {@code MANAGE} (presence = granted); the other
 * capabilities distinguish {@code VIEW} from {@code MANAGE}. Pedido de Compra approval-step
 * authority is a separate, structural concern (see {@code SitePurchaseRequestApprovalLevel}) and is not gated
 * by a {@link PermissionCapability} at all. {@code HIDDEN} means no access at all — not even
 * read — and the capability's menu is not shown to that member; company staff can never resolve
 * to {@code HIDDEN} (see {@code SitePermissionService#resolve}).
 */
public enum AccessLevel {
    VIEW,
    MANAGE,
    HIDDEN
}
