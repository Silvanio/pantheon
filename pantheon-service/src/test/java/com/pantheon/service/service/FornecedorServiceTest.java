package com.pantheon.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pantheon.service.dto.FornecedorRequest;
import com.pantheon.service.entity.ConstructionSite;
import com.pantheon.service.entity.Fornecedor;
import com.pantheon.service.exception.CnpjPrefixTooShortException;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.FornecedorRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceTest {

    @Mock
    private FornecedorRepository fornecedorRepository;

    @Mock
    private ConstructionSiteRepository siteRepository;

    @Mock
    private SiteAccessService siteAccessService;

    private FornecedorService service;

    private UUID siteId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        service = new FornecedorService(fornecedorRepository, siteRepository, siteAccessService);
        siteId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        lenient().when(siteRepository.findById(siteId)).thenReturn(Optional.of(new ConstructionSite(
                siteId, companyId, "Obra", "Endereco", LocalDate.now(), null, UUID.randomUUID(), Instant.now())));
    }

    @Test
    void findOrCreateCreatesNewFornecedorForUnseenCnpj() {
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "12345678000199")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(), new FornecedorRequest("12345678000199", "Nova Empresa", null, null, null));

        assertThat(result.getCnpj()).isEqualTo("12345678000199");
        assertThat(result.getName()).isEqualTo("Nova Empresa");
        verify(fornecedorRepository).save(any());
    }

    @Test
    void findOrCreateReusesExistingFornecedorForKnownCnpj() {
        Fornecedor existing = new Fornecedor(
                UUID.randomUUID(), companyId, "12345678000199", "Empresa Original", null, null, null,
                UUID.randomUUID(), Instant.now());
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "12345678000199")).thenReturn(Optional.of(existing));

        Fornecedor result = service.findOrCreate(
                companyId, UUID.randomUUID(),
                new FornecedorRequest("12345678000199", "Nome Diferente Ignorado", null, null, null));

        assertThat(result).isEqualTo(existing);
        assertThat(result.getName()).isEqualTo("Empresa Original");
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void searchReturnsMatchesByPrefix() {
        Fornecedor match = new Fornecedor(
                UUID.randomUUID(), companyId, "12345678000199", "Empresa", null, null, null, UUID.randomUUID(),
                Instant.now());
        when(fornecedorRepository.findByCompanyIdAndCnpjStartingWithOrderByNameAsc(companyId, "12345"))
                .thenReturn(List.of(match));

        List<Fornecedor> result = service.searchByCnpjPrefix(siteId, UUID.randomUUID(), "12345");

        assertThat(result).containsExactly(match);
    }

    @Test
    void searchRejectsPrefixShorterThanFiveCharacters() {
        assertThatThrownBy(() -> service.searchByCnpjPrefix(siteId, UUID.randomUUID(), "1234"))
                .isInstanceOf(CnpjPrefixTooShortException.class);
    }

    @Test
    void sameCnpjUnderDifferentCompaniesStaysSeparate() {
        UUID otherCompanyId = UUID.randomUUID();
        when(fornecedorRepository.findByCompanyIdAndCnpj(companyId, "12345678000199")).thenReturn(Optional.empty());
        when(fornecedorRepository.findByCompanyIdAndCnpj(otherCompanyId, "12345678000199")).thenReturn(Optional.empty());
        when(fornecedorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FornecedorRequest request = new FornecedorRequest("12345678000199", "Empresa", null, null, null);
        Fornecedor first = service.findOrCreate(companyId, UUID.randomUUID(), request);
        Fornecedor second = service.findOrCreate(otherCompanyId, UUID.randomUUID(), request);

        assertThat(first.getCompanyId()).isEqualTo(companyId);
        assertThat(second.getCompanyId()).isEqualTo(otherCompanyId);
        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
