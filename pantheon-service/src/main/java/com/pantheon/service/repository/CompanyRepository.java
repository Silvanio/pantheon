package com.pantheon.service.repository;

import com.pantheon.service.entity.Company;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findByCreatedBy(UUID createdBy);

    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
