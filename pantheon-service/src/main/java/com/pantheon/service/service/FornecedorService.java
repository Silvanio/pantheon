package com.pantheon.service.service;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.exception.CnpjPrefixTooShortException;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.FornecedorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Company-scoped supplier registry: find-or-create by CNPJ when an Orçamento is created, and
 * CNPJ-prefix search for autocomplete. See {@code supplier-registry}.
 */
@Service
public class FornecedorService {

    private static final int MIN_SEARCH_PREFIX_LENGTH = 5;

    private final FornecedorRepository fornecedorRepository;
    private final ConstructionSiteRepository siteRepository;
    private final SiteAccessService siteAccessService;

    public FornecedorService(
            FornecedorRepository fornecedorRepository, ConstructionSiteRepository siteRepository,
            SiteAccessService siteAccessService) {
        this.fornecedorRepository = fornecedorRepository;
        this.siteRepository = siteRepository;
        this.siteAccessService = siteAccessService;
    }

    /** Reuses an existing Fornecedor for (companyId, cnpj) if present, ignoring any differences in the other fields. */
    @Transactional
    Fornecedor findOrCreate(UUID companyId, UUID actingUserId, FornecedorRequest request) {
        return fornecedorRepository.findByCompanyIdAndCnpj(companyId, request.cnpj()).orElseGet(() ->
                fornecedorRepository.save(new Fornecedor(
                        UUID.randomUUID(), companyId, request.cnpj(), request.name(), request.address(),
                        request.contactName(), request.contactPhone(), actingUserId, Instant.now())));
    }

    public List<Fornecedor> searchByCnpjPrefix(UUID siteId, UUID actingUserId, String cnpjPrefix) {
        if (cnpjPrefix == null || cnpjPrefix.length() < MIN_SEARCH_PREFIX_LENGTH) {
            throw new CnpjPrefixTooShortException();
        }
        siteAccessService.requireAccess(siteId, actingUserId);
        UUID companyId = requireSite(siteId).getCompanyId();
        return fornecedorRepository.findByCompanyIdAndCnpjStartingWithOrderByNameAsc(companyId, cnpjPrefix);
    }

    private ConstructionSite requireSite(UUID siteId) {
        return siteRepository.findById(siteId).orElseThrow(() -> new ConstructionSiteNotFoundException(siteId));
    }
}
