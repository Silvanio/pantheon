package com.pantheon.service.entity;

/**
 * {@code DRAFT} → {@code PENDING_APPROVAL} (submitted, its approval steps created for a new
 * cycle) → {@code APPROVED} (its cycle's final step approved). A rejection at any step returns
 * {@code PENDING_APPROVAL} to {@code DRAFT} rather than a dead-end "rejected" state — see
 * {@code daily-report-approval-workflow}'s "Acting on a Diário de Obra approval step".
 */
public enum DailyReportStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED
}
