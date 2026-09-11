package com.pantheon.service.repository;

import com.pantheon.service.entity.Fornecedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, UUID> {

    Optional<Fornecedor> findByCompanyIdAndCnpj(UUID companyId, String cnpj);

    List<Fornecedor> findByCompanyIdAndCnpjStartingWithOrderByNameAsc(UUID companyId, String cnpjPrefix);
}
