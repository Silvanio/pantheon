package com.pantheon.service.service;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.entity.FornecedorPaymentMethod;
import com.pantheon.service.exception.CnpjPrefixTooShortException;
import com.pantheon.service.exception.ConstructionSiteNotFoundException;
import com.pantheon.service.exception.InvalidCpfCnpjException;
import com.pantheon.service.exception.PixKeyRequiredException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.FornecedorRepository;
import com.pantheon.service.validation.CnpjValidator;
import com.pantheon.service.validation.CpfValidator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    /**
     * Reuses an existing Fornecedor for (companyId, cnpj) if present, ignoring any differences in
     * the other fields. The CPF/CNPJ is optional — without one there's no reliable identity key,
     * so a fresh Fornecedor is always created instead of attempting to find one.
     */
    @Transactional
    Fornecedor findOrCreate(UUID companyId, UUID actingUserId, FornecedorRequest request) {
        boolean hasDocument = request.cnpj() != null && !request.cnpj().isBlank();
        if (hasDocument) {
            requireValidCpfOrCnpj(request.cnpj());
        }
        Optional<Fornecedor> existing = hasDocument
                ? fornecedorRepository.findByCompanyIdAndCnpj(companyId, request.cnpj())
                : Optional.empty();
        return existing.orElseGet(() -> {
            boolean isPix = request.paymentMethod() == FornecedorPaymentMethod.PIX;
            if (isPix && (request.pixKey() == null || request.pixKey().isBlank())) {
                throw new PixKeyRequiredException();
            }
            return fornecedorRepository.save(new Fornecedor(
                    UUID.randomUUID(), companyId, request.cnpj(), request.name(), request.address(),
                    request.contactName(), request.contactPhone(), request.paymentMethod(),
                    isPix ? request.pixKey() : null, actingUserId, Instant.now()));
        });
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

    /**
     * Validates {@code document} as a CPF (11 digits) or CNPJ (14 digits) by its digit count —
     * any other length, or a checksum mismatch, is rejected.
     */
    private void requireValidCpfOrCnpj(String document) {
        String digits = document.replaceAll("\\D", "");
        boolean valid = switch (digits.length()) {
            case 11 -> CpfValidator.isValid(digits);
            case 14 -> CnpjValidator.isValid(digits);
            default -> false;
        };
        if (!valid) {
            throw new InvalidCpfCnpjException(document);
        }
    }
}
