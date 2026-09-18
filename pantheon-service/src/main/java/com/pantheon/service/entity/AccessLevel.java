package com.pantheon.service.entity;

/**
 * Resolved or configured access for a {@link PermissionCapability}. Most capabilities only ever
 * use {@code VIEW} (unrestricted read) or {@code MANAGE} (full read/write). {@code HIDDEN} means
 * no access at all — not even read — and the capability's menu is not shown to that member;
 * company staff can never resolve to {@code HIDDEN} (see {@code SitePermissionService#resolve}).
 * {@code VIEW_AND_APPROVE} is meaningful only for {@code PURCHASE_REQUEST}: it cannot create or
 * submit a Pedido de Compra, but can act on an approval step matching the member's
 * {@code SiteMembership} function, and only sees a Pedido de Compra that is {@code CONCLUIDO} or
 * relevant to that function in the current approval cycle (see
 * {@code PurchaseRequestService#requireStepAuthority} and its dynamic-visibility helper).
 */
public enum AccessLevel {
    VIEW,
    MANAGE,
    VIEW_AND_APPROVE,
    HIDDEN
}
