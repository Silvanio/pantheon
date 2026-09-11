package com.pantheon.service.entity;

/**
 * Aguardando entrega (AWAITING_DELIVERY) → Entregue (DELIVERED) → Entregue e conferido
 * (DELIVERED_AND_CHECKED). Progression is strictly sequential — see {@code material-delivery-tracking}'s
 * "Material delivery status progression".
 */
public enum MaterialDeliveryStatus {
    AWAITING_DELIVERY,
    DELIVERED,
    DELIVERED_AND_CHECKED
}
