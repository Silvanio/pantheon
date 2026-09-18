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
 * A construction site's budget/quote from a single supplier, with free-text line items (see
 * {@link OrcamentoLineItem}). Its {@link OrcamentoStatus} follows its originating
 * {@link PurchaseRequest}'s approval outcome, if any — see {@code orcamento-management}.
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
        this.fornecedorCnpj = fornecedorCnpj;
        this.fornecedorNome = fornecedorNome;
        this.fornecedorEndereco = fornecedorEndereco;
        this.fornecedorContatoNome = fornecedorContatoNome;
        this.fornecedorContatoTelefone = fornecedorContatoTelefone;
        this.sourceFornecedorId = sourceFornecedorId;
        this.sourcePurchaseRequestId = sourcePurchaseRequestId;
    }

    /** Called when its originating Pedido de Compra's approval reaches CONFERIDO/CONCLUIDO. */
    public void lock() {
        this.status = OrcamentoStatus.LOCKED;
    }

    /** Called when its originating Pedido de Compra's approval is rejected back to ORCADO. */
    public void unlock() {
        this.status = OrcamentoStatus.DRAFT;
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
