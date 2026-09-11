package com.pantheon.service.entity;

/** Whether a {@link PurchaseRequestItem} is still awaiting an Orcamento, or has already been converted into one. */
public enum PurchaseRequestItemStatus {
    PENDING,
    CONVERTED
}
