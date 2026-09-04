package com.pantheon.service.repository;

import com.pantheon.service.entity.Orcamento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, UUID> {

    List<Orcamento> findByMaterialRequestId(UUID materialRequestId);
}
