package com.pantheon.service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A construction site's budget/quote, with free-text line items (see {@link OrcamentoLineItem})
 * and a configurable sequential approval chain (see {@link OrcamentoApproval}). See
 * {@code orcamento-approval-workflow}.
 */
@Entity
@Table(name = "orcamento")
public class Orcamento {

    @Id
    private UUID id;

    @Column(name = "construction_site_id", nullable = false)
    private UUID constructionSiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrcamentoStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "current_approval_cycle", nullable = false)
    private int currentApprovalCycle;

    @Column(name = "last_rejection_reason")
    private String lastRejectionReason;

    @Column(name = "fornecedor_cnpj", nullable = false)
    private String fornecedorCnpj;

    @Column(name = "fornecedor_nome", nullable = false)
    private String fornecedorNome;

    @Column(name = "fornecedor_endereco")
    private String fornecedorEndereco;

    @Column(name = "fornecedor_contato_nome")
    private String fornecedorContatoNome;

    @Column(name = "fornecedor_contato_telefone")
    private String fornecedorContatoTelefone;

    @Column(name = "source_fornecedor_id")
    private UUID sourceFornecedorId;

    @Column(name = "source_purchase_request_id")
    private UUID sourcePurchaseRequestId;

    protected Orcamento() {
        // JPA
    }

    public Orcamento(
            UUID id, UUID constructionSiteId, UUID createdBy, Instant createdAt, String fornecedorCnpj,
            String fornecedorNome, String fornecedorEndereco, String fornecedorContatoNome,
            String fornecedorContatoTelefone, UUID sourceFornecedorId, UUID sourcePurchaseRequestId) {
        this.id = id;
        this.constructionSiteId = constructionSiteId;
        this.status = OrcamentoStatus.DRAFT;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.currentApprovalCycle = 0;
        this.fornecedorCnpj = fornecedorCnpj;
        this.fornecedorNome = fornecedorNome;
        this.fornecedorEndereco = fornecedorEndereco;
        this.fornecedorContatoNome = fornecedorContatoNome;
        this.fornecedorContatoTelefone = fornecedorContatoTelefone;
        this.sourceFornecedorId = sourceFornecedorId;
        this.sourcePurchaseRequestId = sourcePurchaseRequestId;
    }

    /** Starts a new approval cycle: DRAFT/rejected -> IN_APPROVAL. */
    public void submitForApproval(Instant now) {
        this.status = OrcamentoStatus.IN_APPROVAL;
        this.submittedAt = now;
        this.currentApprovalCycle += 1;
    }

    public void approve(Instant now) {
        this.status = OrcamentoStatus.APPROVED;
        this.approvedAt = now;
    }

    /** A rejection at any approval step bounces the whole Orcamento back to DRAFT for revision. */
    public void returnToDraftAfterRejection(String reason) {
        this.status = OrcamentoStatus.DRAFT;
        this.lastRejectionReason = reason;
    }

    public void complete(Instant now) {
        this.status = OrcamentoStatus.COMPLETED;
        this.completedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConstructionSiteId() {
        return constructionSiteId;
    }

    public OrcamentoStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public int getCurrentApprovalCycle() {
        return currentApprovalCycle;
    }

    public String getLastRejectionReason() {
        return lastRejectionReason;
    }

    public String getFornecedorCnpj() {
        return fornecedorCnpj;
    }

    public String getFornecedorNome() {
        return fornecedorNome;
    }

    public String getFornecedorEndereco() {
        return fornecedorEndereco;
    }

    public String getFornecedorContatoNome() {
        return fornecedorContatoNome;
    }

    public String getFornecedorContatoTelefone() {
        return fornecedorContatoTelefone;
    }

    public UUID getSourceFornecedorId() {
        return sourceFornecedorId;
    }

    public UUID getSourcePurchaseRequestId() {
        return sourcePurchaseRequestId;
    }
}
